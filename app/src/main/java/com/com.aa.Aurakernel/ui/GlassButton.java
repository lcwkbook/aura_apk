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
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

import com.aa.Aurakernel.ui.core.ThemeManager;

/**
 * 液态玻璃按钮：主题强调色渐变底 + 顶部高光 + 按压弹性（0.93 回弹）。
 * 替代现有 button()/createModernButton() 的实心圆角按钮。
 */
public class GlassButton extends Button implements ThemeManager.Listener {

  private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();
  private final float density;

  private float radiusDp = 14f;
  private float scale = 1f;
  private ValueAnimator pressAnim;
  private boolean pressed = false;
  private boolean customAcc = false;
  private int customAccColor;

  public GlassButton(Context context) {
    super(context);
    density = getResources().getDisplayMetrics().density;
    setWillNotDraw(false);
    setAllCaps(false);
    setMinHeight(0);
    setMinimumHeight(0);
    if (android.os.Build.VERSION.SDK_INT >= 21) {
      setStateListAnimator(null);
      setElevation(0f);
    }
    ThemeManager.Theme t = ThemeManager.get().getTheme();
    applyTheme(t);
    ThemeManager.get().addListener(this);
  }

  public void setCornerRadius(float radiusDp) {
    this.radiusDp = radiusDp;
    invalidate();
  }

  /** 自定义强调色（如业务按钮的红色/蓝色），设置后不受主题色系影响。 */
  public void setCustomColors(int solid) {
    customAcc = true;
    customAccColor = solid;
    rebuildShader();
    invalidate();
  }

  @Override
  public void onThemeChanged(ThemeManager.Theme theme, boolean dark) {
    applyTheme(theme);
    invalidate();
  }

  private void applyTheme(ThemeManager.Theme t) {
    rebuildShader();
  }

  private void rebuildShader() {
    int base = customAcc ? customAccColor : ThemeManager.get().getTheme().acc;
    int grad = ThemeManager.get().getTheme().grad; // 渐变搭档色（混搭渐变）
    boolean dark = ThemeManager.get().isDark();
    // 浅渐变玻璃：主题色 → 搭档色 双色渐变 + 同色描边
    bgPaint.setShader(new LinearGradient(
        0, 0, 0, 120 * density,
        withAlpha(base, dark ? 118 : 84),
        withAlpha(grad, dark ? 42 : 16),
        Shader.TileMode.CLAMP));
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setStrokeWidth(Math.max(1f, density));
    borderPaint.setColor(withAlpha(base, dark ? 230 : 160));
    setTextColor(dark ? Color.WHITE : base);
  }

  private static int withAlpha(int color, int alpha) {
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
  }

  private static int lighten(int color, float f) {
    int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
    return Color.rgb(
        Math.min(255, Math.round(r + (255 - r) * f)),
        Math.min(255, Math.round(g + (255 - g) * f)),
        Math.min(255, Math.round(b + (255 - b) * f)));
  }

  @Override
  protected void onDetachedFromWindow() {
    ThemeManager.get().removeListener(this);
    super.onDetachedFromWindow();
  }

  // ==================== 按压弹性 ====================

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        pressed = true;
        animateScale(0.93f, 100);
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (pressed) {
          pressed = false;
          animateScale(1f, 340);
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
    float w = getWidth();
    float h = getHeight();
    if (w <= 0 || h <= 0) {
      super.onDraw(c);
      return;
    }
    float radius = radiusDp * density;
    rect.set(0, 0, w, h);

    // 渐变玻璃底
    bgPaint.setStyle(Paint.Style.FILL);
    c.drawRoundRect(rect, radius, radius, bgPaint);

    // 同色描边
    c.drawRoundRect(rect, radius, radius, borderPaint);

    // 顶部高光
    glowPaint.setStyle(Paint.Style.FILL);
    glowPaint.setShader(new LinearGradient(
        0, 0, 0, h * 0.45f,
        Color.argb(50, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP));
    c.drawRoundRect(rect, radius, radius, glowPaint);
    glowPaint.setShader(null);

    // 文字（居中）
    super.onDraw(c);
  }
}
