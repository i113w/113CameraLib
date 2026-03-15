package com.i113w.camera_lib.render;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.i113w.camera_lib.selection.RTSSelectionManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Set;

@EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class RTSRenderHandler {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!RTSCameraController.get().isActive()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            renderSelectedOutlines(event);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!RTSCameraController.get().isActive()) return;
        RTSSelectionManager manager = RTSSelectionManager.get();

        if (manager.isDragging()) {
            var rect = manager.getSelectionRect();
            event.getGuiGraphics().fill((int)rect.x(), (int)rect.y(), (int)(rect.x() + rect.width()), (int)(rect.y() + rect.height()), 0x8000FF00);
            event.getGuiGraphics().renderOutline((int)rect.x(), (int)rect.y(), (int)rect.width(), (int)rect.height(), 0xFF00FF00);
        }
        if (manager.isRightDragging()) {
            var rect = manager.getRightDragRect();
            event.getGuiGraphics().fill((int)rect.x(), (int)rect.y(), (int)(rect.x() + rect.width()), (int)(rect.y() + rect.height()), 0x80FF0000);
            event.getGuiGraphics().renderOutline((int)rect.x(), (int)rect.y(), (int)rect.width(), (int)rect.height(), 0xFFFF0000);
        }
    }

    private static void renderSelectedOutlines(RenderLevelStageEvent event) {
        Set<Integer> selectedIds = CameraLibAPI.get().getSelectedEntities();
        int hoveredId = CameraLibAPI.get().getHoveredEntityId();

        if (selectedIds.isEmpty() && hoveredId == -1) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

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
        poseStack.translate(entity.getX() - camPos.x, entity.getY() - camPos.y, entity.getZ() - camPos.z);
        AABB localAABB = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ()).inflate(0.05);
        LevelRenderer.renderLineBox(poseStack, buffer, localAABB, r, g, b, a);
        poseStack.popPose();
    }
}