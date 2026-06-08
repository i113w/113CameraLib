package com.i113w.camera_lib.client;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.i113w.camera_lib.math.MatrixCache;
import com.i113w.camera_lib.selection.RTSSelectionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    /**
     * 玩家登出时，强制退出 RTS 模式并清理所有客户端状态，
     * 防止下次进入存档时残留数据污染新会话。
     */
    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RTSCameraController.get().reset();
        RTSSelectionManager.get().reset();
        CameraLibAPI.get().clearSelection();
        MatrixCache.clear();
    }
}
