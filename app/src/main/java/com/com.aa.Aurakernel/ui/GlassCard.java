package com.aa.Aurakernel.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.aa.Aurakernel.ui.core.ThemeManager;

/**
 * 液态玻璃卡片容器：半透明玻璃底 + 1px 高光描边 + 顶部内高光线 + 底部内侧阴影。
 * - 主题变化自动刷新（页面整体重建场景下旧实例即弃，无需插值）。
 * - 可选按压弹性（setPressable(true)）：按下 scale 0.97，松手回弹。
 */
public class GlassCard extends FrameLayout implements ThemeManager.Listener {

  private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint topGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint innerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();
  private final float density;

  private float radiusDp = 22f;
  private boolean pressable = false;
  private boolean pressed = false;
  private float scale = 1f;
  private ValueAnimator pressAnim;

  public GlassCard(Context context) {
    super(context);
    density = getResources().getDisplayMetrics().density;
    setWillNotDraw(false);
    ThemeManager.Theme t = ThemeManager.get().getTheme();
    applyTheme(t);
    ThemeManager.get().addListener(this);
  }

  /** 圆角半径（dp），默认 22。 */
  public void setCornerRadius(float radiusDp) {
    this.radiusDp = radiusDp;
    invalidate();
  }

  /** 启用按压弹性（0.97 回弹）。 */
  public void setPressable(boolean pressable) {
    this.pressable = pressable;
  }

  @Override
  public void onThemeChanged(ThemeManager.Theme theme, boolean dark) {
    applyTheme(theme);
    invalidate();
  }

  private void applyTheme(ThemeManager.Theme t) {
    bgPaint.setColor(t.glass);
    borderPaint.setColor(t.border);
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setStrokeWidth(density * 1f);
  }

  @Override
  protected void onDetachedFromWindow() {
    ThemeManager.get().removeListener(this);
    super.onDetachedFromWindow();
  }

  // ==================== 按压弹性 ====================

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!pressable) return super.onTouchEvent(event);
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        pressed = true;
        animateScale(0.97f, 90);
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (pressed) {
          pressed = false;
          animateScale(1f, 320);
        }
        break;
    }
    return super.onTouchEvent(event);
  }

  private void animateScale(float target, long duration) {
    if (pressAnim != null) pressAnim.cancel();
    pressAnim = ValueAnimator.ofFloat(scale, target);
    pressAnim.setDuration(duration);
    pressAnim.setInterpolator(new DecelerateInterpolator(1.4f));
    pressAnim.addUpdateListener(a -> {
      scale = (float) a.getAnimatedValue();
      setScaleX(scale);
      setScaleY(scale);
    });
    pressAnim.start();
  }

  // ==================== 绘制 ====================

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    float w = getWidth();
    float h = getHeight();
    if (w <= 0 || h <= 0) return;
    float radius = radiusDp * density;
    rect.set(0, 0, w, h);

    // 1. 玻璃底
    bgPaint.setStyle(Paint.Style.FILL);
    c.drawRoundRect(rect, radius, radius, bgPaint);

    // 2. 顶部内高光线（白色渐变，模拟玻璃上沿反光）
    topGlowPaint.setStyle(Paint.Style.FILL);
    topGlowPaint.setShader(new LinearGradient(
        0, 0, 0, Math.min(h * 0.22f, 40f * density),
        Color.argb(60, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP));
    RectF topRect = new RectF(radius * 0.5f, 0, w - radius * 0.5f, Math.min(h * 0.22f, 40f * density));
    c.drawRoundRect(topRect, radius, radius, topGlowPaint);
    topGlowPaint.setShader(null);

    // 3. 底部内侧阴影（柔和）
    innerShadowPaint.setStyle(Paint.Style.FILL);
    innerShadowPaint.setShader(new LinearGradient(
        0, h - Math.min(h * 0.3f, 46f * density), 0, h,
        Color.TRANSPARENT, Color.argb(26, 0, 0, 0), Shader.TileMode.CLAMP));
    RectF bottomRect = new RectF(radius * 0.5f, h - Math.min(h * 0.3f, 46f * density), w - radius * 0.5f, h);
    c.drawRoundRect(bottomRect, radius, radius, innerShadowPaint);
    innerShadowPaint.setShader(null);

    // 4. 1px 高光描边
    borderPaint.setStyle(Paint.Style.STROKE);
    c.drawRoundRect(rect, radius, radius, borderPaint);
  }
}
