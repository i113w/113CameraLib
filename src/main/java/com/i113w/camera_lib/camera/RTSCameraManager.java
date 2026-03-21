package com.i113w.camera_lib.camera;

import com.i113w.camera_lib.config.CameraLibConfig;
import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.entity.RTSCameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RTSCameraManager {

    private static final RTSCameraManager INSTANCE = new RTSCameraManager();

    public enum CameraStyle {
        FREE, // 原版自由飞行视角
        RTS   // 等距锁定视角 (模拟正交)
    }

    private boolean isActive = false;
    private CameraStyle currentStyle = CameraStyle.RTS;

    private RTSCameraEntity cameraEntity;
    private Entity originalViewEntity;

    private Vec3 targetPos = Vec3.ZERO;
    private float targetYaw = 0f;
    private float targetPitch = 40f;
    private float zoomLevel = 20f;

    private static final float LERP_SPEED = 0.2f;

    private RTSCameraManager() {}

    public static RTSCameraManager get() { return INSTANCE; }

    public void toggleRTSMode() {
        if (isActive) exitRTS();
        else enterRTS();
    }

    public void toggleCameraStyle() {
        if (!isActive) return;
        if (this.currentStyle == CameraStyle.FREE) {
            this.currentStyle = CameraStyle.RTS;
            double groundY = Minecraft.getInstance().player != null
                    ? Minecraft.getInstance().player.getY() : 64.0;
            Vec3 forward = Vec3.directionFromRotation(targetPitch, targetYaw);
            if (forward.y < -0.1) {
                double dist = (targetPos.y - groundY) / -forward.y;
                this.targetPos = targetPos.add(forward.scale(dist));
            } else {
                this.targetPos = new Vec3(targetPos.x, groundY, targetPos.z);
            }
            float snap = CameraLibConfig.rtsSnapAngle;
            if (snap > 0) {
                this.targetYaw = Math.round((targetYaw - snap/2f) / snap) * snap + snap/2f;
            }
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
            float rawYaw = mc.player.getYRot();
            float snap = CameraLibConfig.rtsSnapAngle;
            this.targetYaw = snap > 0
                    ? Math.round((rawYaw - snap / 2f) / snap) * snap + snap / 2f
                    : rawYaw;
            this.targetPitch = (CameraLibConfig.rtsPitchMin + CameraLibConfig.rtsPitchMax) / 2.0f;
        } else {
            this.targetPos  = playerPos.add(0, zoomLevel, 0);
            this.targetYaw  = mc.player.getYRot();
            this.targetPitch = 60f;
        }

        int minHeight = mc.level.getMinBuildHeight();
        if (this.targetPos.y < minHeight + 5) {
            this.targetPos = new Vec3(this.targetPos.x, minHeight + 10, this.targetPos.z);
        }

        this.cameraEntity = new RTSCameraEntity(CameraLibEntities.RTS_CAMERA.get(), mc.level);
        this.cameraEntity.setPos(this.targetPos);
        this.cameraEntity.setYRot(targetYaw);
        this.cameraEntity.setXRot(targetPitch);

        // 劫持玩家相机
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
        this.currentStyle = CameraStyle.RTS;
    }

     // 退出登录或强制重置时调用，保证所有状态归零。
    public void reset() {
        if (isActive) exitRTS();
        this.originalViewEntity = null;
        this.targetPos   = Vec3.ZERO;
        this.targetYaw   = 0f;
        this.targetPitch = 40f;
        this.zoomLevel   = 20f;
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
        float curYaw   = cameraEntity.getYRot() + yawDiff * LERP_SPEED;
        float curPitch = Mth.lerp(LERP_SPEED, cameraEntity.getXRot(), targetPitch);

        cameraEntity.setPos(curX, curY, curZ);
        cameraEntity.setYRot(curYaw);
        cameraEntity.setXRot(curPitch);

        cameraEntity.xo     = curX;
        cameraEntity.yo     = curY;
        cameraEntity.zo     = curZ;
        cameraEntity.yRotO  = curYaw;
        cameraEntity.xRotO  = curPitch;
    }

    public void handleInput(float moveX, float moveZ, float rotateYaw, float moveY) {
        if (!isActive) return;

        float moveSpeed = CameraLibConfig.moveBaseSpeed;
        if (Minecraft.getInstance().options.keySprint.isDown()) {
            moveSpeed *= CameraLibConfig.moveSprintMultiplier;
        }

        float sin = Mth.sin(targetYaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(targetYaw * Mth.DEG_TO_RAD);

        double dx = (moveX * cos - moveZ * sin) * moveSpeed;
        double dz = (moveZ * cos + moveX * sin) * moveSpeed;
        double dy = moveY * moveSpeed;

        int minHeight = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getMinBuildHeight() : -64;

        if (this.currentStyle == CameraStyle.RTS) {
            this.targetPos = this.targetPos.add(dx, dy, dz);
            double clampedY = Mth.clamp(this.targetPos.y, minHeight, 320);
            this.targetPos = new Vec3(this.targetPos.x, clampedY, this.targetPos.z);
        } else {
            this.targetPos = this.targetPos.add(dx, dy, dz);
            this.targetYaw += rotateYaw * CameraLibConfig.freeRotationSpeed;
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