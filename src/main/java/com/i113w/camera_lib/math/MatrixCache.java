package com.i113w.camera_lib.math;

import com.i113w.camera_lib.CameraLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class MatrixCache {
    public static final Matrix4f VIEW_MATRIX = new Matrix4f();
    public static final Matrix4f PROJ_MATRIX = new Matrix4f();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // 在实体渲染结束后捕获，此时的相机位姿是最准确的
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            VIEW_MATRIX.set(event.getPoseStack().last().pose());
            PROJ_MATRIX.set(event.getProjectionMatrix());
        }
    }

    // 退出登录时调用，防止下次进入时读到过期矩阵
    public static void clear() {
        VIEW_MATRIX.identity();
        PROJ_MATRIX.identity();
    }
}