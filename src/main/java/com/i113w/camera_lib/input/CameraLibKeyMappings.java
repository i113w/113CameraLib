package com.i113w.camera_lib.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class CameraLibKeyMappings {
    public static final KeyMapping CAMERA_ROTATE = new KeyMapping(
            "key.camera_lib.rotate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories.camera_lib"
    );

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(CAMERA_ROTATE);
    }
}