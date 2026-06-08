package com.i113w.camera_lib.camera;

import com.i113w.camera_lib.config.CameraLibConfig;
import com.i113w.camera_lib.entity.CameraLibEntities;
import com.i113w.camera_lib.entity.RTSCameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RTSCameraController {
    private static final RTSCameraController INSTANCE = new RTSCameraController();

    public enum CameraStyle { FREE, RTS, ORTHOGRAPHIC }

    private boolean isActive = false;
    private CameraStyle currentStyle = CameraStyle.RTS;

    private RTSCameraEntity cameraEntity;
    private Entity originalViewEntity;

    private Vec3 targetPos = Vec3.ZERO;
    private float targetYaw = 0f;
    private float targetPitch = 40f;
    private float zoomLevel = 20f;

    private static final float LERP_SPEED = 0.2f;
    private static final double ORTHOGRAPHIC_VIEW_DISTANCE = 128.0;
    private static final long SHADER_FALLBACK_MESSAGE_INTERVAL_MS = 5000L;

    private long lastShaderFallbackMessageMs = 0L;

    public static RTSCameraController get() { return INSTANCE; }

    public void reset() {
        if (isActive) exitRTS();
        this.cameraEntity = null;
        this.originalViewEntity = null;
        this.isActive = false;
        this.currentStyle = CameraStyle.RTS;
        this.targetPos = Vec3.ZERO;
        this.lastShaderFallbackMessageMs = 0L;
    }

    public void toggleRTSMode() {
        if (isActive) exitRTS();
        else enterRTS();
    }

    public void enterMode(CameraStyle style) {
        if (isActive) exitRTS();
        this.currentStyle = style;
        enterRTS();
    }

    public void exitMode() {
        if (isActive) exitRTS();
    }

    public void toggleCameraStyle() {
        if (!isActive) return;
        switch (this.currentStyle) {
            case RTS -> switchToFree();
            case FREE -> switchToOrthographic();
            case ORTHOGRAPHIC -> switchToRts();
        }
    }

    public CameraStyle getCameraStyle() { return currentStyle; }
    public boolean isActive() { return isActive; }
    public boolean isGroundFocusedStyle() { return currentStyle == CameraStyle.RTS || currentStyle == CameraStyle.ORTHOGRAPHIC; }
    public float getZoomLevel() { return zoomLevel; }
    public Vec3 getFocusPosition() { return targetPos; }

    public boolean shouldUseOrthographicProjection() {
        if (!isActive || currentStyle != CameraStyle.ORTHOGRAPHIC) return false;
        return !ShaderPackDetector.isShaderPackActive();
    }

    public void updateShaderFallback() {
        if (isActive && currentStyle == CameraStyle.ORTHOGRAPHIC && ShaderPackDetector.isShaderPackActive()) {
            fallbackOrthographicToRts();
        }
    }

    private void enterRTS() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        this.originalViewEntity = mc.getCameraEntity();
        Vec3 playerPos = mc.player.getPosition(1.0f);
        this.zoomLevel = 20f;

        updateShaderFallback();

        if (isGroundFocusedStyle()) {
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
        if (isGroundFocusedStyle()) {
            this.targetPitch = Mth.clamp(this.targetPitch, CameraLibConfig.rtsPitchMin, CameraLibConfig.rtsPitchMax);
        } else {
            this.targetPitch = Mth.clamp(this.targetPitch, CameraLibConfig.freePitchMin, CameraLibConfig.freePitchMax);
        }
    }

    public void snapYaw(float step) {
        if (!isActive || !isGroundFocusedStyle()) return;
        this.targetYaw += step;
    }

    public void adjustYaw(float delta) {
        if (!isActive) return;
        this.targetYaw += delta;
    }

    public void tick(float partialTick) {
        if (!isActive || cameraEntity == null) return;
        updateShaderFallback();

        double goalX, goalY, goalZ;

        if (currentStyle == CameraStyle.RTS) {
            double orthoDist = this.zoomLevel * 4.0;
            Vec3 backward = Vec3.directionFromRotation(targetPitch, targetYaw).scale(-orthoDist);
            goalX = targetPos.x + backward.x;
            goalY = targetPos.y + backward.y;
            goalZ = targetPos.z + backward.z;
        } else if (currentStyle == CameraStyle.ORTHOGRAPHIC) {
            Vec3 backward = Vec3.directionFromRotation(targetPitch, targetYaw).scale(-ORTHOGRAPHIC_VIEW_DISTANCE);
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

        if (isGroundFocusedStyle()) {
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
        if (isGroundFocusedStyle()) {
            this.zoomLevel -= scrollDelta * CameraLibConfig.rtsZoomSpeed;
            this.zoomLevel = Mth.clamp(this.zoomLevel, CameraLibConfig.rtsZoomMin, CameraLibConfig.rtsZoomMax);
        } else {
            Vec3 forward = Vec3.directionFromRotation(targetPitch, targetYaw).scale(scrollDelta * 2.0);
            this.targetPos = this.targetPos.add(forward);
        }
    }

    private void switchToRts() {
        this.targetPos = projectCurrentViewToGround();
        this.currentStyle = CameraStyle.RTS;
        applyRtsRotationRules();
    }

    private void switchToOrthographic() {
        if (this.currentStyle == CameraStyle.FREE) {
            this.targetPos = projectCurrentViewToGround();
        }
        this.currentStyle = CameraStyle.ORTHOGRAPHIC;
        applyRtsRotationRules();
        updateShaderFallback();
    }

    private void switchToFree() {
        if (this.cameraEntity != null) {
            this.targetPos = this.cameraEntity.position();
        } else {
            Vec3 backward = Vec3.directionFromRotation(targetPitch, targetYaw).scale(-ORTHOGRAPHIC_VIEW_DISTANCE);
            this.targetPos = this.targetPos.add(backward);
        }
        this.currentStyle = CameraStyle.FREE;
        this.targetPitch = Mth.clamp(this.targetPitch, CameraLibConfig.freePitchMin, CameraLibConfig.freePitchMax);
    }

    private Vec3 projectCurrentViewToGround() {
        Minecraft mc = Minecraft.getInstance();
        double groundY = mc.player != null ? mc.player.getY() : 64.0;
        Vec3 forward = Vec3.directionFromRotation(targetPitch, targetYaw);
        if (forward.y < -0.1) {
            double dist = (targetPos.y - groundY) / -forward.y;
            return targetPos.add(forward.scale(dist));
        }
        return new Vec3(targetPos.x, groundY, targetPos.z);
    }

    private void applyRtsRotationRules() {
        float snap = Math.max(1.0f, CameraLibConfig.rtsSnapAngle);
        float halfSnap = snap / 2.0f;
        this.targetYaw = Math.round((targetYaw - halfSnap) / snap) * snap + halfSnap;
        this.targetPitch = Mth.clamp(targetPitch, CameraLibConfig.rtsPitchMin, CameraLibConfig.rtsPitchMax);
    }

    private void fallbackOrthographicToRts() {
        if (this.currentStyle != CameraStyle.ORTHOGRAPHIC) return;
        this.currentStyle = CameraStyle.RTS;
        showShaderFallbackMessage();
    }

    private void showShaderFallbackMessage() {
        long now = System.currentTimeMillis();
        if (now - this.lastShaderFallbackMessageMs < SHADER_FALLBACK_MESSAGE_INTERVAL_MS) return;
        this.lastShaderFallbackMessageMs = now;

        Minecraft mc = Minecraft.getInstance();
        Component message = Component.translatable("message.i113w_camera_lib.orthographic_shader_fallback");
        if (mc.player != null) {
            mc.player.displayClientMessage(message, false);
            mc.player.displayClientMessage(message, true);
        } else if (mc.gui != null) {
            mc.gui.setOverlayMessage(message, false);
        }
    }
}
