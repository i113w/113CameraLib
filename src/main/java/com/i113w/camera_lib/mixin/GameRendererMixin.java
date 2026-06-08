package com.i113w.camera_lib.mixin;

import com.i113w.camera_lib.camera.OrthographicProjection;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getProjectionMatrix(D)Lorg/joml/Matrix4f;", at = @At("HEAD"), cancellable = true)
    private void i113wCameraLib$useOrthographicProjection(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        RTSCameraController controller = RTSCameraController.get();
        if (!controller.shouldUseOrthographicProjection()) return;

        Window window = Minecraft.getInstance().getWindow();
        float aspect = window.getHeight() == 0 ? 1.0f : (float) window.getWidth() / (float) window.getHeight();
        cir.setReturnValue(OrthographicProjection.create(controller.getZoomLevel(), aspect));
    }
}
