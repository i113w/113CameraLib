package com.i113w.camera_lib.client;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.i113w.camera_lib.math.MatrixCache;
import com.i113w.camera_lib.selection.RTSSelectionManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RTSCameraController.get().reset();
        RTSSelectionManager.get().reset();
        CameraLibAPI.get().clearSelection();
        MatrixCache.clear();
    }
}