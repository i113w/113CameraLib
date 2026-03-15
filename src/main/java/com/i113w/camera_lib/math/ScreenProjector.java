package com.i113w.camera_lib.math;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ScreenProjector {
    public record ScreenRect(float x, float y, float width, float height) {
        public boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    public static boolean isAABBInScreenRect(AABB aabb, ScreenRect rect, Vec3 camPos) {
        if (!MatrixCache.isValid()) return false;
        Matrix4f view = MatrixCache.getModelViewMatrix();
        Matrix4f proj = MatrixCache.getProjectionMatrix();

        Vec3[] corners = new Vec3[]{
                new Vec3(aabb.minX, aabb.minY, aabb.minZ),
                new Vec3(aabb.minX, aabb.maxY, aabb.minZ),
                new Vec3(aabb.maxX, aabb.minY, aabb.minZ),
                new Vec3(aabb.maxX, aabb.maxY, aabb.minZ),
                new Vec3(aabb.minX, aabb.minY, aabb.maxZ),
                new Vec3(aabb.minX, aabb.maxY, aabb.maxZ),
                new Vec3(aabb.maxX, aabb.minY, aabb.maxZ),
                new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)
        };

        for (Vec3 corner : corners) {
            Vector4f screenPos = project(corner, view, proj, camPos);
            // w > 0 表示顶点在摄像机前方（透视除法前的原始 w 保存于此）
            if (screenPos.w() > 0 && rect.contains(screenPos.x(), screenPos.y())) {
                return true;
            }
        }

        return false;
    }

    private static Vector4f project(Vec3 worldPos, Matrix4f view, Matrix4f proj, Vec3 camPos) {
        // 1. 转换为相对摄像机的观察空间坐标
        float x = (float) (worldPos.x - camPos.x);
        float y = (float) (worldPos.y - camPos.y);
        float z = (float) (worldPos.z - camPos.z);

        Vector4f pos = new Vector4f(x, y, z, 1.0f);

        // 2. 应用视图矩阵与投影矩阵
        pos.mul(view);
        pos.mul(proj);

        // 3. 保存裁剪空间 w，用于判断顶点是否在摄像机前方
        float clipW = pos.w();

        // 4. 透视除法，变换到 NDC 空间 (-1, 1)
        if (clipW != 0) {
            pos.div(clipW);
        }

        // 5. NDC -> GUI 坐标（Minecraft GUI 原点在左上角，Y 轴向下）
        Minecraft mc = Minecraft.getInstance();
        float winW = mc.getWindow().getGuiScaledWidth();
        float winH = mc.getWindow().getGuiScaledHeight();

        pos.x = (pos.x() * 0.5f + 0.5f) * winW;
        pos.y = (1.0f - (pos.y() * 0.5f + 0.5f)) * winH;
        // 将原始裁剪空间 w 写回，供调用方用于前/后方判断
        pos.w = clipW;

        return pos;
    }
}