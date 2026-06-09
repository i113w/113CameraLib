package com.i113w.camera_lib.input;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.api.CameraLibAPI;
import com.i113w.camera_lib.api.IRTSInteractionDelegate;
import com.i113w.camera_lib.api.event.RTSBoxSelectEvent;
import com.i113w.camera_lib.api.event.RTSRightClickEvent;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.i113w.camera_lib.client.NativeCursorController;
import com.i113w.camera_lib.config.CameraLibConfig;
import com.i113w.camera_lib.math.MouseRayCaster;
import com.i113w.camera_lib.math.ScreenProjector;
import com.i113w.camera_lib.selection.RTSSelectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class RTSInputHandler {

    // 禁用原版移动
    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (RTSCameraController.get().isActive()) {
            event.getInput().forwardImpulse = 0;
            event.getInput().leftImpulse = 0;
            event.getInput().jumping = false;
            event.getInput().shiftKeyDown = false;
        }
    }

    // 鼠标滚轮缩放
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (RTSCameraController.get().isActive()) {
            RTSCameraController.get().handleZoom((float) event.getScrollDelta());
            event.setCanceled(true);
        }
    }

    // ── 鼠标按键：框选 / 右键动作 ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        if (!RTSCameraController.get().isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        RTSSelectionManager selMgr = RTSSelectionManager.get();
        double mx = getScaledMouseX(mc);
        double my = getScaledMouseY(mc);

        // 左键框选
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                selMgr.startDrag((float) mx, (float) my);
            } else if (event.getAction() == GLFW.GLFW_RELEASE && selMgr.isDragging()) {
                performLeftSelection();
                selMgr.endDrag();
            }
            event.setCanceled(true);
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (event.getAction() == GLFW.GLFW_PRESS) {
                selMgr.startRightDrag((float) mx, (float) my);
            } else if (event.getAction() == GLFW.GLFW_RELEASE && selMgr.isRightDragging()) {
                performRightAction();
                selMgr.endRightDrag();
            }
            event.setCanceled(true);
        }
    }

    // ── 键盘按键：ESC 退出 RTS ────────────────────────────────────────────────

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!RTSCameraController.get().isActive()) return;
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            RTSCameraController.get().toggleRTSMode();
            CameraLibAPI.get().clearSelection();
            Minecraft.getInstance().setScreen(null);
        }
    }

    // ── 每 Tick 主循环 ────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        RTSCameraController cameraMgr = RTSCameraController.get();
        if (!cameraMgr.isActive()) return;

        Minecraft mc = Minecraft.getInstance();

        // 释放鼠标抓取，显示系统光标（库自定义光标由 RTSRenderHandler 绘制）
        if (mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }
        NativeCursorController.hideForCamera();

        // 相机插值更新
        cameraMgr.tick(mc.getFrameTime());

        // 拖拽矩形实时更新
        RTSSelectionManager selMgr = RTSSelectionManager.get();
        double mx = getScaledMouseX(mc);
        double my = getScaledMouseY(mc);
        if (selMgr.isDragging())  selMgr.updateDrag((float) mx, (float) my);
        if (selMgr.isRightDragging()) selMgr.updateRightDrag((float) mx, (float) my);

        // 悬停实体检测（每 3 tick 一次）
        if (mc.level != null && mc.level.getGameTime() % 3 == 0) {
            HitResult hit = MouseRayCaster.pickFromMouse(
                    mc.mouseHandler.xpos(), mc.mouseHandler.ypos(), 1024.0);
            if (hit.getType() == HitResult.Type.ENTITY) {
                CameraLibAPI.get().setHoveredEntityId(((EntityHitResult) hit).getEntity().getId());
            } else {
                CameraLibAPI.get().setHoveredEntityId(-1);
            }
        }

        // ── WASD 移动 + Ctrl 旋转 + 边缘俯仰 ────────────────────────────────

        float moveX = 0, moveZ = 0, moveY = 0;
        if (mc.options.keyUp.isDown())    moveZ += 1;
        if (mc.options.keyDown.isDown())  moveZ -= 1;
        if (mc.options.keyLeft.isDown())  moveX += 1;
        if (mc.options.keyRight.isDown()) moveX -= 1;
        if (mc.options.keyJump.isDown())  moveY += 1;
        if (mc.options.keyShift.isDown()) moveY -= 1;

        float rotateYaw = 0;
        boolean isRotateKeyDown = CameraLibKeyMappings.CAMERA_ROTATE.isDown();

        if (isRotateKeyDown) {
            double centerX = mc.getWindow().getScreenWidth() / 2.0;
            double deltaX  = mc.mouseHandler.xpos() - centerX;

            if (cameraMgr.isGroundFocusedStyle()) {
                // RTS 风格：鼠标偏移 > 40px 时触发 90° 阶跃旋转
                if (Math.abs(deltaX) > 40.0) {
                    float step = deltaX > 0 ? CameraLibConfig.rtsSnapAngle : -CameraLibConfig.rtsSnapAngle;
                    cameraMgr.snapYaw(step);
                    GLFW.glfwSetCursorPos(
                            mc.getWindow().getWindow(),
                            centerX,
                            mc.getWindow().getScreenHeight() / 2.0);
                }
            } else {
                // Free 风格：连续旋转
                if (Math.abs(deltaX) > 5.0) {
                    rotateYaw = (float)(deltaX * 0.05);
                    GLFW.glfwSetCursorPos(
                            mc.getWindow().getWindow(),
                            centerX,
                            mc.getWindow().getScreenHeight() / 2.0);
                }
            }
        } else {
            // 旋转键未按下时，处理屏幕边缘俯仰调整
            handleEdgePitch(mc, cameraMgr);
        }

        if (moveX != 0 || moveZ != 0 || moveY != 0 || rotateYaw != 0) {
            cameraMgr.handleInput(moveX, moveZ, rotateYaw, 0, moveY, mc.options.keySprint.isDown());
        }
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────────────────

    /** 鼠标靠近屏幕上下边缘时自动调整俯仰角 */
    private static void handleEdgePitch(Minecraft mc, RTSCameraController manager) {
        double mouseY = mc.mouseHandler.ypos();
        double height = mc.getWindow().getHeight();
        if (mouseY < CameraLibConfig.edgePanThreshold) {
            manager.adjustPitch(-CameraLibConfig.edgePanPitchSpeed);
        } else if (mouseY > height - CameraLibConfig.edgePanThreshold) {
            manager.adjustPitch(CameraLibConfig.edgePanPitchSpeed);
        }
    }

    private static void performLeftSelection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var rect = RTSSelectionManager.get().getSelectionRect();
        List<Entity> results = new ArrayList<>();
        IRTSInteractionDelegate delegate = CameraLibAPI.get().getDelegate();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 selectionCenter = RTSCameraController.get().isGroundFocusedStyle()
                ? RTSCameraController.get().getFocusPosition()
                : camPos;

        // 判断是点选还是框选
        if (rect.width() < 2 && rect.height() < 2) {
            // 点选
            HitResult hit = MouseRayCaster.pickFromMouse(
                    mc.mouseHandler.xpos(), mc.mouseHandler.ypos(), 1024.0);
            if (hit.getType() == HitResult.Type.ENTITY) {
                Entity e = ((EntityHitResult) hit).getEntity();
                if (isEntityValidForCamera(e, selectionCenter) && delegate.isSelectable(e)) {
                    results.add(e);
                }
            }
        } else {
            // 框选
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!isEntityValidForCamera(entity, selectionCenter)) continue;
                if (delegate.isSelectable(entity)
                        && ScreenProjector.isAABBInScreenRect(entity.getBoundingBox(), rect, camPos)) {
                    results.add(entity);
                }
            }
        }

        // 抛出事件给主模组处理
        MinecraftForge.EVENT_BUS.post(new RTSBoxSelectEvent(results));
    }

    private static void performRightAction() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        var rect = RTSSelectionManager.get().getRightDragRect();

        if (rect.width() < 2 && rect.height() < 2) {
            HitResult hit = MouseRayCaster.pickFromMouse(
                    mc.mouseHandler.xpos(), mc.mouseHandler.ypos(), 1024.0);
            MinecraftForge.EVENT_BUS.post(new RTSRightClickEvent(hit));
        } else {
            List<Entity> candidates = new ArrayList<>();
            Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof LivingEntity) || !entity.isAlive() || entity == mc.player) continue;
                if (ScreenProjector.isAABBInScreenRect(entity.getBoundingBox(), rect, camPos)) {
                    candidates.add(entity);
                }
            }
            MinecraftForge.EVENT_BUS.post(new RTSRightClickEvent(candidates));
        }
    }

    private static boolean isEntityValidForCamera(Entity entity, Vec3 focusPos) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return false;
        if (entity == Minecraft.getInstance().player) return false;

        double verticalDist = Math.abs(entity.getY() - focusPos.y);
        if (verticalDist > 250.0) return false;

        double horizontalDistSqr = entity.position().distanceToSqr(focusPos.x, entity.getY(), focusPos.z);
        if (horizontalDistSqr > 256.0 * 256.0) return false;

        return entity.getY() >= -64 && entity.getY() <= 320;
    }

    private static double getScaledMouseX(Minecraft mc) {
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
    }

    private static double getScaledMouseY(Minecraft mc) {
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
    }
}
