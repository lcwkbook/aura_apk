package com.aa.Aurakernel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import com.aa.Aurakernel.ui.core.FluidPhysics;
import com.aa.Aurakernel.ui.core.ThemeManager;

/**
 * 流体进度条：玻璃凹槽 + 双频波浪填充（主波 + 高光波，无缝滚动）。
 * - setProgress()：显示值平滑逼近目标；变化瞬间波浪流速 ×4.7 并在 0.55s 内回落（涌起）。
 * - 可拖动：拖动改值，松手涌起回弹（下载中请 setDraggable(false)）。
 */
public class FluidProgressView extends View implements ThemeManager.Listener {

  public interface Listener {
    void onProgressChanged(float progress);
  }

  private static final float SURGE_DURATION = 0.55f;
  private static final float SURGE_SPEED_MULT = 4.7f;
  private static final float WAVE_SPEED = 2.2f; // rad/s 基线流速

  private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path wavePath = new Path();
  private final RectF rect = new RectF();
  private final float density;

  private float target = 0f;   // 目标进度 0-100
  private float display = 0f;  // 显示进度（平滑逼近）
  private float phase = 0f;
  private float surgeT = 0f;   // >0 表示涌起中
  private float time = 0f;
  private long lastFrameNanos = 0;
  private boolean running = false;

  private boolean draggable = false;
  private boolean dragging = false;
  private Listener listener;

  private int accColor, gradColor, grad3Color, grad4Color, trackColor, borderColor;

  public FluidProgressView(Context context) {
    super(context);
    density = getResources().getDisplayMetrics().density;
    ThemeManager.Theme t = ThemeManager.get().getTheme();
    applyTheme(t);
    ThemeManager.get().addListener(this);
  }

  // ==================== 接口 ====================

  /** 设置进度 0-100（触发涌起动画）。 */
  public void setProgress(float p) {
    p = FluidPhysics.clamp(p, 0f, 100f);
    if (p != target) {
      target = p;
      triggerSurge();
    }
  }

  /** 静默设置进度（不触发涌起，用于平滑动画如倒计时）。 */
  public void setProgressSilent(float p) {
    p = FluidPhysics.clamp(p, 0f, 100f);
    if (p != target) target = p;
  }

  public float getProgress() {
    return target;
  }

  /** 拖动中请设为 false（如下载进行中）。 */
  public void setDraggable(boolean draggable) {
    this.draggable = draggable;
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  private void triggerSurge() {
    surgeT = SURGE_DURATION;
  }

  // ==================== 主题 ====================

  @Override
  public void onThemeChanged(ThemeManager.Theme theme, boolean dark) {
    applyTheme(theme);
    invalidate();
  }

  private void applyTheme(ThemeManager.Theme t) {
    accColor = t.acc;
    gradColor = t.grad; // 渐变搭档色
    grad3Color = t.grad3; // 渐变第三色
    grad4Color = t.grad4; // 渐变第四色（相近色相多色渐变）
    trackColor = t.glass;
    borderColor = t.border;
  }

  // ==================== 触摸拖动 ====================

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!draggable) return false;
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        dragging = true;
        updateFromX(event.getX());
        return true;
      case MotionEvent.ACTION_MOVE:
        if (dragging) updateFromX(event.getX());
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (dragging) {
          dragging = false;
          triggerSurge();
          if (listener != null) listener.onProgressChanged(target);
        }
        return true;
    }
    return false;
  }

  private void updateFromX(float x) {
    int w = getWidth();
    if (w <= 0) return;
    target = FluidPhysics.clamp(x / w * 100f, 0f, 100f);
    display = target; // 拖动时即时跟随
    invalidate();
  }

  // ==================== 生命周期 ====================

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
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

  private void start() {
    if (running) return;
    running = true;
    lastFrameNanos = System.nanoTime();
    Choreographer.getInstance().postFrameCallback(frameCallback);
  }

  private void stop() {
    running = false;
    Choreographer.getInstance().removeFrameCallback(frameCallback);
  }

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

      // 显示进度平滑逼近（拖动中已即时跟随，跳过）
      if (!dragging) {
        display += (target - display) * Math.min(1f, dt * 8f);
        if (Math.abs(target - display) < 0.05f) display = target;
      }

      // 涌起计时
      if (surgeT > 0f) surgeT = Math.max(0f, surgeT - dt);

      // 波浪相位推进（涌起时流速 ×4.7）
      float mult = 1f + (SURGE_SPEED_MULT - 1f) * (surgeT / SURGE_DURATION);
      phase += dt * WAVE_SPEED * mult;

      invalidate();
      Choreographer.getInstance().postFrameCallback(frameCallback);
    }
  };

  // ==================== 绘制 ====================

  @Override
  protected void onDraw(Canvas c) {
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;
    float radius = h / 2f;
    rect.set(0, 0, w, h);

    // 1. 玻璃凹槽
    trackPaint.setStyle(Paint.Style.FILL);
    trackPaint.setColor(trackColor);
    c.drawRoundRect(rect, radius, radius, trackPaint);

    // 2. 波浪填充（裁剪到圆角矩形内）
    float fillW = w * display / 100f;
    if (fillW > 2f) {
      float amp = h * (0.16f + 0.10f * (surgeT / SURGE_DURATION));
      Path clip = new Path();
      clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
      int save = c.save();
      c.clipPath(clip);

      // 双频叠加波浪路径
      wavePath.reset();
      wavePath.moveTo(0, h);
      float step = 3f * density;
      for (float x = 0; x <= fillW + step; x += step) {
        float y =
            h / 2f
                + amp * (float) Math.sin(x * 0.045f + phase)
                + amp * 0.55f * (float) Math.sin(x * 0.023f + phase * 1.3f + 1.1f);
        wavePath.lineTo(x, y);
      }
      wavePath.lineTo(fillW, h);
      wavePath.close();

      wavePaint.setStyle(Paint.Style.FILL);
      wavePaint.setShader(new LinearGradient(
          0, 0, 0, h, new int[] {accColor, gradColor, grad3Color, grad4Color}, null, Shader.TileMode.CLAMP));
      c.drawPath(wavePath, wavePaint);
      wavePaint.setShader(null);

      // 高光波（白色半透明，相位错开）
      highlightPaint.setStyle(Paint.Style.STROKE);
      highlightPaint.setStrokeWidth(Math.max(1f, density * 0.8f));
      highlightPaint.setColor(Color.argb(80, 255, 255, 255));
      wavePath.reset();
      wavePath.moveTo(0, h / 2f + amp * (float) Math.sin(phase + 2.4f));
      for (float x = step; x <= fillW + step; x += step) {
        float y =
            h / 2f
                + amp * (float) Math.sin(x * 0.045f + phase + 2.4f)
                + amp * 0.55f * (float) Math.sin(x * 0.023f + phase * 1.3f + 3.5f);
        wavePath.lineTo(x, y);
      }
      c.drawPath(wavePath, highlightPaint);

      c.restoreToCount(save);
    }

    // 3. 凹槽描边
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setStrokeWidth(density);
    borderPaint.setColor(borderColor);
    c.drawRoundRect(rect, radius, radius, borderPaint);
  }
}
