package dev.hoyin1600p.arcanebeam.update;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class UpdateManifestFetcher {
    static final int MAX_MANIFEST_BYTES = 262_144;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Executor BODY_READER = Executors.newCachedThreadPool(
            runnable -> {
                Thread thread = new Thread(runnable, "update-manifest-reader");
                thread.setDaemon(true);
                return thread;
            }
    );
    private static HttpClient sharedClient;

    private UpdateManifestFetcher() {
    }

    private static synchronized HttpClient client() {
        if (sharedClient == null) {
            sharedClient = HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }
        return sharedClient;
    }

    static CompletableFuture<Optional<UpdateNotice>> fetch(
            URI manifestUri,
            String modId,
            String displayName,
            String currentVersion,
            String minecraftVersion,
            String downloadUrl
    ) {
        return fetch(
                manifestUri,
                modId,
                displayName,
                currentVersion,
                minecraftVersion,
                downloadUrl,
                REQUEST_TIMEOUT
        );
    }

    static CompletableFuture<Optional<UpdateNotice>> fetch(
            URI manifestUri,
            String modId,
            String displayName,
            String currentVersion,
            String minecraftVersion,
            String downloadUrl,
            Duration requestTimeout
    ) {
        HttpRequest request = HttpRequest.newBuilder(manifestUri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header(
                        "User-Agent",
                        modId + "/" + currentVersion + " update-check"
                )
                .GET()
                .build();

        AtomicReference<InputStream> openStream = new AtomicReference<>();
        CompletableFuture<Optional<UpdateNotice>> result = client().sendAsync(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        ).thenApplyAsync(response -> {
            InputStream stream = response.body();
            openStream.set(stream);
            try (stream) {
                if (response.statusCode() != 200) {
                    throw new CompletionException(new IOException(
                            "Update manifest returned HTTP "
                                    + response.statusCode()
                    ));
                }
                OptionalLong declaredLength = response.headers()
                        .firstValueAsLong("Content-Length");
                if (declaredLength.isPresent()
                        && declaredLength.getAsLong() > MAX_MANIFEST_BYTES) {
                    throw tooLarge();
                }
                String body = new String(
                        readCapped(stream),
                        StandardCharsets.UTF_8
                );
                return UpdateManifestParser.parse(
                        body,
                        modId,
                        displayName,
                        currentVersion,
                        minecraftVersion,
                        downloadUrl
                );
            } catch (IOException exception) {
                throw new CompletionException(exception);
            }
        }, BODY_READER).orTimeout(
                requestTimeout.toMillis(),
                TimeUnit.MILLISECONDS
        );
        // Closing the stream unblocks a reader stuck on a dripping response
        // after a timeout or cancellation.
        result.whenComplete((value, error) -> {
            InputStream stream = openStream.get();
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                    // Nothing further to release.
                }
            }
        });
        return result;
    }

    private static byte[] readCapped(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = stream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_MANIFEST_BYTES) {
                throw tooLarge();
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static CompletionException tooLarge() {
        return new CompletionException(new IOException(
                "Update manifest exceeded " + MAX_MANIFEST_BYTES + " bytes"
        ));
    }
}
