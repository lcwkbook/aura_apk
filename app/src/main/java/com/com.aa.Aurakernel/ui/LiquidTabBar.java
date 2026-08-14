package com.aa.Aurakernel.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aa.Aurakernel.ui.core.FluidPhysics;
import com.aa.Aurakernel.ui.core.ThemeManager;

/**
 * 液态 Tab 栏：玻璃底 + 液体指示球（弹簧甩动、冲刺拉长、回弹压扁、连点搅动、涟漪）。
 * - 点击：球以初速 v += Δx*4.2 甩向目标，欠阻尼自然回弹（Q弹）；图标弹性缩放反馈。
 * - 形变：冲向目标时柔和拉长（上限1.22），越过目标回弹时轻微压扁，面积守恒近似。
 * - 长按 200ms 进入拖拽：球跟随手指，悬停实时切页；松手弹簧自然弹回（Q弹收尾）。
 * - 图标/文字颜色逐帧渐变插值，切换丝滑不硬切。
 */
public class LiquidTabBar extends FrameLayout implements ThemeManager.Listener {

  public interface Listener {
    void onSelect(int tab);

    /** 拖拽悬停变化（实时切页）。 */
    void onHover(int tab);

    /** 拖拽结束（页面已由 onHover 切换完毕）。 */
    void onDragEnd();
  }

  private static final float SPRING_K = 340f;
  private static final float SPRING_C = 30f;
  private static final float ENERGY_DECAY_PER_SEC = 0.9f; // 0.015/帧 @60fps
  private static final float ENERGY_CAP = 1.6f;
  private static final long LONG_PRESS_MS = 200L;

  private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint blobGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Path blobPath = new Path();
  private final RectF rect = new RectF();
  private final float density;

  private final FluidPhysics.Spring spring = new FluidPhysics.Spring(0f);
  private final FluidPhysics.EnergyPool energy = new FluidPhysics.EnergyPool(ENERGY_DECAY_PER_SEC, ENERGY_CAP);
  private final Handler handler = new Handler(Looper.getMainLooper());

  private Listener listener;
  private String[] emojis;
  private String[] labels;
  private final LinearLayout[] tabs = new LinearLayout[4];
  private TextView[] tabIcons;
  private TextView[] tabLabels;

  private int activeTab = 0;
  private int hoverTab = -1;
  private boolean dragging = false;
  private boolean draggingActive = false;
  private float startX = 0f;
  private float lastDragX = 0f;
  private float targetX = 0f;
  private float time = 0f;
  private long lastFrameNanos = 0;
  private boolean running = false;

  private float rippleX = -1f;
  private float rippleR = 0f;
  private static final float RIPPLE_MAX = 46f;

  // 形变平滑：stretch 目标值每帧低通滤波逼近，避免生硬突变
  private float smoothStretch = 1f;

  private int glassColor, borderColor, acc1, acc2, acc3, acc4, iconNormal, iconActive;
  private boolean dark;

  // 图标/标签颜色逐帧渐变插值（丝滑过渡）
  private final float[] iconR = new float[4];
  private final float[] iconG = new float[4];
  private final float[] iconB = new float[4];
  private final int[] lastIconColor = new int[4];
  private boolean colorInit = false;

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

      if (!draggingActive) {
        spring.step(targetX, SPRING_K, SPRING_C, dt);
      }
      energy.step(dt);

      if (rippleX >= 0f) {
        rippleR += dt * 160f * density;
        if (rippleR > RIPPLE_MAX * density) rippleX = -1f;
      }

      lerpTabColors(dt);

      invalidate();
      Choreographer.getInstance().postFrameCallback(frameCallback);
    }
  };

  public LiquidTabBar(Context context, String[] emojis, String[] labels) {
    super(context);
    density = getResources().getDisplayMetrics().density;
    this.emojis = emojis;
    this.labels = labels;
    setWillNotDraw(false);
    setClickable(true);

    ThemeManager.Theme t = ThemeManager.get().getTheme();
    applyTheme(t, ThemeManager.get().isDark());
    ThemeManager.get().addListener(this);

    buildTabs();
    spring.snap(tabCenter(0));
    targetX = tabCenter(0);
  }

  private void buildTabs() {
    LinearLayout row = new LinearLayout(getContext());
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(android.view.Gravity.CENTER);
    row.setPadding(dp(6), dp(6), dp(6), dp(6));
    FrameLayout.LayoutParams rowLp = new FrameLayout.LayoutParams(-1, -1);
    rowLp.gravity = android.view.Gravity.CENTER_VERTICAL;
    addView(row, rowLp);

    tabIcons = new TextView[4];
    tabLabels = new TextView[4];
    for (int i = 0; i < 4; i++) {
      final int index = i;
      LinearLayout tab = new LinearLayout(getContext());
      tab.setOrientation(LinearLayout.HORIZONTAL);
      tab.setGravity(android.view.Gravity.CENTER);
      tab.setPadding(dp(6), dp(4), dp(6), dp(4));

      TextView icon = new TextView(getContext());
      icon.setText(emojis[i]);
      icon.setTextSize(20);
      icon.setGravity(android.view.Gravity.CENTER);
      icon.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
      LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(28), dp(28));
      iconLp.setMargins(0, 0, dp(4), 0);
      tab.addView(icon, iconLp);

      TextView lbl = new TextView(getContext());
      lbl.setText(labels[i]);
      lbl.setTextSize(12);
      lbl.setGravity(android.view.Gravity.CENTER);
      tab.addView(lbl);

      tabIcons[i] = icon;
      tabLabels[i] = lbl;
      tabs[i] = tab;
      row.addView(tab, new LinearLayout.LayoutParams(0, dp(52), 1f));
    }
    refreshTabColors();
  }

  // ==================== 对外接口 ====================

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /** 外部切页后同步（页面切换/初始化）。animate=false 时球直接落位。 */
  public void setActive(int tab, boolean animate) {
    if (tab < 0 || tab >= 4) return;
    activeTab = tab;
    float center = tabCenter(tab);
    if (animate) {
      targetX = center;
      float dx = center - spring.x;
      spring.impulse(dx * 3.6f); // 甩动注入（柔和，避免拉太长）
      energy.add(0.35f);
      rippleX = center;
      rippleR = 0f;
    } else {
      spring.snap(center);
      targetX = center;
    }
    refreshTabColors();
  }

  /** 触发点击涟漪（外部调用，如悬停切页）。 */
  public void rippleAt(float x) {
    rippleX = x;
    rippleR = 0f;
  }

  // ==================== 主题 ====================

  @Override
  public void onThemeChanged(ThemeManager.Theme theme, boolean dark) {
    applyTheme(theme, dark);
    refreshTabColors();
    invalidate();
  }

  private void applyTheme(ThemeManager.Theme t, boolean dark) {
    this.dark = dark;
    glassColor = t.glass;
    borderColor = t.border;
    acc1 = t.acc;
    acc2 = t.grad; // 渐变搭档色
    acc3 = t.grad3; // 渐变第三色
    acc4 = t.grad4; // 渐变第四色（相近色相多色渐变）
    iconNormal = t.sub;
    iconActive = Color.WHITE;
  }

  private void refreshTabColors() {
    if (tabIcons == null) return;
    // 颜色交由 lerpTabColors 逐帧渐变插值；这里只负责加粗切换
    for (int i = 0; i < 4; i++) {
      boolean active = (i == activeTab) || (draggingActive && i == hoverTab);
      tabLabels[i].setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
    }
    if (!colorInit) {
      // 首次：直接落位，避免从黑渐变
      colorInit = true;
      for (int i = 0; i < 4; i++) {
        boolean active = (i == activeTab) || (draggingActive && i == hoverTab);
        int target = active ? iconActive : iconNormal;
        iconR[i] = Color.red(target);
        iconG[i] = Color.green(target);
        iconB[i] = Color.blue(target);
        lastIconColor[i] = target;
        tabIcons[i].setTextColor(target);
        tabLabels[i].setTextColor(target);
      }
    }
  }

  /** 图标/标签颜色逐帧向目标色渐变（丝滑过渡，避免硬切）。 */
  private void lerpTabColors(float dt) {
    if (tabIcons == null || !colorInit) return;
    float f = Math.min(1f, dt * 12f); // 快速但平滑的趋近
    for (int i = 0; i < 4; i++) {
      boolean active = (i == activeTab) || (draggingActive && i == hoverTab);
      int target = active ? iconActive : iconNormal;
      float tr = Color.red(target);
      float tg = Color.green(target);
      float tb = Color.blue(target);
      iconR[i] += (tr - iconR[i]) * f;
      iconG[i] += (tg - iconG[i]) * f;
      iconB[i] += (tb - iconB[i]) * f;
      int cur = Color.rgb((int) iconR[i], (int) iconG[i], (int) iconB[i]);
      if (cur != lastIconColor[i]) {
        lastIconColor[i] = cur;
        tabIcons[i].setTextColor(cur);
        tabLabels[i].setTextColor(cur);
      }
    }
  }

  // ==================== 手势 ====================

  private final Runnable longPressRunnable = new Runnable() {
    @Override
    public void run() {
      draggingActive = true;
      hoverTab = activeTab;
      try {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) v.vibrate(20);
      } catch (Exception ignored) {
      }
      refreshTabColors();
    }
  };

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        startX = event.getX();
        lastDragX = startX;
        dragging = true;
        handler.removeCallbacks(longPressRunnable);
        handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
        return true;

      case MotionEvent.ACTION_MOVE:
        if (draggingActive) {
          float x = clampX(event.getX());
          spring.snap(x); // 拖拽中球直接跟随手指
          lastDragX = x;
          int hover = hoverIndexAt(x);
          if (hover != hoverTab) {
            hoverTab = hover;
            refreshTabColors();
            if (listener != null) listener.onHover(hover);
          }
        } else {
          if (Math.abs(event.getX() - startX) > dp(10)) {
            handler.removeCallbacks(longPressRunnable);
          }
        }
        return true;

      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        handler.removeCallbacks(longPressRunnable);
        if (draggingActive) {
          draggingActive = false;
          if (hoverTab >= 0) {
            activeTab = hoverTab;
            targetX = tabCenter(hoverTab);
            // 不硬吸附：弹簧从当前跟随位置自然弹回目标（Q弹收尾）
            float dx = targetX - spring.x;
            spring.impulse(dx * 1.8f);
          }
          hoverTab = -1;
          if (listener != null) listener.onDragEnd();
        } else {
          float cx = event.getX();
          int tab = (int) ((cx - dp(6)) / tabWidth());
          tab = Math.max(0, Math.min(3, tab));
          energy.add(0.35f); // 连点搅动（同 tab 连点也生效）
          rippleX = cx;
          rippleR = 0f;
          bounceIcon(tab); // Q弹：图标弹性缩放反馈
          if (listener != null) listener.onSelect(tab);
        }
        dragging = false;
        refreshTabColors();
        return true;
    }
    return true;
  }

  /** Q弹反馈：点击 tab 时图标/文字弹性缩放（Overshoot 过冲回弹）。 */
  private void bounceIcon(int tab) {
    if (tab < 0 || tab >= 4) return;
    tabIcons[tab].animate().cancel();
    tabIcons[tab].setScaleX(0.8f);
    tabIcons[tab].setScaleY(0.8f);
    tabIcons[tab]
        .animate()
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(300)
        .setInterpolator(new OvershootInterpolator(1.5f))
        .start();
    if (tabLabels[tab] != null) {
      tabLabels[tab].animate().cancel();
      tabLabels[tab].setScaleX(0.9f);
      tabLabels[tab].setScaleY(0.9f);
      tabLabels[tab]
          .animate()
          .scaleX(1f)
          .scaleY(1f)
          .setDuration(300)
          .setInterpolator(new OvershootInterpolator(1.5f))
          .start();
    }
  }

  // ==================== 几何 ====================

  /** 首次布局（宽度可用）时校正球位置：构造时宽度为 0，按 0 宽计算的球位是错的。 */
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (w > 0 && (oldw == 0 || spring.x < dp(4))) {
      float center = tabCenter(activeTab);
      spring.snap(center);
      targetX = center;
    }
  }

  private float tabWidth() {
    int w = getWidth();
    if (w <= 0) return 1f;
    return (w - dp(12)) / 4f;
  }

  private float tabCenter(int tab) {
    return dp(6) + tabWidth() * (tab + 0.5f);
  }

  private int hoverIndexAt(float x) {
    int i = (int) ((x - dp(6)) / tabWidth());
    return Math.max(0, Math.min(3, i));
  }

  private float clampX(float x) {
    float half = (tabWidth() - dp(12)) / 2f;
    return Math.max(dp(6) + half, Math.min(getWidth() - dp(6) - half, x));
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
    handler.removeCallbacks(longPressRunnable);
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

  // ==================== 绘制 ====================

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    // 玻璃底
    float radius = h / 2f;
    rect.set(0, 0, w, h);
    barPaint.setStyle(Paint.Style.FILL);
    barPaint.setColor(glassColor);
    c.drawRoundRect(rect, radius, radius, barPaint);

    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setStrokeWidth(density);
    borderPaint.setColor(borderColor);
    c.drawRoundRect(rect, radius, radius, borderPaint);

    // 液体球：冲刺柔和拉长（上限1.22）+ 回弹轻微压扁（Q弹），面积守恒近似
    float ballW = tabWidth() - dp(12);
    float ballH = h - dp(14);
    if (ballW > 0 && ballH > 0) {
      float vx = spring.v;
      float dx = targetX - spring.x;
      boolean approaching = (vx * dx) > 0f; // 正在接近目标（同向）
      float targetStretch;
      if (approaching) {
        // 冲向目标：柔和拉长
        targetStretch = Math.min(1.22f, 1f + Math.abs(vx) / 1600f + energy.value() * 0.3f);
      } else {
        // 越过目标回弹：轻微压扁（Q弹手感）
        targetStretch = Math.max(0.93f, 1f - Math.abs(vx) / 3500f - energy.value() * 0.15f);
      }
      // 低通滤波：形变平滑过渡，不突变（更柔顺）
      smoothStretch += (targetStretch - smoothStretch) * 0.16f;
      float stretch = smoothStretch;
      float jitter = (float) Math.sin(time * 30f) * energy.value() * 1.6f * density;
      float cx = spring.x + jitter;
      float bw = ballW * stretch;
      float bh = ballH / (0.75f + 0.25f * stretch);

      blobPath.reset();
      float left = cx - bw / 2f, right = cx + bw / 2f;
      float top = (h - bh) / 2f, bottom = (h + bh) / 2f;
      float r = bh / 2f;
      // 圆润胶囊形（无尖端箭头），仅保留速度方向的拉伸形变
      blobPath.addRoundRect(new RectF(left, top, right, bottom), r, r, Path.Direction.CW);

      // 球渐变（四色相近渐变）
      blobPaint.setStyle(Paint.Style.FILL);
      blobPaint.setShader(new LinearGradient(
          0, top, 0, bottom, new int[] {acc1, acc2, acc3, acc4}, null, Shader.TileMode.CLAMP));
      c.drawPath(blobPath, blobPaint);
      blobPaint.setShader(null);

      // 球顶高光
      blobGlowPaint.setStyle(Paint.Style.FILL);
      blobGlowPaint.setShader(new LinearGradient(
          0, top, 0, top + bh * 0.4f,
          Color.argb(90, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP));
      c.drawPath(blobPath, blobGlowPaint);
      blobGlowPaint.setShader(null);
    }

    // 涟漪（球之上）
    if (rippleX >= 0f) {
      float max = RIPPLE_MAX * density;
      float alpha = (1f - rippleR / max) * 0.4f;
      if (alpha > 0f) {
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(dp(1.6f) * (1f - rippleR / max * 0.5f));
        ripplePaint.setColor(Color.argb((int) (255 * alpha), 255, 255, 255));
        c.drawCircle(rippleX, h / 2f, rippleR, ripplePaint);
      }
    }
  }

  private int dp(float v) {
    return (int) (v * density + 0.5f);
  }
}
