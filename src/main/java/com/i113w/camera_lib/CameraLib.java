package com.i113w.camera_lib;

import com.i113w.camera_lib.config.CameraLibConfig;
import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.input.CameraLibKeyMappings;
import com.i113w.camera_lib.render.RTSCameraRenderer;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CameraLib.MODID)
public class CameraLib {
    public static final String MODID = "i113w_camera_lib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CameraLib() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册客户端专有配置
        FMLJavaModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CameraLibConfig.SPEC);

        // 注册实体
        CameraLibEntities.register(modEventBus);

        // 客户端专有注册
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(CameraLibKeyMappings::onRegisterKeyMappings);
            modEventBus.addListener(RTSCameraRenderer::onRegisterRenderers);
        });

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("113's Camera Lib initialized!");
    }
}