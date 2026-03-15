package com.i113w.camera_lib.api;

import java.util.HashSet;
import java.util.Set;

public final class CameraLibAPI {
    private static final CameraLibAPI INSTANCE = new CameraLibAPI();

    private IRTSInteractionDelegate interactionDelegate = IRTSInteractionDelegate.DEFAULT;

    // 供渲染层使用的高亮缓存
    private final Set<Integer> selectedEntityIds = new HashSet<>();
    private int hoveredEntityId = -1;

    private CameraLibAPI() {}

    public static CameraLibAPI get() {
        return INSTANCE;
    }

    public void setInteractionDelegate(IRTSInteractionDelegate delegate) {
        this.interactionDelegate = delegate;
    }

    public IRTSInteractionDelegate getDelegate() {
        return interactionDelegate;
    }

    /**
     * 主模组同步当前的选区给库，以便库渲染绿色高亮边框
     */
    public void setSelectedEntities(Set<Integer> entityIds) {
        this.selectedEntityIds.clear();
        this.selectedEntityIds.addAll(entityIds);
    }

    public Set<Integer> getSelectedEntities() {
        return new HashSet<>(selectedEntityIds);
    }

    public void setHoveredEntityId(int id) {
        this.hoveredEntityId = id;
    }

    public int getHoveredEntityId() {
        return hoveredEntityId;
    }

    public void clearSelection() {
        this.selectedEntityIds.clear();
        this.hoveredEntityId = -1;
    }
}