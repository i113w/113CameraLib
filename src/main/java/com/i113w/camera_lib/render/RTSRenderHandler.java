package com.i113w.camera_lib.render;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.i113w.camera_lib.selection.RTSSelectionManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod.EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class RTSRenderHandler {

    // ── 1. 强制压缩 FOV，产生等距视觉效果 ───────────────────────────────────

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        RTSCameraController controller = RTSCameraController.get();
        if (!controller.isActive()) return;
        if (controller.getCameraStyle() == RTSCameraController.CameraStyle.ORTHOGRAPHIC) {
            controller.updateShaderFallback();
        }
        if (controller.getCameraStyle() == RTSCameraController.CameraStyle.RTS) {
            event.setFOV(25.0);
        }
    }

    // ── 2. 隐藏第一人称手臂 ──────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (RTSCameraController.get().isActive()) {
            event.setCanceled(true);
        }
    }

    // ── 3. HUD 白名单过滤：仅保留聊天、调试、列表、标题、字幕、唱片 ──────────

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!RTSCameraController.get().isActive()) return;

        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CHAT_PANEL.id())    ||
                id.equals(VanillaGuiOverlay.DEBUG_TEXT.id())     ||
                id.equals(VanillaGuiOverlay.PLAYER_LIST.id())    ||
                id.equals(VanillaGuiOverlay.TITLE_TEXT.id())     ||
                id.equals(VanillaGuiOverlay.SUBTITLES.id())      ||
                id.equals(VanillaGuiOverlay.RECORD_OVERLAY.id())) {
            return; // 白名单内，放行
        }
        event.setCanceled(true);
    }

    // ── 4. 3D 实体高亮边框（在半透明方块渲染后插入）──────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!RTSCameraController.get().isActive()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        renderSelectedOutlines(event);
    }

    private static void renderSelectedOutlines(RenderLevelStageEvent event) {
        Set<Integer> selectedIds = CameraLibAPI.get().getSelectedEntities();
        int hoveredId = CameraLibAPI.get().getHoveredEntityId();

        if (selectedIds.isEmpty() && hoveredId == -1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 camPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        // 已选中实体 → 白色线框
        for (Integer id : selectedIds) {
            Entity entity = mc.level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                renderEntityOutline(poseStack, buffer, entity, camPos, 1f, 1f, 1f, 1f);
            }
        }

        // 悬停但未选中 → 黄色线框
        if (hoveredId != -1 && !selectedIds.contains(hoveredId)) {
            Entity entity = mc.level.getEntity(hoveredId);
            if (entity != null && entity.isAlive()) {
                renderEntityOutline(poseStack, buffer, entity, camPos, 1f, 1f, 0f, 1f);
            }
        }
    }

    private static void renderEntityOutline(PoseStack poseStack, VertexConsumer buffer,
                                            Entity entity, Vec3 camPos,
                                            float r, float g, float b, float a) {
        poseStack.pushPose();
        poseStack.translate(
                entity.getX() - camPos.x,
                entity.getY() - camPos.y,
                entity.getZ() - camPos.z
        );

        AABB localAABB = entity.getBoundingBox()
                .move(-entity.getX(), -entity.getY(), -entity.getZ())
                .inflate(0.05);

        LevelRenderer.renderLineBox(poseStack, buffer, localAABB, r, g, b, a);
        poseStack.popPose();
    }

    // ── 5. 2D GUI：选框 + 光标（保持原有逻辑不变）───────────────────────────

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!RTSCameraController.get().isActive()) return;

        RTSSelectionManager selMgr = RTSSelectionManager.get();

        // 渲染左键选择框 (绿色)
        if (selMgr.isDragging()) {
            var rect = selMgr.getSelectionRect();
            event.getGuiGraphics().fill(
                    (int) rect.x(), (int) rect.y(),
                    (int)(rect.x() + rect.width()), (int)(rect.y() + rect.height()),
                    0x8000FF00);
            event.getGuiGraphics().renderOutline(
                    (int) rect.x(), (int) rect.y(),
                    (int) rect.width(), (int) rect.height(),
                    0xFF00FF00);
        }

        // 渲染右键动作框 (红色)
        if (selMgr.isRightDragging()) {
            var rect = selMgr.getRightDragRect();
            event.getGuiGraphics().fill(
                    (int) rect.x(), (int) rect.y(),
                    (int)(rect.x() + rect.width()), (int)(rect.y() + rect.height()),
                    0x80FF0000);
            event.getGuiGraphics().renderOutline(
                    (int) rect.x(), (int) rect.y(),
                    (int) rect.width(), (int) rect.height(),
                    0xFFFF0000);
        }

        // 渲染光标
        renderCustomCursor(event);
    }

    private static void renderCustomCursor(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        int hoveredId = CameraLibAPI.get().getHoveredEntityId();
        Entity hoveredEntity = hoveredId != -1 && mc.level != null ? mc.level.getEntity(hoveredId) : null;
        ResourceLocation cursorTex = CameraLibAPI.get().getDelegate()
                .getCursorIcon(hoveredEntity, RTSSelectionManager.get().isRightDragging());
        if (cursorTex == null) return;

        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        RenderSystem.enableBlend();
        event.getGuiGraphics().blit(cursorTex, (int) mx, (int) my, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
    }
}
