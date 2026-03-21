package com.i113w.camera_lib.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

import java.util.Collections;
import java.util.List;

public class RTSBoxSelectEvent extends Event {
    private final Player player;
    private final List<Entity> selectedEntities;

    public RTSBoxSelectEvent(Player player, List<Entity> selectedEntities) {
        this.player = player;
        this.selectedEntities = Collections.unmodifiableList(selectedEntities);
    }

    public Player getPlayer() {
        return player;
    }

    public List<Entity> getSelectedEntities() {
        return selectedEntities;
    }
}