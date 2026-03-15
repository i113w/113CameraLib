package com.i113w.camera_lib;

import com.i113w.camera_lib.config.CameraLibConfig;
import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.input.CameraLibKeyMappings;
import com.i113w.camera_lib.render.RTSCameraRenderer;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(CameraLib.MODID)
public class CameraLib {
    public static final String MODID = "i113w_camera_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CameraLib(IEventBus modEventBus, ModContainer modContainer) {
        // 注册客户端专有配置
        modContainer.registerConfig(ModConfig.Type.CLIENT, CameraLibConfig.SPEC);

        // 监听配置加载与重载事件
        modEventBus.addListener(CameraLibConfig::onLoad);
        modEventBus.addListener(CameraLibConfig::onReload);

        // 注册实体
        CameraLibEntities.register(modEventBus);

        // 客户端专有注册
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(CameraLibKeyMappings::onRegisterKeyMappings);
            modEventBus.addListener(RTSCameraRenderer::onRegisterRenderers);
        }

        LOGGER.info("113's Camera Lib initialized!");
    }
}