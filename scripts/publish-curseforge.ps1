param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$')]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [string]$ChangelogFile,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https://github\.com/HoYin1600p/ArcaneBeam/releases/tag/v')]
    [string]$GitHubReleaseUrl,

    [string]$ArtifactPath,
    [switch]$DryRun,
    [switch]$ValidateCredential
)

$ErrorActionPreference = 'Stop'
$repositoryDirectory = Split-Path -Parent $PSScriptRoot
$manifestPath = Join-Path $repositoryDirectory '.codex/mod-publish.json'
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json

function Test-CurseForgeCredentialAndMetadata {
    $apiToken = [Environment]::GetEnvironmentVariable([string]$manifest.curseforge.uploadTokenEnvironment)
    if ([string]::IsNullOrWhiteSpace($apiToken)) {
        throw "Set $($manifest.curseforge.uploadTokenEnvironment) in the process environment before validating CurseForge author access."
    }

    $expectedIds = @($manifest.curseforge.gameVersionIds)
    $expectedNames = @($manifest.curseforge.gameVersions)
    if ($expectedIds.Count -ne $expectedNames.Count) {
        throw 'CurseForge game-version IDs and names must have the same number of entries.'
    }

    Add-Type -AssemblyName System.Net.Http
    $client = [Net.Http.HttpClient]::new()
    try {
        $client.DefaultRequestHeaders.Add('X-Api-Token', $apiToken)
        $response = $client.GetAsync('https://minecraft.curseforge.com/api/game/versions').GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "CurseForge author authentication failed with HTTP $([int]$response.StatusCode)."
        }

        $availableVersions = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        for ($index = 0; $index -lt $expectedIds.Count; $index++) {
            $expectedId = [int]$expectedIds[$index]
            $expectedName = [string]$expectedNames[$index]
            $match = @($availableVersions | Where-Object {
                [int]$_.id -eq $expectedId -and [string]$_.name -eq $expectedName
            })
            if ($match.Count -ne 1) {
                throw "CurseForge game-version mapping $expectedId -> $expectedName is unavailable or ambiguous."
            }
        }

        Write-Host "CurseForge author credential and $($expectedIds.Count) upload metadata mappings verified."
    }
    finally {
        $client.Dispose()
        $apiToken = $null
    }
}

$configuredVersionLine = Get-Content -LiteralPath (Join-Path $repositoryDirectory 'gradle.properties') |
    Where-Object { $_ -match '^mod_version=' } |
    Select-Object -First 1
$configuredVersion = ($configuredVersionLine -split '=', 2)[1].Trim()
if ($configuredVersion -ne $Version) {
    throw "Requested version $Version does not match gradle.properties version $configuredVersion."
}

if ([string]::IsNullOrWhiteSpace($ArtifactPath)) {
    $candidatePath = Join-Path $repositoryDirectory "build/libs/ArcaneBeam-1.18.2-$Version.jar"
}
else {
    $candidatePath = if ([IO.Path]::IsPathRooted($ArtifactPath)) {
        $ArtifactPath
    }
    else {
        Join-Path $repositoryDirectory $ArtifactPath
    }
}

$artifact = Get-Item -LiteralPath $candidatePath -ErrorAction Stop
if ($artifact.Name -ne "ArcaneBeam-1.18.2-$Version.jar") {
    throw "The selected artifact has an unexpected name: $($artifact.Name)"
}
if ($artifact.Name -match '(?i)(sources|dev|shadow)') {
    throw "A classified JAR cannot be uploaded: $($artifact.Name)"
}

$resolvedChangelogPath = if ([IO.Path]::IsPathRooted($ChangelogFile)) {
    $ChangelogFile
}
else {
    Join-Path $repositoryDirectory $ChangelogFile
}
$changelog = (Get-Content -LiteralPath $resolvedChangelogPath -Raw).Trim()
if ([string]::IsNullOrWhiteSpace($changelog)) {
    throw 'The CurseForge changelog is empty.'
}
if (-not $changelog.Contains($GitHubReleaseUrl)) {
    $changelog = "$changelog`n`nFull release details: $GitHubReleaseUrl"
}

$artifactHash = (Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
$metadata = [ordered]@{
    changelog = $changelog
    changelogType = 'markdown'
    displayName = "ArcaneBeam $Version"
    releaseType = $manifest.curseforge.releaseChannel
    gameVersions = @($manifest.curseforge.gameVersionIds)
    isMarkedForManualRelease = [bool]$manifest.curseforge.manualRelease
    relations = [ordered]@{
        projects = @($manifest.curseforge.relations | ForEach-Object {
            [ordered]@{
                slug = [string]$_.projectSlug
                type = [string]$_.type
            }
        })
    }
}

if ($ValidateCredential) {
    Test-CurseForgeCredentialAndMetadata
}

$plan = [ordered]@{
    dryRun = [bool]$DryRun
    endpoint = "https://minecraft.curseforge.com/api/projects/$($manifest.curseforge.projectId)/upload-file"
    authenticationEnvironment = $manifest.curseforge.uploadTokenEnvironment
    artifact = [ordered]@{
        path = $artifact.FullName.Substring($repositoryDirectory.Length + 1).Replace('\', '/')
        filename = $artifact.Name
        size = $artifact.Length
        sha256 = $artifactHash
    }
    metadata = $metadata
    expectedPublicMetadata = [ordered]@{
        projectId = [string]$manifest.curseforge.projectId
        fileName = $artifact.Name
        displayName = $metadata.displayName
        releaseChannel = $metadata.releaseType
        gameVersions = @($manifest.curseforge.gameVersions)
        manualRelease = $metadata.isMarkedForManualRelease
        relations = @($manifest.curseforge.relations | ForEach-Object { [string]$_.projectId })
        githubReleaseUrl = $GitHubReleaseUrl
    }
}

if ($DryRun) {
    $outputDirectory = Join-Path $repositoryDirectory 'build/mod-publish-rehearsal'
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    $outputPath = Join-Path $outputDirectory 'curseforge-upload-dry-run.json'
    $plan | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $outputPath -Encoding utf8
    Write-Host 'CurseForge upload dry run passed; no remote request was issued.'
    Write-Host "Artifact: $($artifact.Name) ($($artifact.Length) bytes)"
    Write-Host "SHA-256: $artifactHash"
    Write-Host "Metadata plan: $outputPath"
    exit 0
}

$apiToken = [Environment]::GetEnvironmentVariable([string]$manifest.curseforge.uploadTokenEnvironment)
if ([string]::IsNullOrWhiteSpace($apiToken)) {
    throw "Set $($manifest.curseforge.uploadTokenEnvironment) in the process environment before uploading."
}

Add-Type -AssemblyName System.Net.Http
$client = [Net.Http.HttpClient]::new()
$multipart = [Net.Http.MultipartFormDataContent]::new()
$fileStream = [IO.File]::OpenRead($artifact.FullName)
try {
    $client.DefaultRequestHeaders.Add('X-Api-Token', $apiToken)
    $metadataContent = [Net.Http.StringContent]::new(
        ($metadata | ConvertTo-Json -Depth 6 -Compress),
        [Text.Encoding]::UTF8,
        'application/json'
    )
    $fileContent = [Net.Http.StreamContent]::new($fileStream)
    $fileContent.Headers.ContentType = [Net.Http.Headers.MediaTypeHeaderValue]::new('application/java-archive')
    $multipart.Add($metadataContent, 'metadata')
    $multipart.Add($fileContent, 'file', $artifact.Name)

    $response = $client.PostAsync($plan.endpoint, $multipart).GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if (-not $response.IsSuccessStatusCode) {
        throw "CurseForge upload failed with HTTP $([int]$response.StatusCode): $body"
    }
    $result = $body | ConvertFrom-Json
    if ($null -eq $result.id) {
        throw "CurseForge accepted the request but did not return a file ID: $body"
    }
    Write-Host "CurseForge upload submitted. File ID: $($result.id)"
}
finally {
    $fileStream.Dispose()
    $multipart.Dispose()
    $client.Dispose()
}
