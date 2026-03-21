package com.i113w.camera_lib.input;

import com.i113w.camera_lib.CameraLib;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class CameraLibKeyMappings {

    // RTS 视角旋转组合键（按住时移动鼠标旋转视角）
    public static final KeyMapping RTS_CAMERA_ROTATE = new KeyMapping(
            "key." + CameraLib.MODID + ".rts_camera_rotate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories." + CameraLib.MODID
    );

    /**
     * 注册按键映射
     * 此方法已在 CameraLib 主类的 DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...) 中
     * 通过 modEventBus.addListener 绑定到 MOD 总线上。
     */
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(RTS_CAMERA_ROTATE);
    }
}