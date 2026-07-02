package com.aa.ABC;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.util.Base64;

public class SignatureGuard {

  // ============================================================
  // ⚠️ IMPORTANT: 这个数组先用空值占位
  //    首次编译安装后，运行 adb logcat -s SIG_GUARD 获取实际哈希
  //    然后把实际哈希填入下方数组（用 initExpectedHash 方法生成）
  // ============================================================
  private static byte[] EXPECTED_SIG_HASH = null;

  // 初始化期望的签名哈希
  // 用 `generateExpectedHash(yourBase64Hash)` 来设置
  public static void initExpectedHash(String base64Hash) {
    if (base64Hash != null && !base64Hash.isEmpty()) {
      EXPECTED_SIG_HASH = Base64.getDecoder().decode(base64Hash);
    }
  }

  // 🔐 工具方法：把 Base64 字符串转成字节数组（用于设置 EXPECTED_SIG_HASH）
  public static byte[] fromBase64(String base64) {
    return Base64.getDecoder().decode(base64);
  }

  // 获取当前 APK 的签名 SHA-256 指纹（字节数组形式）
  public static byte[] getApkSignatureHash(Context context) {
    try {
      PackageManager pm = context.getPackageManager();
      String pkgName = context.getPackageName();

      // Android 9+ 推荐用 getSigningCertificateHistory
      if (android.os.Build.VERSION.SDK_INT >= 28) {
        PackageInfo pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES);
        if (pkgInfo.signingInfo != null) {
          android.content.pm.SigningInfo si = pkgInfo.signingInfo;
          Signature[] sigs = si.getApkContentsSigners();
          if (sigs != null && sigs.length > 0) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(sigs[0].toByteArray());
          }
        }
      } else {
        // Android 8.x 及以下用旧方法
        @SuppressWarnings("deprecation")
        PackageInfo pkgInfo = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES);
        Signature[] sigs = pkgInfo.signatures;
        if (sigs != null && sigs.length > 0) {
          MessageDigest md = MessageDigest.getInstance("SHA-256");
          return md.digest(sigs[0].toByteArray());
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  // 获取签名哈希的 Base64 字符串形式（方便打印查看）
  public static String getApkSignatureHashBase64(Context context) {
    byte[] hash = getApkSignatureHash(context);
    if (hash == null) return null;
    return Base64.getEncoder().encodeToString(hash);
  }

  // === 核心校验方法 ===

  /**
   * 校验 APK 签名是否与期望值一致
   *
   * @param context 上下文
   * @return true=签名有效 | false=签名被篡改
   */
  // === 核心校验方法 ===
  public static boolean isSignatureValid(Context context) {
    // ✅ 从 StringGuard 解密获取期望的签名哈希（索引6）
    String expectedBase64 = StringGuard.get(6);
    if (expectedBase64 == null || expectedBase64.isEmpty()) {
      // 开发阶段：没有设置哈希则放行
      android.util.Log.w("SIG_GUARD", "⚠️ 签名哈希未配置，跳过校验");
      return true;
    }

    byte[] expectedHash;
    try {
      expectedHash = Base64.getDecoder().decode(expectedBase64);
    } catch (Exception e) {
      android.util.Log.e("SIG_GUARD", "❌ 无法解码期望签名哈希");
      return false;
    }

    byte[] currentHash = getApkSignatureHash(context);
    if (currentHash == null) {
      android.util.Log.e("SIG_GUARD", "❌ 无法获取当前 APK 签名");
      return false;
    }

    // 长度检查
    if (currentHash.length != expectedHash.length) {
      android.util.Log.e("SIG_GUARD", "❌ 签名哈希长度不匹配");
      return false;
    }

    // 常量时间比较（防时序攻击）
    boolean match = true;
    for (int i = 0; i < currentHash.length; i++) {
      if (currentHash[i] != expectedHash[i]) {
        match = false;
      }
    }

    return match;
  }

  /**
   * 生成期望的签名哈希（在开发机上运行一次，把输出粘贴到代码里） 使用方式： 1. 编译并安装 APK 到手机 2. 在 onCreate 里临时加一行 Log.d 打印 3. 把打印出来的
   * Base64 字符串复制到代码中
   */
  public static String generateExpectedHash(Context context) {
    return getApkSignatureHashBase64(context);
  }
}
