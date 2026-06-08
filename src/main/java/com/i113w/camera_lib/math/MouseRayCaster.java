package com.i113w.camera_lib.math;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class MouseRayCaster {

    public static HitResult pickFromMouse(double mouseX, double mouseY, double pickRange) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();

        if (camera == null || mc.level == null || !MatrixCache.isValid()) {
            return BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
        }

        int winW = mc.getWindow().getWidth();
        int winH = mc.getWindow().getHeight();

        float ndcX = (float)(2.0 * mouseX / winW  - 1.0);
        float ndcY = (float)(1.0 - 2.0 * mouseY / winH);

        Matrix4f view = MatrixCache.getModelViewMatrix();
        Matrix4f proj = MatrixCache.getProjectionMatrix();

        Vec3 eyePos = camera.getPosition();
        Vec3 rayDir = unprojectRay(ndcX, ndcY, view, proj);
        Vec3 endPos = eyePos.add(rayDir.scale(pickRange));

        HitResult blockHit = mc.level.clip(new ClipContext(
                eyePos, endPos,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                camera.getEntity()
        ));

        double distBlock = blockHit.getLocation().distanceToSqr(eyePos);

        Vec3 entityEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : endPos;
        AABB box = new AABB(eyePos, entityEnd).inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.level, camera.getEntity(), eyePos, entityEnd, box,
                e -> !e.isSpectator() && e.isPickable()
        );

        if (entityHit != null && eyePos.distanceToSqr(entityHit.getLocation()) < distBlock) {
            return entityHit;
        }

        return blockHit;
    }

    private static Vec3 unprojectRay(float ndcX, float ndcY, Matrix4f view, Matrix4f proj) {
        Matrix4f mvp = new Matrix4f(proj).mul(view);
        Matrix4f invMVP = mvp.invert(new Matrix4f());

        Vector4f near4 = new Vector4f(ndcX, ndcY, -1f, 1f).mul(invMVP);
        Vector4f far4  = new Vector4f(ndcX, ndcY,  1f, 1f).mul(invMVP);

        if (near4.w() != 0f) near4.div(near4.w());
        if (far4.w()  != 0f) far4.div(far4.w());

        return new Vec3(
                far4.x() - near4.x(),
                far4.y() - near4.y(),
                far4.z() - near4.z()
        ).normalize();
    }
}