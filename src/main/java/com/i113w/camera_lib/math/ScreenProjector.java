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

    /**
     * 判断实体的 AABB（8 个顶点中至少有 1 个）是否落在屏幕选框内。
     * View/Projection 矩阵由 {@link MatrixCache} 自动提供，无需手动传入。
     *
     * @param aabb   实体包围盒（世界坐标）
     * @param rect   屏幕选框（GUI scaled 像素坐标）
     * @param camPos 相机世界坐标（用于平移到相机空间）
     */
    public static boolean isAABBInScreenRect(AABB aabb, ScreenRect rect, Vec3 camPos) {
        if (!MatrixCache.isValid()) return false;
        Matrix4f view = MatrixCache.getModelViewMatrix();
        Matrix4f proj = MatrixCache.getProjectionMatrix();

        Vec3[] corners = {
                new Vec3(aabb.minX, aabb.minY, aabb.minZ), new Vec3(aabb.minX, aabb.maxY, aabb.minZ),
                new Vec3(aabb.maxX, aabb.minY, aabb.minZ), new Vec3(aabb.maxX, aabb.maxY, aabb.minZ),
                new Vec3(aabb.minX, aabb.minY, aabb.maxZ), new Vec3(aabb.minX, aabb.maxY, aabb.maxZ),
                new Vec3(aabb.maxX, aabb.minY, aabb.maxZ), new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)
        };

        Minecraft mc = Minecraft.getInstance();
        float gw = mc.getWindow().getGuiScaledWidth();
        float gh = mc.getWindow().getGuiScaledHeight();

        for (Vec3 corner : corners) {
            // 平移至相机空间（view 矩阵只含旋转，平移在此处手动处理）
            Vec3 rel = corner.subtract(camPos);
            Vector4f v = new Vector4f(
                    (float) rel.x, (float) rel.y, (float) rel.z, 1.0f);

            // View 变换（旋转）→ Projection 变换
            v.mul(view).mul(proj);

            // w ≤ 0 表示在相机后方，跳过
            if (v.w() <= 0f) continue;

            // 透视除法 → NDC → GUI 坐标（scaled pixels）
            v.div(v.w());
            float sx = (v.x() * 0.5f + 0.5f) * gw;
            float sy = (1.0f - (v.y() * 0.5f + 0.5f)) * gh;

            if (rect.contains(sx, sy)) {
                return true;   // 任意顶点在框内即算命中
            }
        }

        return false;
    }
}