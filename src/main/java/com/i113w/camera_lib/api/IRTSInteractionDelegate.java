package com.i113w.camera_lib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface IRTSInteractionDelegate {
    /**
     * 判断该实体是否允许被左键框选（例如：只有自己的队伍成员才能被框选）
     */
    boolean isSelectable(Entity entity);

    /**
     * 获取当前鼠标悬停状态下应该显示的光标贴图
     * @param hoveredEntity 当前鼠标悬停的实体（可能为 null）
     * @param isAttackDragging 是否正在右键拉红框
     */
    ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging);

    // 默认实现
    IRTSInteractionDelegate DEFAULT = new IRTSInteractionDelegate() {
        @Override
        public boolean isSelectable(Entity entity) { return true; }
        @Override
        public ResourceLocation getCursorIcon(@Nullable Entity hoveredEntity, boolean isAttackDragging) {
            return ResourceLocation.withDefaultNamespace("textures/gui/crosshairs.png"); // 默认光标
        }
    };
}