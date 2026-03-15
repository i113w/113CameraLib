package com.i113w.camera_lib;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CameraLib.MODID)
public class CameraLib {
    public static final String MODID = "113_camera_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CameraLib(IEventBus modEventBus, ModContainer modContainer) {
        // 后续注册实体（如 RTSCameraEntity）、网络包或事件的入口
        LOGGER.info("113's Camera Lib initialized!");
    }
}