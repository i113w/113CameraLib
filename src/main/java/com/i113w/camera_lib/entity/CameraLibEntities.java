package com.i113w.camera_lib.entity;

import com.i113w.camera_lib.CameraLib;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CameraLibEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CameraLib.MODID);

    public static final RegistryObject<EntityType<RTSCameraEntity>> RTS_CAMERA = ENTITIES.register(
            "rts_camera",
            () -> EntityType.Builder.<RTSCameraEntity>of(RTSCameraEntity::new, MobCategory.MISC)
                    .sized(0.0f, 0.0f)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(0) // 禁用服务端追踪，仅客户端本地生成
                    .updateInterval(Integer.MAX_VALUE)
                    .build("rts_camera")
    );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}