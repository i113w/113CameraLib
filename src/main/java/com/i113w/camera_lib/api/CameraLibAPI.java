package com.i113w.camera_lib.api;

import java.util.HashSet;
import java.util.Set;

public final class CameraLibAPI {
    private static final CameraLibAPI INSTANCE = new CameraLibAPI();

    private IRTSInteractionDelegate interactionDelegate = IRTSInteractionDelegate.DEFAULT;

    // 供渲染层使用的高亮缓存（替代原 SelectionManager 的职责）
    private final Set<Integer> selectedEntityIds = new HashSet<>();
    private int hoveredEntityId = -1;

    private CameraLibAPI() {}

    public static CameraLibAPI get() {
        return INSTANCE;
    }

    // 供主模组在初始化阶段注册自己的委托
    public void setInteractionDelegate(IRTSInteractionDelegate delegate) {
        if (delegate != null) {
            this.interactionDelegate = delegate;
        }
    }

    public IRTSInteractionDelegate getDelegate() {
        return interactionDelegate;
    }

     // 主模组同步当前的选区给库，以便库渲染白色高亮边框
    public void setSelectedEntities(Set<Integer> entityIds) {
        this.selectedEntityIds.clear();
        if (entityIds != null) {
            this.selectedEntityIds.addAll(entityIds);
        }
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