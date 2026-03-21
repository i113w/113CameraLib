package com.i113w.camera_lib.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collections;
import java.util.List;

public class RTSRightClickEvent extends Event {
    private final Player player;
    private final boolean isDrag;

    // 如果是拖拽框选，提供框内的目标实体
    private final List<Entity> draggedTargets;

    // 如果是单点点击，提供射线检测结果
    private final HitResult singleHitResult;

    public RTSRightClickEvent(Player player, boolean isDrag, List<Entity> draggedTargets, HitResult singleHitResult) {
        this.player = player;
        this.isDrag = isDrag;
        this.draggedTargets = draggedTargets != null ? Collections.unmodifiableList(draggedTargets) : Collections.emptyList();
        this.singleHitResult = singleHitResult;
    }

    public Player getPlayer() { return player; }
    public boolean isDrag() { return isDrag; }
    public List<Entity> getDraggedTargets() { return draggedTargets; }
    public HitResult getSingleHitResult() { return singleHitResult; }
}