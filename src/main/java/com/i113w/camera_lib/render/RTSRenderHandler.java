package com.i113w.camera_lib.render;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.i113w.camera_lib.selection.RTSSelectionManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Set;

@EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class RTSRenderHandler {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (!RTSCameraController.get().isActive()) return;
        if (Minecraft.getInstance().options.hideGui) return;
        renderSelectedOutlines(event);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!RTSCameraController.get().isActive()) return;
        if (Minecraft.getInstance().options.hideGui) return;
        RTSSelectionManager manager = RTSSelectionManager.get();

        if (manager.isDragging()) {
            var rect = manager.getSelectionRect();
            event.getGuiGraphics().fill((int)rect.x(), (int)rect.y(), (int)(rect.x() + rect.width()), (int)(rect.y() + rect.height()), 0x8000FF00);
            event.getGuiGraphics().outline((int)rect.x(), (int)rect.y(), (int)rect.width(), (int)rect.height(), 0xFF00FF00);
        }
        if (manager.isRightDragging()) {
            var rect = manager.getRightDragRect();
            event.getGuiGraphics().fill((int)rect.x(), (int)rect.y(), (int)(rect.x() + rect.width()), (int)(rect.y() + rect.height()), 0x80FF0000);
            event.getGuiGraphics().outline((int)rect.x(), (int)rect.y(), (int)rect.width(), (int)rect.height(), 0xFFFF0000);
        }
    }

    private static void renderSelectedOutlines(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Set<Integer> selectedIds = CameraLibAPI.get().getSelectedEntities();
        int hoveredId = CameraLibAPI.get().getHoveredEntityId();

        if (selectedIds.isEmpty() && hoveredId == -1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderTypes.lines());

        for (Integer id : selectedIds) {
            Entity entity = mc.level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                renderEntityOutline(poseStack, buffer, entity, camPos, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }

        if (hoveredId != -1 && !selectedIds.contains(hoveredId)) {
            Entity entity = mc.level.getEntity(hoveredId);
            if (entity != null && entity.isAlive()) {
                renderEntityOutline(poseStack, buffer, entity, camPos, 1.0f, 1.0f, 0.0f, 1.0f);
            }
        }
    }

    private static void renderEntityOutline(PoseStack poseStack, VertexConsumer buffer, Entity entity, Vec3 camPos, float r, float g, float b, float a) {
        poseStack.pushPose();
        AABB localAABB = entity.getBoundingBox().move(-camPos.x, -camPos.y, -camPos.z).inflate(0.05);
        int color = ((int)(a * 255.0f) << 24)
                | ((int)(r * 255.0f) << 16)
                | ((int)(g * 255.0f) << 8)
                | (int)(b * 255.0f);
        ShapeRenderer.renderShape(poseStack, buffer, Shapes.create(localAABB), 0.0, 0.0, 0.0, color, 1.0f);
        poseStack.popPose();
    }
}
