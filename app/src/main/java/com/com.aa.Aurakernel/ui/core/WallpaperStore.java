package com.aa.Aurakernel.ui.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 壁纸存储：内置 3 张程序化渐变壁纸 + 相册选择（ACTION_OPEN_DOCUMENT，免权限）。
 * - 相册图：降采样 → 高斯模糊（stack blur）→ 缓存到 cacheDir → 供 LiquidBackgroundView 使用。
 * - 选择失败自动回退内置壁纸（调用方 Toast）。
 */
public final class WallpaperStore {

  public interface Callback {
    void onReady(Bitmap bitmap);

    void onError(String message);
  }

  private static final String PREFS = "wallpaper";
  private static final String KEY_TYPE = "type"; // builtin_0/1/2 | custom
  private static final String CUSTOM_FILE = "wallpaper_custom.jpg";
  private static final int TARGET_SIZE = 1080;
  private static final int BLUR_RADIUS = 24;

  private WallpaperStore() {}

  // ==================== 内置壁纸（程序化渐变，无资源依赖） ====================

  /** 生成内置渐变壁纸：0=绿意 1=青蓝 2=紫粉。 */
  public static Bitmap builtin(Context ctx, int index) {
    int w = 720, h = 1280;
    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bmp);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    int[] colors;
    switch (index % 3) {
      case 1:
        colors = new int[] {0xFF062E44, 0xFF0B7FA3, 0xFF3FB9D8, 0xFFB8F0E8};
        break;
      case 2:
        colors = new int[] {0xFF241040, 0xFF7B2FB8, 0xFFC05AE0, 0xFFF2B8F0};
        break;
      default:
        colors = new int[] {0xFF0C3A24, 0xFF1E8A4F, 0xFF4FC97E, 0xFFC8F5D8};
    }
    p.setShader(new LinearGradient(0, 0, w, h, colors, null, Shader.TileMode.CLAMP));
    c.drawRect(0, 0, w, h, p);
    p.setShader(null);

    // 柔和光斑
    drawGlow(c, p, w * 0.25f, h * 0.28f, w * 0.5f, 0x33FFFFFF);
    drawGlow(c, p, w * 0.78f, h * 0.62f, w * 0.42f, 0x26FFFFFF);
    drawGlow(c, p, w * 0.55f, h * 0.88f, w * 0.35f, 0x1AFFFFFF);
    return bmp;
  }

  private static void drawGlow(Canvas c, Paint p, float x, float y, float r, int color) {
    p.setShader(new android.graphics.RadialGradient(
        x, y, r, new int[] {color, Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
    c.drawCircle(x, y, r, p);
    p.setShader(null);
  }

  // ==================== 相册选择（免权限） ====================

  /** 弹出系统相册选择（ACTION_OPEN_DOCUMENT，无需任何存储权限）。 */
  public static void pickFromGallery(Activity activity, Callback callback) {
    try {
      Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType("image/*");
      activity.startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
      pendingCallback = callback;
      pendingActivity = activity;
    } catch (Exception e) {
      callback.onError("无法打开相册");
    }
  }

  public static final int REQUEST_PICK_WALLPAPER = 0x5A17;
  private static Callback pendingCallback;
  private static Activity pendingActivity;

  /** 在 Activity.onActivityResult 中转发。 */
  public static boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    if (requestCode != REQUEST_PICK_WALLPAPER) return false;
    Callback cb = pendingCallback;
    Activity act = pendingActivity;
    pendingCallback = null;
    pendingActivity = null;
    if (cb == null || act != activity) return true; // 不匹配的返回也吞掉，避免误处理
    if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
      cb.onError("未选择图片");
      return true;
    }
    Uri uri = data.getData();
    try {
      Bitmap processed = processCustom(activity, uri);
      if (processed == null) {
        cb.onError("图片加载失败");
        return true;
      }
      saveCustom(activity, processed);
      cb.onReady(processed);
    } catch (Exception e) {
      cb.onError("图片处理失败");
    }
    return true;
  }

  /** 解码 + 降采样 + 高斯模糊。 */
  public static Bitmap processCustom(Context ctx, Uri uri) throws Exception {
    BitmapFactory.Options bounds = new BitmapFactory.Options();
    bounds.inJustDecodeBounds = true;
    BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bounds);
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

    int sample = 1;
    while (Math.max(bounds.outWidth, bounds.outHeight) / sample > TARGET_SIZE) sample *= 2;

    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inSampleSize = sample;
    Bitmap decoded = BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, opts);
    if (decoded == null) return null;

    Bitmap blurred = stackBlur(decoded, BLUR_RADIUS);
    if (blurred != decoded) decoded.recycle();
    return blurred;
  }

  /** stack blur：3 次 (水平+垂直) box blur 近似高斯，纯 Java 实现。 */
  public static Bitmap stackBlur(Bitmap src, int radius) {
    if (radius <= 0) return src;
    Bitmap current = src;
    for (int i = 0; i < 3; i++) {
      Bitmap blurred =
          Bitmap.createBitmap(current.getWidth(), current.getHeight(), Bitmap.Config.ARGB_8888);
      boxBlur(current, blurred, radius);
      if (current != src) current.recycle();
      current = blurred;
    }
    return current;
  }

  /** 水平 + 垂直两次滑窗 box blur（src → dst）。 */
  private static void boxBlur(Bitmap src, Bitmap dst, int radius) {
    int w = src.getWidth(), h = src.getHeight();
    int[] pixels = new int[w * h];
    src.getPixels(pixels, 0, w, 0, 0, w, h);
    int[] tmp = new int[w * h];

    // 水平方向：pixels → tmp
    for (int y = 0; y < h; y++) {
      int rowStart = y * w;
      long r = 0, g = 0, b = 0;
      int count = 0;
      for (int x = -radius; x <= radius; x++) {
        int px = pixels[rowStart + Math.max(0, Math.min(w - 1, x))];
        r += (px >> 16) & 0xFF;
        g += (px >> 8) & 0xFF;
        b += px & 0xFF;
        count++;
      }
      for (int x = 0; x < w; x++) {
        tmp[rowStart + x] =
            (0xFF << 24)
                | (((int) (r / count)) << 16)
                | (((int) (g / count)) << 8)
                | (int) (b / count);
        int addX = Math.min(w - 1, x + radius + 1);
        int remX = Math.max(0, x - radius);
        int add = pixels[rowStart + addX];
        int rem = pixels[rowStart + remX];
        r += ((add >> 16) & 0xFF) - ((rem >> 16) & 0xFF);
        g += ((add >> 8) & 0xFF) - ((rem >> 8) & 0xFF);
        b += (add & 0xFF) - (rem & 0xFF);
      }
    }

    // 垂直方向：tmp → pixels
    for (int x = 0; x < w; x++) {
      long r = 0, g = 0, b = 0;
      int count = 0;
      for (int y = -radius; y <= radius; y++) {
        int py = tmp[Math.max(0, Math.min(h - 1, y)) * w + x];
        r += (py >> 16) & 0xFF;
        g += (py >> 8) & 0xFF;
        b += py & 0xFF;
        count++;
      }
      for (int y = 0; y < h; y++) {
        int idx = y * w + x;
        pixels[idx] =
            (0xFF << 24)
                | (((int) (r / count)) << 16)
                | (((int) (g / count)) << 8)
                | (int) (b / count);
        int addY = Math.min(h - 1, y + radius + 1);
        int remY = Math.max(0, y - radius);
        int add = tmp[addY * w + x];
        int rem = tmp[remY * w + x];
        r += ((add >> 16) & 0xFF) - ((rem >> 16) & 0xFF);
        g += ((add >> 8) & 0xFF) - ((rem >> 8) & 0xFF);
        b += (add & 0xFF) - (rem & 0xFF);
      }
    }
    dst.setPixels(pixels, 0, w, 0, 0, w, h);
  }

  // ==================== 持久化 ====================

  public static void saveCustom(Context ctx, Bitmap bmp) {
    try {
      File file = new File(ctx.getCacheDir(), CUSTOM_FILE);
      FileOutputStream fos = new FileOutputStream(file);
      bmp.compress(Bitmap.CompressFormat.JPEG, 88, fos);
      fos.close();
      prefs(ctx).edit().putString(KEY_TYPE, "custom").apply();
    } catch (Exception ignored) {
    }
  }

  public static void setBuiltin(Context ctx, int index) {
    prefs(ctx).edit().putString(KEY_TYPE, "builtin_" + index).apply();
  }

  public static void clearCustom(Context ctx) {
    prefs(ctx).edit().putString(KEY_TYPE, "builtin_0").apply();
    File f = new File(ctx.getCacheDir(), CUSTOM_FILE);
    if (f.exists()) f.delete();
  }

  /** 加载当前壁纸（内置或自定义缓存），失败回退内置 0。 */
  public static Bitmap loadCurrent(Context ctx) {
    String type = prefs(ctx).getString(KEY_TYPE, "builtin_0");
    try {
      if (type != null && type.startsWith("builtin_")) {
        int idx = Integer.parseInt(type.substring("builtin_".length()));
        return builtin(ctx, idx);
      }
      File f = new File(ctx.getCacheDir(), CUSTOM_FILE);
      if (f.exists()) {
        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
        if (bmp != null) return bmp;
      }
    } catch (Exception ignored) {
    }
    return builtin(ctx, 0);
  }

  public static String getType(Context ctx) {
    return prefs(ctx).getString(KEY_TYPE, "builtin_0");
  }

  private static SharedPreferences prefs(Context ctx) {
    return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }
}
