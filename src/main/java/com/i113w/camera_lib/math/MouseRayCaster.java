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

        Vec3 eyePos = camera.getPosition();
        Vec3 rayDir = calculateRayDirection(mouseX, mouseY);
        Vec3 endPos = eyePos.add(rayDir.scale(pickRange));

        HitResult blockHit = mc.level.clip(new ClipContext(
                eyePos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                camera.getEntity()
        ));

        double distToBlock = blockHit.getLocation().distanceToSqr(eyePos);
        Vec3 entityCheckEnd = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : endPos;
        AABB searchBox = new AABB(eyePos, endPos).inflate(1.0);

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.level,
                camera.getEntity(),
                eyePos,
                entityCheckEnd,
                searchBox,
                (e) -> !e.isSpectator() && e.isPickable()
        );

        if (entityHit != null) {
            double distToEntity = eyePos.distanceToSqr(entityHit.getLocation());
            if (distToEntity < distToBlock) {
                return entityHit;
            }
        }
        return blockHit;
    }

    private static Vec3 calculateRayDirection(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();

        float ndcX = (float) (2.0 * mouseX / windowWidth - 1.0);
        float ndcY = (float) (1.0 - 2.0 * mouseY / windowHeight);

        Matrix4f invMVP = new Matrix4f(MatrixCache.getProjectionMatrix());
        invMVP.mul(MatrixCache.getModelViewMatrix());
        invMVP.invert();

        Vector4f nearPoint = new Vector4f(ndcX, ndcY, -1.0f, 1.0f);
        Vector4f farPoint = new Vector4f(ndcX, ndcY, 1.0f, 1.0f);

        nearPoint.mul(invMVP);
        farPoint.mul(invMVP);

        if (nearPoint.w != 0) nearPoint.div(nearPoint.w);
        if (farPoint.w != 0) farPoint.div(farPoint.w);

        return new Vec3(
                farPoint.x() - nearPoint.x(),
                farPoint.y() - nearPoint.y(),
                farPoint.z() - nearPoint.z()
        ).normalize();
    }
}