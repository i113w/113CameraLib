package com.i113w.camera_lib.selection;

import com.i113w.camera_lib.math.ScreenProjector;
import net.minecraft.world.phys.Vec2;

public class RTSSelectionManager {
    private static final RTSSelectionManager INSTANCE = new RTSSelectionManager();

    private boolean isDragging = false;
    private boolean isRightDragging = false;

    private Vec2 dragStart = Vec2.ZERO;
    private Vec2 dragEnd = Vec2.ZERO;
    private Vec2 rightDragStart = Vec2.ZERO;
    private Vec2 rightDragEnd = Vec2.ZERO;

    public static RTSSelectionManager get() { return INSTANCE; }

    public void reset() {
        this.isDragging = false;
        this.isRightDragging = false;
        this.dragStart = Vec2.ZERO;
        this.dragEnd = Vec2.ZERO;
        this.rightDragStart = Vec2.ZERO;
        this.rightDragEnd = Vec2.ZERO;
    }

    public void startDrag(float x, float y) { this.isDragging = true; this.dragStart = new Vec2(x, y); this.dragEnd = this.dragStart; }
    public void updateDrag(float x, float y) { if (isDragging) this.dragEnd = new Vec2(x, y); }
    public void endDrag() { this.isDragging = false; }
    public boolean isDragging() { return isDragging; }

    public void startRightDrag(float x, float y) { this.isRightDragging = true; this.rightDragStart = new Vec2(x, y); this.rightDragEnd = this.rightDragStart; }
    public void updateRightDrag(float x, float y) { if (isRightDragging) this.rightDragEnd = new Vec2(x, y); }
    public void endRightDrag() { this.isRightDragging = false; }
    public boolean isRightDragging() { return isRightDragging; }

    public ScreenProjector.ScreenRect getSelectionRect() {
        return createRect(dragStart, dragEnd);
    }

    public ScreenProjector.ScreenRect getRightDragRect() {
        return createRect(rightDragStart, rightDragEnd);
    }

    private ScreenProjector.ScreenRect createRect(Vec2 start, Vec2 end) {
        float minX = Math.min(start.x, end.x);
        float minY = Math.min(start.y, end.y);
        float maxX = Math.max(start.x, end.x);
        float maxY = Math.max(start.y, end.y);
        return new ScreenProjector.ScreenRect(minX, minY, maxX - minX, maxY - minY);
    }
}