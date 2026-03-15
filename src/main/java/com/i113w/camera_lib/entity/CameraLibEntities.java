package com.i113w.camera_lib.entity;

import com.i113w.camera_lib.CameraLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CameraLibEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CameraLib.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RTSCameraEntity>> RTS_CAMERA = ENTITIES.register("rts_camera",
            () -> EntityType.Builder.<RTSCameraEntity>of(RTSCameraEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .noSave()
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("rts_camera"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}