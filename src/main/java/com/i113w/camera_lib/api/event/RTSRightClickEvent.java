package com.i113w.camera_lib.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.Event;
import java.util.List;

/**
 * 当玩家在 RTS 模式下按下右键或完成右键拖拽时触发
 */
public class RTSRightClickEvent extends Event {
    private final HitResult singleHitResult;
    private final List<Entity> dragTargets;
    private final boolean isDrag;

    public RTSRightClickEvent(HitResult singleHitResult) {
        this.singleHitResult = singleHitResult;
        this.dragTargets = List.of();
        this.isDrag = false;
    }

    public RTSRightClickEvent(List<Entity> dragTargets) {
        this.singleHitResult = null;
        this.dragTargets = dragTargets;
        this.isDrag = true;
    }

    public boolean isDrag() { return isDrag; }
    public HitResult getSingleHitResult() { return singleHitResult; }
    public List<Entity> getDragTargets() { return dragTargets; }
}