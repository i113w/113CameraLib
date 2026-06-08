package com.i113w.camera_lib.math;

import com.i113w.camera_lib.CameraLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class MatrixCache {
    private static final Matrix4f cachedModelViewMatrix = new Matrix4f();
    private static final Matrix4f cachedProjectionMatrix = new Matrix4f();
    private static boolean matricesValid = false;

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 在实体渲染结束后捕获，此时的相机位姿是最准确的
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            cachedModelViewMatrix.set(event.getPoseStack().last().pose());
            cachedProjectionMatrix.set(event.getProjectionMatrix());
            matricesValid = true;
        }
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

    // 退出登录时调用，防止下次进入时读到过期矩阵
    public static void clear() {
        cachedModelViewMatrix.identity();
        cachedProjectionMatrix.identity();
        matricesValid = false;
    }
}