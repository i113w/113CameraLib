package com.i113w.camera_lib.render;

import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.entity.RTSCameraEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class RTSCameraRenderer extends EntityRenderer<RTSCameraEntity> {

    public RTSCameraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RTSCameraEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                CameraLibEntities.RTS_CAMERA.get(),
                RTSCameraRenderer::new
        );
    }
}