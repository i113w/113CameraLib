package com.i113w.camera_lib.client;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class NativeCursorController {
    private static boolean hiddenByCamera;

    private NativeCursorController() {}

    public static void hideForCamera() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (window == 0L) return;
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        hiddenByCamera = true;
    }

    public static void restoreAfterCamera() {
        if (!hiddenByCamera) return;
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (window != 0L) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
        hiddenByCamera = false;
    }
}
