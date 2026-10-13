package dev.hoyin1600p.arcanebeam.mixin.manastealer;

import dev.hoyin1600p.arcanebeam.hook.ManaStealerMixinState;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import static dev.hoyin1600p.arcanebeam.util.ArcaneBeamIds.VAULT_NAMESPACE;

public final class ManaStealerMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("ArcaneBeam");

    private boolean applicable;
    private String declineReason;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            applicable = FMLEnvironment.dist == Dist.CLIENT
                    && FMLLoader.getLoadingModList() != null
                    && FMLLoader.getLoadingModList().getModFileById(VAULT_NAMESPACE) != null
                    && ManaStealerMixinState.owns();
        } catch (RuntimeException | LinkageError failure) {
            applicable = false;
            declineReason = failure.toString();
        }
        if (!applicable) {
            ManaStealerMixinState.markMixinsDeclined();
            LOGGER.warn(
                    "Mana Stealer mixins NOT applied: {}",
                    declineReason != null ? declineReason : "not a client with the_vault present, or legacy VRO module owns the visuals"
            );
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return applicable;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
        ManaStealerMixinState.markMixinsApplied();
    }
}
