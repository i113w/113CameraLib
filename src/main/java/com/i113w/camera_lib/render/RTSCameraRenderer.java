package com.i113w.camera_lib.render;

import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.entity.RTSCameraEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.NotNull;

public class RTSCameraRenderer extends EntityRenderer<RTSCameraEntity> {

    public RTSCameraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(@NotNull RTSCameraEntity entity, @NotNull Frustum frustum,
                                double camX, double camY, double camZ) {
        // 绝对不渲染实体本身
        return false;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull RTSCameraEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    /**
     * 注册渲染器
     * 此方法已在 CameraLib 主类的 DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...) 中
     * 通过 modEventBus.addListener 绑定到 MOD 总线上。
     */
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CameraLibEntities.RTS_CAMERA.get(), RTSCameraRenderer::new);
    }
}