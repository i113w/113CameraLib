package com.i113w.camera_lib.render;

import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.entity.RTSCameraEntity;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class RTSCameraRenderer extends EntityRenderer<RTSCameraEntity, EntityRenderState> {

    public RTSCameraRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(RTSCameraEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                CameraLibEntities.RTS_CAMERA.get(),
                RTSCameraRenderer::new
        );
    }
}
