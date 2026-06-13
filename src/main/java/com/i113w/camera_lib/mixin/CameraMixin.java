package com.i113w.camera_lib.mixin;

import com.i113w.camera_lib.camera.OrthographicProjection;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void i113wCameraLib$useOrthographicProjection(CameraRenderState cameraState, float cameraEntityPartialTicks, CallbackInfo ci) {
        RTSCameraController controller = RTSCameraController.get();
        if (!controller.shouldUseOrthographicProjection()) return;

        Window window = Minecraft.getInstance().getWindow();
        float aspect = window.getHeight() == 0 ? 1.0f : (float) window.getWidth() / (float) window.getHeight();
        cameraState.projectionMatrix.set(OrthographicProjection.create(controller.getOrthographicVisibleWidth(), aspect));
    }
}
