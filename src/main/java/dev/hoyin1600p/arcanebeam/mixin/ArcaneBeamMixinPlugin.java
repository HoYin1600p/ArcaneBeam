package dev.hoyin1600p.arcanebeam.mixin;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

public class ArcaneBeamMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SOPHISTICATED_STORAGE_DISPLAY_MIXIN = "dev.hoyin1600p.arcanebeam.mixin.SophisticatedStorageDisplayItemRendererMixin";
    private static final String SOPHISTICATED_STORAGE_BARREL_BAKED_MIXIN = "dev.hoyin1600p.arcanebeam.mixin.SophisticatedStorageBarrelBakedModelBaseMixin";
    private static final Set<String> OPTIONAL_SOPHISTICATED_STORAGE_MIXINS = Set.of(
            SOPHISTICATED_STORAGE_DISPLAY_MIXIN,
            SOPHISTICATED_STORAGE_BARREL_BAKED_MIXIN
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
            LOGGER.info("ArcaneBeam {} optional Sophisticated Storage mixin {} for target {}", targetPresent ? "applying" : "skipping", mixinClassName, targetClassName);
            return targetPresent;
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
}
