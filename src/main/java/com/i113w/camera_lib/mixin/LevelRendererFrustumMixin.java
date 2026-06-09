package com.i113w.camera_lib.mixin;

import com.i113w.camera_lib.camera.RTSCameraController;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class LevelRendererFrustumMixin {
    @Redirect(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/culling/Frustum;offsetToFullyIncludeCameraCube(I)Lnet/minecraft/client/renderer/culling/Frustum;"
            )
    )
    private Frustum i113wCameraLib$skipCameraCubeFrustumOffset(Frustum frustum, int step) {
        if (RTSCameraController.get().shouldUseOrthographicProjection()) {
            return frustum;
        }
        return frustum.offsetToFullyIncludeCameraCube(step);
    }
}
