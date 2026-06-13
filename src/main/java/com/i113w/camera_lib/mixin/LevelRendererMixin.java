package com.i113w.camera_lib.mixin;

import com.i113w.camera_lib.camera.RTSCameraController;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
            method = "addCloudsPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/CloudStatus;Lnet/minecraft/world/phys/Vec3;JFIFI)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void i113wCameraLib$skipCloudsInOrthographicMode(
            FrameGraphBuilder frame,
            CloudStatus cloudStatus,
            Vec3 cameraPosition,
            long gameTime,
            float partialTicks,
            int cloudColor,
            float cloudHeight,
            int cloudRange,
            CallbackInfo ci
    ) {
        if (RTSCameraController.get().shouldUseOrthographicProjection()) {
            ci.cancel();
        }
    }
}
