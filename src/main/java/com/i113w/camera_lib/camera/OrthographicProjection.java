package com.i113w.camera_lib.camera;

import org.joml.Matrix4f;

public final class OrthographicProjection {
    public static final float NEAR_PLANE = 0.05f;
    public static final float FAR_PLANE = 1024.0f;

    private OrthographicProjection() {}

    public static Matrix4f create(float visibleWidth, float aspectRatio) {
        float safeWidth = Math.max(1.0f, visibleWidth);
        float safeAspect = Math.max(0.01f, aspectRatio);
        float halfWidth = safeWidth * 0.5f;
        float halfHeight = halfWidth / safeAspect;
        return new Matrix4f().setOrtho(
                -halfWidth, halfWidth,
                -halfHeight, halfHeight,
                NEAR_PLANE, FAR_PLANE
        );
    }
}
