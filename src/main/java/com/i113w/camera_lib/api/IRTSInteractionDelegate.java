package com.i113w.camera_lib.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface IRTSInteractionDelegate {
    /** 判断实体是否允许被左键框选/点选 */
    boolean isSelectable(Entity entity);

    /** 判断实体是否为敌对方（用于鼠标指针变红等视觉反馈） */
    boolean isEnemy(Entity entity);

    /** 获取自定义光标纹理，返回 null 则使用默认光标 */
    @Nullable
    ResourceLocation getCursorTexture(CursorType type);

    enum CursorType {
        DEFAULT, ATTACK, ALLY, DRAG_SELECT
    }
}