package com.i113w.camera_lib.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class RTSCameraEntity extends Entity {
    public RTSCameraEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        // 位置完全由 Camera Manager 掌控，屏蔽原版物理计算
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(@NotNull CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(@NotNull CompoundTag tag) {}

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException("RTSCameraEntity is client-only!");
    }
}