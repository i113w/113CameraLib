package com.i113w.camera_lib.mixin.plugin;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CameraLibMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".LevelRendererFrustumMixin")) {
            return !isRendererReplacementLoaded();
        }
        return true;
    }

    private static boolean isRendererReplacementLoaded() {
        FMLLoader loader = FMLLoader.getCurrentOrNull();
        if (loader == null) return false;

        LoadingModList modList = loader.getLoadingModList();
        return modList.getModFileById("embeddium") != null
                || modList.getModFileById("rubidium") != null
                || modList.getModFileById("sodium") != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
