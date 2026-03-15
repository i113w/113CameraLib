package com.i113w.camera_lib.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CameraLibConfig {
    public static final ModConfigSpec SPEC;

    // --- 高性能静态缓存字段 ---
    public static float rtsPitchMin;
    public static float rtsPitchMax;
    public static float freePitchMin;
    public static float freePitchMax;
    public static float rtsZoomMin;
    public static float rtsZoomMax;
    public static float rtsZoomSpeed;
    public static double edgePanThreshold;
    public static float edgePanPitchSpeed;
    public static float rtsSnapAngle;
    public static float freeRotationSpeed;
    public static float moveBaseSpeed;
    public static float moveSprintMultiplier;

    // --- Spec 定义 ---
    private static final ModConfigSpec.DoubleValue RTS_PITCH_MIN;
    private static final ModConfigSpec.DoubleValue RTS_PITCH_MAX;
    private static final ModConfigSpec.DoubleValue FREE_PITCH_MIN;
    private static final ModConfigSpec.DoubleValue FREE_PITCH_MAX;
    private static final ModConfigSpec.DoubleValue RTS_ZOOM_MIN;
    private static final ModConfigSpec.DoubleValue RTS_ZOOM_MAX;
    private static final ModConfigSpec.DoubleValue RTS_ZOOM_SPEED;
    private static final ModConfigSpec.DoubleValue EDGE_PAN_THRESHOLD;
    private static final ModConfigSpec.DoubleValue EDGE_PAN_PITCH_SPEED;
    private static final ModConfigSpec.DoubleValue RTS_SNAP_ANGLE;
    private static final ModConfigSpec.DoubleValue FREE_ROTATION_SPEED;
    private static final ModConfigSpec.DoubleValue MOVE_BASE_SPEED;
    private static final ModConfigSpec.DoubleValue MOVE_SPRINT_MULT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("113's Camera Lib - Client Configuration").push("camera_settings");

        builder.push("rts_mode");
        RTS_PITCH_MIN = builder.defineInRange("rtsPitchMin", 35.0, -90.0, 90.0);
        RTS_PITCH_MAX = builder.defineInRange("rtsPitchMax", 45.0, -90.0, 90.0);
        RTS_ZOOM_MIN = builder.defineInRange("rtsZoomMin", 10.0, 1.0, 200.0);
        RTS_ZOOM_MAX = builder.defineInRange("rtsZoomMax", 80.0, 1.0, 200.0);
        RTS_ZOOM_SPEED = builder.defineInRange("rtsZoomSpeedMultiplier", 3.5, 0.1, 20.0);
        RTS_SNAP_ANGLE = builder.defineInRange("rtsSnapAngle", 90.0, 0.0, 360.0);
        builder.pop();

        builder.push("free_mode");
        FREE_PITCH_MIN = builder.defineInRange("freePitchMin", 10.0, -90.0, 90.0);
        FREE_PITCH_MAX = builder.defineInRange("freePitchMax", 90.0, -90.0, 90.0);
        FREE_ROTATION_SPEED = builder.defineInRange("freeRotationSpeed", 5.0, 0.1, 50.0);
        builder.pop();

        builder.push("edge_panning");
        EDGE_PAN_THRESHOLD = builder.comment("Distance in pixels from the screen edge").defineInRange("thresholdPx", 20.0, 0.0, 200.0);
        EDGE_PAN_PITCH_SPEED = builder.defineInRange("pitchAdjustSpeed", 2.0, 0.0, 20.0);
        builder.pop();

        builder.push("movement");
        MOVE_BASE_SPEED = builder.defineInRange("baseSpeed", 1.0, 0.01, 50.0);
        MOVE_SPRINT_MULT = builder.defineInRange("sprintMultiplier", 2.0, 1.0, 20.0);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    /**
     * 将 Config 值烘焙到静态变量中
     */
    public static void bake() {
        rtsPitchMin = RTS_PITCH_MIN.get().floatValue();
        rtsPitchMax = RTS_PITCH_MAX.get().floatValue();
        freePitchMin = FREE_PITCH_MIN.get().floatValue();
        freePitchMax = FREE_PITCH_MAX.get().floatValue();
        rtsZoomMin = RTS_ZOOM_MIN.get().floatValue();
        rtsZoomMax = RTS_ZOOM_MAX.get().floatValue();
        rtsZoomSpeed = RTS_ZOOM_SPEED.get().floatValue();
        edgePanThreshold = EDGE_PAN_THRESHOLD.get();
        edgePanPitchSpeed = EDGE_PAN_PITCH_SPEED.get().floatValue();
        rtsSnapAngle = RTS_SNAP_ANGLE.get().floatValue();
        freeRotationSpeed = FREE_ROTATION_SPEED.get().floatValue();
        moveBaseSpeed = MOVE_BASE_SPEED.get().floatValue();
        moveSprintMultiplier = MOVE_SPRINT_MULT.get().floatValue();
    }

    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }

    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) bake();
    }
}