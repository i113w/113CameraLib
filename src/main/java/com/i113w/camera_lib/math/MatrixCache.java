package com.i113w.camera_lib.math;

import com.i113w.camera_lib.CameraLib;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class MatrixCache {
    private static final Matrix4f cachedModelViewMatrix = new Matrix4f();
    private static final Matrix4f cachedProjectionMatrix = new Matrix4f();
    private static boolean matricesValid = false;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        // 在实体渲染后捕获矩阵，确保 3D 转 2D 投影正确
        cachedModelViewMatrix.set(event.getModelViewMatrix());
        cachedProjectionMatrix.set(event.getLevelRenderState().cameraRenderState.projectionMatrix);
        matricesValid = true;
    }

    public static Matrix4f getModelViewMatrix() {
        return cachedModelViewMatrix;
    }

    public static Matrix4f getProjectionMatrix() {
        return cachedProjectionMatrix;
    }

    public static boolean isValid() {
        return matricesValid;
    }

    public static void clear() {
        cachedModelViewMatrix.identity();
        cachedProjectionMatrix.identity();
        matricesValid = false;
    }
}
