package com.aa.Aurakernel.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.view.Choreographer;
import android.view.View;

import com.aa.Aurakernel.ui.core.FluidPhysics;
import com.aa.Aurakernel.ui.core.ThemeManager;

import java.util.Random;

/**
 * 全屏流体背景：3 层正弦波浪（无缝流动）+ 光斑漂移 + 气泡上浮 + 可选壁纸层。
 * - 主题切换：颜色 600ms 插值过渡（onThemeChanged 驱动）。
 * - 帧率自适应：失焦暂停；连续掉帧自动降波浪层数 3→2；恢复后回升。
 * - 需挂在页面容器最底层，页面背景透明后波浪透出。
 */
public class LiquidBackgroundView extends View implements ThemeManager.Listener {

  // 波浪参数（spec §2）：振幅(dp) / 频率(rad/px) / 相位速度(rad/s) / 基线高度比例
  private static final float[][] WAVE_DEFS = {
      {26f, 0.0085f, 1.20f, 0.52f},
      {17f, 0.0140f, 1.90f, 0.68f},
      {8f,  0.0220f, 2.70f, 0.86f},
  };
  private static final int MAX_LAYERS = WAVE_DEFS.length;

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path path = new Path();
  private final Random random = new Random();

  private final Choreographer choreographer = Choreographer.getInstance();
  private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
    @Override
    public void doFrame(long frameTimeNanos) {
      if (!running) return;
      long now = System.nanoTime();
      float dt = (now - lastFrameNanos) / 1e9f;
      lastFrameNanos = now;
      if (dt <= 0f) dt = 1f / 60f;
      if (dt > 0.05f) dt = 0.05f;

      time += dt;
      advanceThemeTransition(dt);
      detectFrameHealth(dt);
      updateBubbles(dt);
      invalidate();
      choreographer.postFrameCallback(frameCallback);
    }
  };

  private boolean running = false;
  private long lastFrameNanos = 0;
  private float time = 0f;

  // 帧率自适应
  private int slowFrames = 0;
  private int fastFrames = 0;
  private int layerCount = MAX_LAYERS;

  // 波浪实例（phase 随 time 推进）
  private final FluidPhysics.Wave[] waves = new FluidPhysics.Wave[MAX_LAYERS];

  // 气泡
  private float[] bubbleX, bubbleY, bubbleR, bubbleA, bubbleS;
  private static final int BUBBLE_COUNT = 6;

  // 光斑（径向渐变 Shader 缓存）
  private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private float glow1X, glow1Y, glow2X, glow2Y;

  // 壁纸层（可选）
  private Bitmap wallpaper;
  private final Paint wallpaperPaint = new Paint();

  // 主题过渡
  private int[] fromColors = null, toColors = null;
  private float themeT = 1f; // 1 = 过渡完成
  private int curW1, curW2, curW3, curGlow;

  private float density = 1f;

  public LiquidBackgroundView(Context context) {
    super(context);
    density = getResources().getDisplayMetrics().density;
    for (int i = 0; i < MAX_LAYERS; i++) {
      waves[i] = new FluidPhysics.Wave(
          WAVE_DEFS[i][0] * density,
          WAVE_DEFS[i][1],
          WAVE_DEFS[i][2],
          random.nextFloat() * 6.28f);
    }
    initBubbles();
    ThemeManager.Theme t = ThemeManager.get().getTheme();
    applyThemeColors(t);
  }

  private void initBubbles() {
    bubbleX = new float[BUBBLE_COUNT];
    bubbleY = new float[BUBBLE_COUNT];
    bubbleR = new float[BUBBLE_COUNT];
    bubbleA = new float[BUBBLE_COUNT];
    bubbleS = new float[BUBBLE_COUNT];
    for (int i = 0; i < BUBBLE_COUNT; i++) {
      bubbleR[i] = (4f + random.nextFloat() * 7f) * density;
      bubbleA[i] = 0.10f + random.nextFloat() * 0.18f;
      bubbleS[i] = 12f + random.nextFloat() * 18f; // dp/s 上浮速度
      bubbleX[i] = random.nextFloat();
      bubbleY[i] = random.nextFloat();
    }
  }

  private void applyThemeColors(ThemeManager.Theme t) {
    curW1 = t.w1; curW2 = t.w2; curW3 = t.w3;
    curGlow = Color.argb(30, 255, 255, 255);
  }

  // ==================== 主题切换（600ms 插值） ====================

  @Override
  public void onThemeChanged(ThemeManager.Theme theme, boolean dark) {
    fromColors = new int[] {curW1, curW2, curW3};
    toColors = new int[] {theme.w1, theme.w2, theme.w3};
    themeT = 0f;
  }

  private void advanceThemeTransition(float dt) {
    if (themeT >= 1f) return;
    themeT = Math.min(1f, themeT + dt / 0.6f);
    float e = themeT;
    curW1 = ThemeManager.lerpColor(fromColors[0], toColors[0], e);
    curW2 = ThemeManager.lerpColor(fromColors[1], toColors[1], e);
    curW3 = ThemeManager.lerpColor(fromColors[2], toColors[2], e);
  }

  // ==================== 帧率自适应 ====================

  private void detectFrameHealth(float dt) {
    if (dt > 0.033f) { // >20ms/帧（约30fps以下）
      slowFrames++;
      fastFrames = 0;
      if (slowFrames >= 3 && layerCount > 2) {
        layerCount = 2;
        slowFrames = 0;
      }
    } else {
      fastFrames++;
      slowFrames = 0;
      if (fastFrames >= 90 && layerCount < MAX_LAYERS) {
        layerCount = MAX_LAYERS;
        fastFrames = 0;
      }
    }
  }

  // ==================== 生命周期 ====================

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    ThemeManager.get().addListener(this);
    start();
  }

  @Override
  protected void onDetachedFromWindow() {
    ThemeManager.get().removeListener(this);
    stop();
    super.onDetachedFromWindow();
  }

  @Override
  public void onWindowFocusChanged(boolean hasWindowFocus) {
    super.onWindowFocusChanged(hasWindowFocus);
    if (hasWindowFocus) start();
    else stop();
  }

  public void start() {
    if (running) return;
    running = true;
    lastFrameNanos = System.nanoTime();
    choreographer.postFrameCallback(frameCallback);
  }

  public void stop() {
    running = false;
    choreographer.removeFrameCallback(frameCallback);
  }

  // ==================== 壁纸 ====================

  /** 设置壁纸层（CENTER_CROP 铺满，绘制在波浪之下）。传 null 清除。 */
  public void setWallpaper(Bitmap bitmap) {
    wallpaper = bitmap;
    invalidate();
  }

  // ==================== 绘制 ====================

  private void updateBubbles(float dt) {
    float h = getHeight();
    for (int i = 0; i < BUBBLE_COUNT; i++) {
      bubbleY[i] -= bubbleS[i] * dt / (h <= 0 ? 1 : h);
      if (bubbleY[i] < -0.05f) {
        bubbleY[i] = 1.05f;
        bubbleX[i] = random.nextFloat();
      }
    }
  }

  @Override
  protected void onDraw(Canvas c) {
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    // 1. 壁纸层
    if (wallpaper != null) {
      Rect src = new Rect(0, 0, wallpaper.getWidth(), wallpaper.getHeight());
      float scale = Math.max(w / (float) wallpaper.getWidth(), h / (float) wallpaper.getHeight());
      int dw = (int) (wallpaper.getWidth() * scale);
      int dh = (int) (wallpaper.getHeight() * scale);
      Rect dst = new Rect((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2);
      wallpaperPaint.setAlpha(200);
      c.drawBitmap(wallpaper, src, dst, wallpaperPaint);
      wallpaperPaint.setAlpha(255);
    }

    // 2. 全屏液体底色（垂直渐变 w1→w3，透明度低）
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(new LinearGradient(
        0, 0, 0, h,
        withAlpha(curW1, 42), withAlpha(curW3, 26), Shader.TileMode.CLAMP));
    c.drawRect(0, 0, w, h, paint);
    paint.setShader(null);

    // 3. 波浪层（由远到近）
    int layers = layerCount;
    for (int layer = 0; layer < layers; layer++) {
      drawWaveLayer(c, w, h, layer, layers);
    }

    // 4. 光斑
    drawGlow(c, w, h);
  }

  private void drawWaveLayer(Canvas c, int w, int h, int layer, int layers) {
    FluidPhysics.Wave wave = waves[layer];
    float baseY = h * WAVE_DEFS[layer][3];
    float alphaFill = 0.20f - layer * 0.05f; // 近层更实
    float alphaLine = 0.16f - layer * 0.04f;

    // 波浪路径：x 步长 4dp
    float step = 4f * density;
    path.reset();
    path.moveTo(0, h);
    float y0 = baseY + wave.y(0, time);
    path.lineTo(0, y0);
    for (float x = step; x <= w + step; x += step) {
      path.lineTo(x, baseY + wave.y(x, time));
    }
    path.lineTo(w, h);
    path.close();

    // 填充：该层色 → 透明（向下）
    int color = layer == 0 ? curW1 : layer == 1 ? curW2 : curW3;
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(new LinearGradient(
        0, baseY - wave.amp * 2f, 0, h,
        withAlpha(color, (int) (255 * alphaFill)), withAlpha(color, 0), Shader.TileMode.CLAMP));
    c.drawPath(path, paint);
    paint.setShader(null);

    // 波顶高光细线
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.2f * density);
    paint.setColor(Color.argb((int) (255 * alphaLine), 255, 255, 255));
    path.reset();
    path.moveTo(0, y0);
    for (float x = step; x <= w + step; x += step) {
      path.lineTo(x, baseY + wave.y(x, time));
    }
    c.drawPath(path, paint);
  }

  private void drawGlow(Canvas c, int w, int h) {
    // 两个光斑缓慢漂移（径向渐变，白色低透明）
    float t = time;
    float gx1 = w * (0.30f + 0.16f * (float) Math.sin(t * 0.11f));
    float gy1 = h * (0.22f + 0.06f * (float) Math.sin(t * 0.17f + 1.2f));
    float gx2 = w * (0.74f + 0.14f * (float) Math.sin(t * 0.09f + 2.4f));
    float gy2 = h * (0.60f + 0.08f * (float) Math.sin(t * 0.13f + 3.1f));

    drawGlowAt(c, gx1, gy1, 150f * density, 0.10f);
    drawGlowAt(c, gx2, gy2, 110f * density, 0.07f);
    glow1X = gx1; glow1Y = gy1; glow2X = gx2; glow2Y = gy2;

    // 气泡
    paint.setStyle(Paint.Style.FILL);
    for (int i = 0; i < BUBBLE_COUNT; i++) {
      float bx = bubbleX[i] * w;
      float by = bubbleY[i] * h;
      float breath = 0.75f + 0.25f * (float) Math.sin(time * 0.8f + i * 1.7f);
      paint.setColor(Color.argb((int) (255 * bubbleA[i] * breath), 255, 255, 255));
      c.drawCircle(bx, by, bubbleR[i] * breath, paint);
    }
  }

  private void drawGlowAt(Canvas c, float x, float y, float radius, float alpha) {
    // 缓存 Shader，避免每帧创建（RadialGradient 以原点为中心，通过 translate 定位）
    glowPaint.setShader(new RadialGradient(
        0, 0, radius,
        new int[] {Color.argb((int) (255 * alpha), 255, 255, 255), Color.TRANSPARENT},
        new float[] {0f, 1f}, Shader.TileMode.CLAMP));
    c.save();
    c.translate(x, y);
    c.drawCircle(0, 0, radius, glowPaint);
    c.restore();
    glowPaint.setShader(null);
  }

  private int withAlpha(int color, int alpha) {
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
  }
}
