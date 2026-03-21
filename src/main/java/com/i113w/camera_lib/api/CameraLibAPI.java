package com.i113w.camera_lib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public final class CameraLibAPI {
    private static final CameraLibAPI INSTANCE = new CameraLibAPI();

    // 默认提供一个空实现（No-Op），防止在未注册时调用引发 NPE
    private final AtomicReference<IRTSInteractionDelegate> delegate = new AtomicReference<>(new IRTSInteractionDelegate() {
        @Override public boolean isSelectable(Entity entity) { return false; }
        @Override public boolean isEnemy(Entity entity) { return false; }
        @Override public @Nullable ResourceLocation getCursorTexture(CursorType type) { return null; }
    });

    private CameraLibAPI() {}

    public static CameraLibAPI get() {
        return INSTANCE;
    }

    /** 供主模组在初始化阶段（如 FMLClientSetupEvent）注册自己的委托 */
    public void registerDelegate(IRTSInteractionDelegate newDelegate) {
        if (newDelegate != null) {
            delegate.set(newDelegate);
        }
    }

    public IRTSInteractionDelegate delegate() {
        return delegate.get();
    }
}