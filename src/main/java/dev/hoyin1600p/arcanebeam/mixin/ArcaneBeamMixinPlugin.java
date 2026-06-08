package dev.hoyin1600p.arcanebeam.mixin;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

public class ArcaneBeamMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String VAULT_ADDITIONS_MOD_ID = "vaultadditions";
    private static final String SOPHISTICATED_STORAGE_DISPLAY_MIXIN = "dev.hoyin1600p.arcanebeam.mixin.SophisticatedStorageDisplayItemRendererMixin";
    private static final String SOPHISTICATED_STORAGE_BARREL_BAKED_MIXIN = "dev.hoyin1600p.arcanebeam.mixin.SophisticatedStorageBarrelBakedModelBaseMixin";
    private static final String SOPHISTICATED_STORAGE_LIMITED_BARREL_CLIENT_INTERACTION_MIXIN = "dev.hoyin1600p.arcanebeam.mixin.SophisticatedStorageLimitedBarrelClientInteractionMixin";
    private static final String SOPHISTICATED_STORAGE_LIMITED_BARREL_CLASS = "net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlock";
    private static final Set<String> OPTIONAL_SOPHISTICATED_STORAGE_MIXINS = Set.of(
            SOPHISTICATED_STORAGE_DISPLAY_MIXIN,
            SOPHISTICATED_STORAGE_BARREL_BAKED_MIXIN,
            SOPHISTICATED_STORAGE_LIMITED_BARREL_CLIENT_INTERACTION_MIXIN
    );
    private static final Set<String> VAULT_ADDITIONS_CONFLICTING_MIXINS = Set.of(
            SOPHISTICATED_STORAGE_DISPLAY_MIXIN,
            SOPHISTICATED_STORAGE_BARREL_BAKED_MIXIN,
            SOPHISTICATED_STORAGE_LIMITED_BARREL_CLIENT_INTERACTION_MIXIN
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (OPTIONAL_SOPHISTICATED_STORAGE_MIXINS.contains(mixinClassName)) {
            boolean targetPresent = isClassPresent(targetClassName);
            if (!targetPresent) {
                LOGGER.info("ArcaneBeam skipping optional Sophisticated Storage mixin {} because target {} is not present", mixinClassName, targetClassName);
                return false;
            }
            if (!isClassPresent(SOPHISTICATED_STORAGE_LIMITED_BARREL_CLASS)) {
                LOGGER.info("ArcaneBeam skipping optional Sophisticated Storage mixin {} because {} is not present", mixinClassName, SOPHISTICATED_STORAGE_LIMITED_BARREL_CLASS);
                return false;
            }
            if (VAULT_ADDITIONS_CONFLICTING_MIXINS.contains(mixinClassName) && isVaultAdditionsPresent()) {
                LOGGER.info("ArcaneBeam skipping optional Sophisticated Storage mixin {} because {} is present", mixinClassName, VAULT_ADDITIONS_MOD_ID);
                return false;
            }

            LOGGER.info("ArcaneBeam applying optional Sophisticated Storage mixin {} for target {}", mixinClassName, targetClassName);
            return true;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isClassPresent(String className) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode(className);
            return true;
        } catch (ClassNotFoundException | IOException e) {
            return false;
        }
    }

    private static boolean isVaultAdditionsPresent() {
        try {
            LoadingModList loadingModList = LoadingModList.get();
            return loadingModList != null && loadingModList.getModFileById(VAULT_ADDITIONS_MOD_ID) != null;
        } catch (LinkageError | RuntimeException e) {
            LOGGER.debug("ArcaneBeam could not query Forge loading mod list for {}", VAULT_ADDITIONS_MOD_ID, e);
            return false;
        }
    }
}
