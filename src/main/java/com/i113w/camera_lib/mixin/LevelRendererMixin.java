package com.i113w.camera_lib.mixin;

import com.i113w.camera_lib.camera.RTSCameraController;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
            method = "offsetFrustum(Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/client/renderer/culling/Frustum;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void i113wCameraLib$skipCameraCubeFrustumOffset(Frustum frustum, CallbackInfoReturnable<Frustum> cir) {
        if (RTSCameraController.get().shouldUseOrthographicProjection()) {
            cir.setReturnValue(frustum);
        }
    }

    @Inject(
            method = "renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void i113wCameraLib$skipCloudsInOrthographicMode(
            PoseStack poseStack,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci
    ) {
        if (RTSCameraController.get().shouldUseOrthographicProjection()) {
            ci.cancel();
        }
    }
}
