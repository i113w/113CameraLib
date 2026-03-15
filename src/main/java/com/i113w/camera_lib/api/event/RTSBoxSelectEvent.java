package com.i113w.camera_lib.api.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import java.util.List;

/**
 * 当玩家完成左键框选或单点时触发
 */
public class RTSBoxSelectEvent extends Event {
    private final List<Entity> candidates;

    public RTSBoxSelectEvent(List<Entity> candidates) {
        this.candidates = candidates;
    }

    /**
     * 获取经过 delegate 过滤后的最终候选实体列表
     */
    public List<Entity> getCandidates() {
        return candidates;
    }
}