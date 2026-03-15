package com.i113w.camera_lib.input;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.api.IRTSInteractionDelegate;
import com.i113w.camera_lib.api.event.RTSBoxSelectEvent;
import com.i113w.camera_lib.api.event.RTSRightClickEvent;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.i113w.camera_lib.config.CameraLibConfig;
import com.i113w.camera_lib.math.MouseRayCaster;
import com.i113w.camera_lib.math.ScreenProjector;
import com.i113w.camera_lib.selection.RTSSelectionManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class RTSInputHandler {

    private static int hoverCheckCooldown = 0;
    private static final int HOVER_CHECK_INTERVAL = 3;

    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (RTSCameraController.get().isActive()) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (RTSCameraController.get().isActive() && RTSCameraController.get().getCameraStyle() == RTSCameraController.CameraStyle.RTS) {
            event.setFOV(25.0);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (RTSCameraController.get().isActive()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (RTSCameraController.get().isActive()) {
            ResourceLocation layerName = event.getName();
            if (!VanillaGuiLayers.CHAT.equals(layerName) &&
                    !VanillaGuiLayers.DEBUG_OVERLAY.equals(layerName) &&
                    !VanillaGuiLayers.TAB_LIST.equals(layerName) &&
                    !VanillaGuiLayers.OVERLAY_MESSAGE.equals(layerName) &&
                    !VanillaGuiLayers.TITLE.equals(layerName) &&
                    !VanillaGuiLayers.SUBTITLE_OVERLAY.equals(layerName)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        RTSCameraController cameraController = RTSCameraController.get();
        if (!cameraController.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();

        cameraController.tick(mc.getTimer().getGameTimeDeltaPartialTick(false));

        float moveX = 0, moveZ = 0, moveY = 0;
        if (mc.options.keyUp.isDown()) moveZ += 1;
        if (mc.options.keyDown.isDown()) moveZ -= 1;
        if (mc.options.keyLeft.isDown()) moveX += 1;
        if (mc.options.keyRight.isDown()) moveX -= 1;
        if (mc.options.keyJump.isDown()) moveY += 1;
        if (mc.options.keyShift.isDown()) moveY -= 1;

        boolean isRotateKeyDown = CameraLibKeyMappings.CAMERA_ROTATE.isDown();
        float rotateYaw = 0;

        if (isRotateKeyDown) {
            double centerX = mc.getWindow().getScreenWidth() / 2.0;
            double deltaX = mc.mouseHandler.xpos() - centerX;
            if (cameraController.getCameraStyle() == RTSCameraController.CameraStyle.RTS) {
                if (Math.abs(deltaX) > 40.0) {
                    // 读取配置的吸附角度
                    float step = deltaX > 0 ? CameraLibConfig.rtsSnapAngle : -CameraLibConfig.rtsSnapAngle;
                    cameraController.snapYaw(step);
                    GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), centerX, mc.getWindow().getScreenHeight() / 2.0);
                }
            } else {
                if (Math.abs(deltaX) > 5.0) {
                    rotateYaw = (float) (deltaX * 0.05); // 原始鼠标灵敏度计算保留，最终旋转速度由 Controller 乘算配置得出
                    GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), centerX, mc.getWindow().getScreenHeight() / 2.0);
                }
            }
        } else {
            handleEdgePitch(mc, cameraController);
        }

        if (moveX != 0 || moveZ != 0 || moveY != 0 || rotateYaw != 0) {
            cameraController.handleInput(moveX, moveZ, rotateYaw, 0, moveY, mc.options.keySprint.isDown());
        }

        RTSSelectionManager manager = RTSSelectionManager.get();
        double guiMouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double guiMouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        if (manager.isDragging()) manager.updateDrag((float) guiMouseX, (float) guiMouseY);
        if (manager.isRightDragging()) manager.updateRightDrag((float) guiMouseX, (float) guiMouseY);

        if (++hoverCheckCooldown >= HOVER_CHECK_INTERVAL) {
            hoverCheckCooldown = 0;
            updateHoveredEntity(mc);
        }
    }

    private static void handleEdgePitch(Minecraft mc, RTSCameraController manager) {
        double y = mc.mouseHandler.ypos();
        double height = mc.getWindow().getHeight();

        // 读取配置的阈值和速度
        double threshold = CameraLibConfig.edgePanThreshold;
        float speed = CameraLibConfig.edgePanPitchSpeed;

        if (y < threshold) {
            manager.adjustPitch(-speed);
        } else if (y > height - threshold) {
            manager.adjustPitch(speed);
        }
    }

    private static void updateHoveredEntity(Minecraft mc) {
        HitResult hit = MouseRayCaster.pickFromMouse(mc.mouseHandler.xpos(), mc.mouseHandler.ypos(), 1024.0);
        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            CameraLibAPI.get().setHoveredEntityId(entityHit.getEntity().getId());
        } else {
            CameraLibAPI.get().setHoveredEntityId(-1);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (RTSCameraController.get().isActive()) {
            RTSCameraController.get().handleZoom((float) event.getScrollDeltaY());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!RTSCameraController.get().isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // 委托主模组获取光标图标
        int hoveredId = CameraLibAPI.get().getHoveredEntityId();
        Entity hoveredEntity = hoveredId != -1 && mc.level != null ? mc.level.getEntity(hoveredId) : null;

        ResourceLocation cursorTexture = CameraLibAPI.get().getDelegate().getCursorIcon(hoveredEntity, RTSSelectionManager.get().isRightDragging());

        double guiMouseX = mc.mouseHandler.xpos() * width / mc.getWindow().getScreenWidth();
        double guiMouseY = mc.mouseHandler.ypos() * height / mc.getWindow().getScreenHeight();

        RenderSystem.enableBlend();
        event.getGuiGraphics().blit(cursorTexture, (int) guiMouseX, (int) guiMouseY, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (RTSCameraController.get().isActive()) {
            if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
                RTSCameraController.get().toggleRTSMode();
                CameraLibAPI.get().clearSelection();
                Minecraft.getInstance().setScreen(null);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        if (!RTSCameraController.get().isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        RTSSelectionManager manager = RTSSelectionManager.get();
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                manager.startDrag((float) mx, (float) my);
            } else if (event.getAction() == GLFW.GLFW_RELEASE && manager.isDragging()) {
                performBoxSelection();
                manager.endDrag();
            }
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                manager.startRightDrag((float) mx, (float) my);
                event.setCanceled(true);
            } else if (event.getAction() == GLFW.GLFW_RELEASE && manager.isRightDragging()) {
                performRightClickAction();
                manager.endRightDrag();
                event.setCanceled(true);
            }
        }
    }

    private static void performBoxSelection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var rect = RTSSelectionManager.get().getSelectionRect();
        List<Entity> candidates = new ArrayList<>();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        IRTSInteractionDelegate delegate = CameraLibAPI.get().getDelegate();

        if (rect.width() < 2 && rect.height() < 2) {
            HitResult hit = MouseRayCaster.pickFromMouse(mc.mouseHandler.xpos(), mc.mouseHandler.ypos(), 1024.0);
            if (hit.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) hit).getEntity();
                if (isEntityValidForCamera(target, camPos) && delegate.isSelectable(target)) {
                    candidates.add(target);
                }
            }
        } else {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!isEntityValidForCamera(entity, camPos)) continue;
                if (ScreenProjector.isAABBInScreenRect(entity.getBoundingBox(), rect, camPos)) {
                    if (delegate.isSelectable(entity)) {
                        candidates.add(entity);
                    }
                }
            }
        }

        // 触发库事件，交给主模组处理业务逻辑
        NeoForge.EVENT_BUS.post(new RTSBoxSelectEvent(candidates));
    }

    private static void performRightClickAction() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var rect = RTSSelectionManager.get().getRightDragRect();

        if (rect.width() < 2 && rect.height() < 2) {
            HitResult hit = MouseRayCaster.pickFromMouse(mc.mouseHandler.xpos(), mc.mouseHandler.ypos(), 1024.0);
            NeoForge.EVENT_BUS.post(new RTSRightClickEvent(hit));
        } else {
            List<Entity> candidates = new ArrayList<>();
            Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == mc.player) continue;
                if (ScreenProjector.isAABBInScreenRect(entity.getBoundingBox(), rect, camPos)) {
                    candidates.add(entity);
                }
            }
            NeoForge.EVENT_BUS.post(new RTSRightClickEvent(candidates));
        }
    }

    private static boolean isEntityValidForCamera(Entity entity, Vec3 camPos) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
        if (entity == Minecraft.getInstance().player) return false;

        double verticalDist = Math.abs(entity.getY() - camPos.y);
        if (verticalDist > 250.0) return false;

        double horizontalDistSqr = entity.position().distanceToSqr(camPos.x, entity.getY(), camPos.z);
        if (horizontalDistSqr > 256.0 * 256.0) return false;

        return entity.getY() >= -64 && entity.getY() <= 320;
    }
}