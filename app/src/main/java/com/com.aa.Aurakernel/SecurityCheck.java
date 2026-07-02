package com.aa.ABC;

import android.content.Context;
import android.os.Debug;
import java.io.File;

public class SecurityCheck {

    // 检测调试器连接
    public static boolean isDebuggerConnected() {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger();
    }

    // 检测Xposed/Frida等Hook框架
    public static boolean hasHookFramework() {
        String[] suspiciousClasses = {
            "de.robv.android.xposed.XposedHelpers",
            "de.robv.android.xposed.XposedBridge",
            "com.saurik.substrate.MS",
            "org.joor.Reflect"
        };
        for (String cls : suspiciousClasses) {
            try {
                Class.forName(cls);
                return true;
            } catch (ClassNotFoundException ignored) {}
        }
        // 检测Frida常用端口
        try {
            new java.net.Socket("127.0.0.1", 27042).close();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    // 检测虚拟环境/多开分身
    public static boolean isVirtualEnvironment(Context context) {
        String[] suspiciousPaths = {
            "/data/app/--virtual",
            "/data/data/com.lbe.parallel",
            "/data/data/com.blydn.run",
            "/data/data/com.excelliance.multi"
        };
        for (String path : suspiciousPaths) {
            if (new File(path).exists()) return true;
        }
        // 检测是否运行在私有目录外的路径
        String pkgPath = context.getPackageCodePath();
        return !pkgPath.contains(context.getPackageName());
    }

    // 综合安全检查
    public static boolean isEnvSafe(Context context) {
        return !isDebuggerConnected() && !hasHookFramework() && !isVirtualEnvironment(context);
    }
}