package com.i113w.camera_lib.camera;

import com.i113w.camera_lib.config.CameraLibConfig;
import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.entity.RTSCameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RTSCameraController {
    private static final RTSCameraController INSTANCE = new RTSCameraController();

    public enum CameraStyle { FREE, RTS }

    private boolean isActive = false;
    private CameraStyle currentStyle = CameraStyle.RTS;

    private RTSCameraEntity cameraEntity;
    private Entity originalViewEntity;

    private Vec3 targetPos = Vec3.ZERO;
    private float targetYaw = 0f;
    private float targetPitch = 40f;
    private float zoomLevel = 20f;

    private static final float LERP_SPEED = 0.2f;

    public static RTSCameraController get() { return INSTANCE; }

    public void reset() {
        if (isActive) exitRTS();
        this.cameraEntity = null;
        this.originalViewEntity = null;
        this.isActive = false;
        this.currentStyle = CameraStyle.RTS;
        this.targetPos = Vec3.ZERO;
    }

    public void toggleRTSMode() {
        if (isActive) exitRTS();
        else enterRTS();
    }

    public void toggleCameraStyle() {
        if (!isActive) return;
        if (this.currentStyle == CameraStyle.FREE) {
            this.currentStyle = CameraStyle.RTS;
            double groundY = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getY() : 64.0;
            Vec3 forward = Vec3.directionFromRotation(targetPitch, targetYaw);
            if (forward.y < -0.1) {
                double dist = (targetPos.y - groundY) / -forward.y;
                this.targetPos = targetPos.add(forward.scale(dist));
            } else {
                this.targetPos = new Vec3(targetPos.x, groundY, targetPos.z);
            }

            // 使用配置的吸附角度
            float snap = Math.max(1.0f, CameraLibConfig.rtsSnapAngle);
            float halfSnap = snap / 2.0f;
            this.targetYaw = Math.round((targetYaw - halfSnap) / snap) * snap + halfSnap;
            this.targetPitch = Mth.clamp(targetPitch, CameraLibConfig.rtsPitchMin, CameraLibConfig.rtsPitchMax);
        } else {
            this.currentStyle = CameraStyle.FREE;
            double orthoDist = this.zoomLevel * 3.0;
            Vec3 backward = Vec3.directionFromRotation(targetPitch, targetYaw).scale(-orthoDist);
            this.targetPos = targetPos.add(backward);
        }
    }

    public CameraStyle getCameraStyle() { return currentStyle; }
    public boolean isActive() { return isActive; }

    private void enterRTS() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        this.originalViewEntity = mc.getCameraEntity();
        Vec3 playerPos = mc.player.getPosition(1.0f);
        this.zoomLevel = 20f;

        if (this.currentStyle == CameraStyle.RTS) {
            this.targetPos = new Vec3(playerPos.x, playerPos.y, playerPos.z);

            float snap = Math.max(1.0f, CameraLibConfig.rtsSnapAngle);
            float halfSnap = snap / 2.0f;
            this.targetYaw = Math.round((mc.player.getYRot() - halfSnap) / snap) * snap + halfSnap;

            this.targetPitch = Mth.clamp(40f, CameraLibConfig.rtsPitchMin, CameraLibConfig.rtsPitchMax);
        } else {
            this.targetPos = playerPos.add(0, zoomLevel, 0);
            this.targetYaw = mc.player.getYRot();
            this.targetPitch = 60f; // 初始默认
        }

        int minHeight = mc.level.getMinBuildHeight();
        if (this.targetPos.y < minHeight + 5) {
            this.targetPos = new Vec3(this.targetPos.x, minHeight + 10, this.targetPos.z);
        }

        this.cameraEntity = new RTSCameraEntity(CameraLibEntities.RTS_CAMERA.get(), mc.level);
        this.cameraEntity.setPos(this.targetPos);
        this.cameraEntity.setYRot(targetYaw);
        this.cameraEntity.setXRot(targetPitch);

        mc.level.addEntity(this.cameraEntity);
        mc.setCameraEntity(this.cameraEntity);
        this.isActive = true;
    }

    private void exitRTS() {
        Minecraft mc = Minecraft.getInstance();
        if (originalViewEntity != null) {
            mc.setCameraEntity(originalViewEntity);
        } else if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        if (cameraEntity != null) {
            cameraEntity.remove(Entity.RemovalReason.DISCARDED);
            cameraEntity = null;
        }
        this.isActive = false;
    }

    public void adjustPitch(float delta) {
        if (!isActive) return;
        this.targetPitch += delta;
        if (this.currentStyle == CameraStyle.RTS) {
            this.targetPitch = Mth.clamp(this.targetPitch, CameraLibConfig.rtsPitchMin, CameraLibConfig.rtsPitchMax);
        } else {
            this.targetPitch = Mth.clamp(this.targetPitch, CameraLibConfig.freePitchMin, CameraLibConfig.freePitchMax);
        }
    }

    public void snapYaw(float step) {
        if (!isActive || currentStyle != CameraStyle.RTS) return;
        this.targetYaw += step;
    }

    public void tick(float partialTick) {
        if (!isActive || cameraEntity == null) return;
        double goalX, goalY, goalZ;

        if (currentStyle == CameraStyle.RTS) {
            double orthoDist = this.zoomLevel * 4.0;
            Vec3 backward = Vec3.directionFromRotation(targetPitch, targetYaw).scale(-orthoDist);
            goalX = targetPos.x + backward.x;
            goalY = targetPos.y + backward.y;
            goalZ = targetPos.z + backward.z;
        } else {
            goalX = targetPos.x;
            goalY = targetPos.y;
            goalZ = targetPos.z;
        }

        double curX = Mth.lerp(LERP_SPEED, cameraEntity.getX(), goalX);
        double curY = Mth.lerp(LERP_SPEED, cameraEntity.getY(), goalY);
        double curZ = Mth.lerp(LERP_SPEED, cameraEntity.getZ(), goalZ);

        float yawDiff = Mth.wrapDegrees(targetYaw - cameraEntity.getYRot());
        float curYaw = cameraEntity.getYRot() + yawDiff * LERP_SPEED;
        float curPitch = Mth.lerp(LERP_SPEED, cameraEntity.getXRot(), targetPitch);

        cameraEntity.setPos(curX, curY, curZ);
        cameraEntity.setYRot(curYaw);
        cameraEntity.setXRot(curPitch);

        cameraEntity.xo = curX;
        cameraEntity.yo = curY;
        cameraEntity.zo = curZ;
        cameraEntity.yRotO = curYaw;
        cameraEntity.xRotO = curPitch;
    }

    public void handleInput(float moveX, float moveZ, float rotateYaw, float zoomDelta, float moveY, boolean sprintDown) {
        if (!isActive) return;
        float moveSpeed = sprintDown ? CameraLibConfig.moveSprintMultiplier : CameraLibConfig.moveBaseSpeed;

        float sin = Mth.sin(targetYaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(targetYaw * Mth.DEG_TO_RAD);

        double dx = (moveX * cos - moveZ * sin) * moveSpeed;
        double dz = (moveZ * cos + moveX * sin) * moveSpeed;
        double dy = moveY * moveSpeed;

        int minHeight = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getMinBuildHeight() : -64;

        if (this.currentStyle == CameraStyle.RTS) {
            this.targetPos = this.targetPos.add(dx, dy, dz);
            double clampedY = Mth.clamp(this.targetPos.y, minHeight, 320);
            this.targetPos = new Vec3(this.targetPos.x, clampedY, this.targetPos.z);
        } else {
            this.targetPos = this.targetPos.add(dx, dy, dz);
            // 应用自由相机旋转速度
            this.targetYaw += rotateYaw * CameraLibConfig.freeRotationSpeed;
            this.targetPos = this.targetPos.add(0, zoomDelta * -2.0, 0);
            double clampedY = Mth.clamp(this.targetPos.y, minHeight + 5, 320);
            this.targetPos = new Vec3(this.targetPos.x, clampedY, this.targetPos.z);
        }
    }

    public void handleZoom(float scrollDelta) {
        if (!isActive) return;
        if (this.currentStyle == CameraStyle.RTS) {
            this.zoomLevel -= scrollDelta * CameraLibConfig.rtsZoomSpeed;
            this.zoomLevel = Mth.clamp(this.zoomLevel, CameraLibConfig.rtsZoomMin, CameraLibConfig.rtsZoomMax);
        } else {
            Vec3 forward = Vec3.directionFromRotation(targetPitch, targetYaw).scale(scrollDelta * 2.0);
            this.targetPos = this.targetPos.add(forward);
        }
    }
}