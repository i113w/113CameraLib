package com.i113w.camera_lib.camera;

import java.lang.reflect.Method;

public final class ShaderPackDetector {
    private static boolean resolved;
    private static Method getInstanceMethod;
    private static Method isShaderPackInUseMethod;

    private ShaderPackDetector() {}

    public static boolean isShaderPackActive() {
        resolveIrisApi();
        if (getInstanceMethod == null || isShaderPackInUseMethod == null) return false;

        try {
            Object api = getInstanceMethod.invoke(null);
            return Boolean.TRUE.equals(isShaderPackInUseMethod.invoke(api));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static void resolveIrisApi() {
        if (resolved) return;
        resolved = true;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            getInstanceMethod = apiClass.getMethod("getInstance");
            isShaderPackInUseMethod = apiClass.getMethod("isShaderPackInUse");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            getInstanceMethod = null;
            isShaderPackInUseMethod = null;
        }
    }
}
