package com.i113w.camera_lib.mixin;

import com.i113w.camera_lib.camera.RTSCameraController;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class LevelRendererFrustumMixin {
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
}
