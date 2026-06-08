package com.i113w.camera_lib.client;

import com.i113w.camera_lib.CameraLib;
import com.i113w.camera_lib.camera.RTSCameraController;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = CameraLib.MODID, value = Dist.CLIENT)
public class CameraLibTestCommands {
    private static final float COMMAND_MOVE_SCALE = 4.0f;

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cameralibtest")
                .executes(ctx -> start(ctx.getSource(), RTSCameraController.CameraStyle.ORTHOGRAPHIC))
                .then(Commands.literal("ortho")
                        .executes(ctx -> start(ctx.getSource(), RTSCameraController.CameraStyle.ORTHOGRAPHIC)))
                .then(Commands.literal("rts")
                        .executes(ctx -> start(ctx.getSource(), RTSCameraController.CameraStyle.RTS)))
                .then(Commands.literal("free")
                        .executes(ctx -> start(ctx.getSource(), RTSCameraController.CameraStyle.FREE)))
                .then(Commands.literal("stop")
                        .executes(ctx -> stop(ctx.getSource())))
                .then(Commands.literal("move")
                        .then(Commands.literal("forward")
                                .executes(ctx -> move(ctx.getSource(), 0.0f, 1.0f, 0.0f))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> move(ctx.getSource(), 0.0f, FloatArgumentType.getFloat(ctx, "amount"), 0.0f))))
                        .then(Commands.literal("back")
                                .executes(ctx -> move(ctx.getSource(), 0.0f, -1.0f, 0.0f))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> move(ctx.getSource(), 0.0f, -FloatArgumentType.getFloat(ctx, "amount"), 0.0f))))
                        .then(Commands.literal("left")
                                .executes(ctx -> move(ctx.getSource(), 1.0f, 0.0f, 0.0f))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> move(ctx.getSource(), FloatArgumentType.getFloat(ctx, "amount"), 0.0f, 0.0f))))
                        .then(Commands.literal("right")
                                .executes(ctx -> move(ctx.getSource(), -1.0f, 0.0f, 0.0f))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> move(ctx.getSource(), -FloatArgumentType.getFloat(ctx, "amount"), 0.0f, 0.0f))))
                        .then(Commands.literal("up")
                                .executes(ctx -> move(ctx.getSource(), 0.0f, 0.0f, 1.0f))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> move(ctx.getSource(), 0.0f, 0.0f, FloatArgumentType.getFloat(ctx, "amount")))))
                        .then(Commands.literal("down")
                                .executes(ctx -> move(ctx.getSource(), 0.0f, 0.0f, -1.0f))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> move(ctx.getSource(), 0.0f, 0.0f, -FloatArgumentType.getFloat(ctx, "amount"))))))
                .then(Commands.literal("yaw")
                        .then(Commands.argument("degrees", FloatArgumentType.floatArg())
                                .executes(ctx -> yaw(ctx.getSource(), FloatArgumentType.getFloat(ctx, "degrees")))))
                .then(Commands.literal("pitch")
                        .then(Commands.argument("degrees", FloatArgumentType.floatArg())
                                .executes(ctx -> pitch(ctx.getSource(), FloatArgumentType.getFloat(ctx, "degrees")))))
                .then(Commands.literal("zoom")
                        .then(Commands.argument("delta", FloatArgumentType.floatArg())
                                .executes(ctx -> zoom(ctx.getSource(), FloatArgumentType.getFloat(ctx, "delta"))))));
    }

    private static int start(CommandSourceStack source, RTSCameraController.CameraStyle style) {
        RTSCameraController.get().enterMode(style);
        RTSCameraController.get().updateShaderFallback();
        source.sendSuccess(() -> Component.literal("CameraLib test camera started: " + RTSCameraController.get().getCameraStyle()), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        RTSCameraController.get().exitMode();
        source.sendSuccess(() -> Component.literal("CameraLib test camera stopped."), false);
        return 1;
    }

    private static int move(CommandSourceStack source, float moveX, float moveZ, float moveY) {
        RTSCameraController.get().handleInput(
                moveX * COMMAND_MOVE_SCALE,
                moveZ * COMMAND_MOVE_SCALE,
                0.0f,
                0.0f,
                moveY * COMMAND_MOVE_SCALE,
                false
        );
        source.sendSuccess(() -> Component.literal("CameraLib test camera moved."), false);
        return 1;
    }

    private static int yaw(CommandSourceStack source, float degrees) {
        RTSCameraController.get().adjustYaw(degrees);
        source.sendSuccess(() -> Component.literal("CameraLib test camera yaw adjusted by " + degrees + " degrees."), false);
        return 1;
    }

    private static int pitch(CommandSourceStack source, float degrees) {
        RTSCameraController.get().adjustPitch(degrees);
        source.sendSuccess(() -> Component.literal("CameraLib test camera pitch adjusted by " + degrees + " degrees."), false);
        return 1;
    }

    private static int zoom(CommandSourceStack source, float delta) {
        RTSCameraController.get().handleZoom(delta);
        source.sendSuccess(() -> Component.literal("CameraLib test camera zoom adjusted by " + delta + "."), false);
        return 1;
    }
}
