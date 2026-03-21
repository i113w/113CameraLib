package com.i113w.camera_lib.selection;

import net.minecraft.world.phys.Vec2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RTSSelectionManager {
    private static final RTSSelectionManager INSTANCE = new RTSSelectionManager();

    private boolean isLeftDragging = false;
    private boolean isRightDragging = false;

    private Vec2 leftStart = Vec2.ZERO;
    private Vec2 leftEnd   = Vec2.ZERO;
    private Vec2 rightStart = Vec2.ZERO;
    private Vec2 rightEnd   = Vec2.ZERO;

    private int hoveredEntityId = -1;

    /** 当前被选中的实体 ID 集合，由主模组通过事件回调写入 */
    private final Set<Integer> selectedEntityIds = new HashSet<>();

    private RTSSelectionManager() {}
    public static RTSSelectionManager get() { return INSTANCE; }

    // ── 拖拽状态（不变）────────────────────────────────────────────────────────

    public void startLeftDrag(float x, float y) {
        isLeftDragging = true;
        leftStart = new Vec2(x, y);
        leftEnd   = leftStart;
    }
    public void updateLeftDrag(float x, float y) { if (isLeftDragging) leftEnd = new Vec2(x, y); }
    public void endLeftDrag() { isLeftDragging = false; }
    public boolean isLeftDragging() { return isLeftDragging; }

    public void startRightDrag(float x, float y) {
        isRightDragging = true;
        rightStart = new Vec2(x, y);
        rightEnd   = rightStart;
    }
    public void updateRightDrag(float x, float y) { if (isRightDragging) rightEnd = new Vec2(x, y); }
    public void endRightDrag() { isRightDragging = false; }
    public boolean isRightDragging() { return isRightDragging; }

    public SelectionRect getLeftRect()  { return buildRect(leftStart, leftEnd); }
    public SelectionRect getRightRect() { return buildRect(rightStart, rightEnd); }

    private SelectionRect buildRect(Vec2 start, Vec2 end) {
        float minX = Math.min(start.x, end.x);
        float minY = Math.min(start.y, end.y);
        float maxX = Math.max(start.x, end.x);
        float maxY = Math.max(start.y, end.y);
        return new SelectionRect(minX, minY, maxX - minX, maxY - minY);
    }

    // ── 悬停 ───────────────────────────────────────────────────────────

    public void setHoveredEntityId(int id) { this.hoveredEntityId = id; }
    public int  getHoveredEntityId()       { return hoveredEntityId; }

    // ── 选区状态 ───────────────────────────────────────────────────────

    /**
     * 由主模组在处理 {@link com.i113w.camera_lib.api.event.RTSBoxSelectEvent} 后调用，
     * 将最终选中的实体 ID 写回库，供渲染高亮边框使用。
     */
    public void setSelectedIds(Set<Integer> ids) {
        selectedEntityIds.clear();
        if (ids != null) selectedEntityIds.addAll(ids);
    }

    /** 返回不可变快照，避免外部直接修改 */
    public Set<Integer> getSelectedIds() {
        return Collections.unmodifiableSet(selectedEntityIds);
    }

    public boolean isSelected(int entityId) {
        return selectedEntityIds.contains(entityId);
    }

    public void clearSelection() {
        selectedEntityIds.clear();
    }

    // ── 全量重置（退出登录 / 强制退出时使用）─────────────────────────────────

    public void reset() {
        isLeftDragging  = false;
        isRightDragging = false;
        leftStart  = leftEnd  = Vec2.ZERO;
        rightStart = rightEnd = Vec2.ZERO;
        hoveredEntityId = -1;
        selectedEntityIds.clear();
    }

    // ── 选框记录类 ─────────────────────────────────────────────────────

    public record SelectionRect(float x, float y, float width, float height) {
        public boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }
}