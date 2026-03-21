package com.i113w.camera_lib.api;

import com.i113w.camera_lib.camera.RTSCameraManager;
import com.i113w.camera_lib.math.MatrixCache;
import com.i113w.camera_lib.selection.RTSSelectionManager;

import java.util.Set;

/**
 * 暴露给外部主模组的摄像机控制器 API。
 */
public final class CameraController {

    private CameraController() {}

    // ── 摄像机开关 ─────────────────────────────────────────────────────────────

    public static void toggleRTSMode() {
        RTSCameraManager.get().toggleRTSMode();
    }

    /**
     * 检查当前 RTS 摄像机是否处于激活状态
     */
    public static boolean isRTSActive() {
        return RTSCameraManager.get().isActive();
    }

    /**
     * 在 "等距锁定视角 (RTS)" 和 "自由视角 (FREE)" 之间切换
     */
    public static void toggleCameraStyle() {
        RTSCameraManager.get().toggleCameraStyle();
    }

    /**
     * 强制设置为 RTS 锁定视角
     */
    public static void setCameraStyleRTS() {
        if (isRTSActive() && RTSCameraManager.get().getCameraStyle() != RTSCameraManager.CameraStyle.RTS) {
            RTSCameraManager.get().toggleCameraStyle();
        }
    }

    /**
     * 强制设置为 自由飞行视角
     */
    public static void setCameraStyleFree() {
        if (isRTSActive() && RTSCameraManager.get().getCameraStyle() != RTSCameraManager.CameraStyle.FREE) {
            RTSCameraManager.get().toggleCameraStyle();
        }
    }

    /**
     * 全量重置：退出 RTS、清空选区、清空矩阵缓存。
     * 一般只在强制退出等异常场景下由主模组调用，
     * 正常的登出清理由库自身的 {@code ClientEventHandler} 处理。
     */
    public static void reset() {
        RTSCameraManager.get().reset();
        RTSSelectionManager.get().reset();
        MatrixCache.clear();
    }

    // ── 选区读写（供主模组在 RTSBoxSelectEvent 回调中使用）────────────────────

    /**
     * 将主模组处理后的最终选区写回库，库据此渲染高亮边框。
     */
    public static void setSelectedEntityIds(Set<Integer> ids) {
        RTSSelectionManager.get().setSelectedIds(ids);
    }

    public static Set<Integer> getSelectedEntityIds() {
        return RTSSelectionManager.get().getSelectedIds();
    }

    public static void clearSelection() {
        RTSSelectionManager.get().clearSelection();
    }
}