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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aa.Aurakernel.ui.core.FluidPhysics;
import com.aa.Aurakernel.ui.core.ThemeManager;

/**
 * 液态 Tab 栏：玻璃底 + 液体指示球（弹簧甩动、速度方向拉伸、连点搅动、涟漪）。
 * - 点击：球以初速 v += Δx*6 甩向目标，欠阻尼自然回弹；连点叠加能量抖动。
 * - 长按 200ms 进入拖拽：球跟随手指，悬停实时切页；松手弹簧吸附。
 */
public class LiquidTabBar extends FrameLayout implements ThemeManager.Listener {

  public interface Listener {
    void onSelect(int tab);

    /** 拖拽悬停变化（实时切页）。 */
    void onHover(int tab);

    /** 拖拽结束（页面已由 onHover 切换完毕）。 */
    void onDragEnd();
  }

  private static final float SPRING_K = 320f;
  private static final float SPRING_C = 26f;
  private static final float ENERGY_DECAY_PER_SEC = 1.08f; // 0.018/帧 @60fps
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

  private int glassColor, borderColor, acc1, acc2, iconNormal, iconActive;
  private boolean dark;

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
      spring.impulse(dx * 6f); // 甩动注入
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
    acc2 = ThemeManager.lerpColor(t.acc, t.w3, 0.45f);
    iconNormal = t.sub;
    iconActive = Color.WHITE;
  }

  private void refreshTabColors() {
    if (tabIcons == null) return;
    for (int i = 0; i < 4; i++) {
      boolean active = (i == activeTab) || (draggingActive && i == hoverTab);
      tabIcons[i].setTextColor(active ? iconActive : iconNormal);
      tabLabels[i].setTextColor(active ? iconActive : iconNormal);
      tabLabels[i].setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
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
            spring.snap(targetX); // 拖拽结束直接吸附（页面已实时切换）
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
          if (listener != null) listener.onSelect(tab);
        }
        dragging = false;
        refreshTabColors();
        return true;
    }
    return true;
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

    // 液体球
    float ballW = tabWidth() - dp(12);
    float ballH = h - dp(14);
    if (ballW > 0 && ballH > 0) {
      float vx = spring.v;
      float stretch = Math.min(1.6f, 1f + Math.abs(vx) / 900f + energy.value() * 0.55f);
      int dir = vx > 30f ? 1 : (vx < -30f ? -1 : 0);
      float jitter = (float) Math.sin(time * 30f) * energy.value() * 3.2f * density;
      float cx = spring.x + jitter;
      float bw = ballW * (dir == 0 ? 1f : stretch);
      float bh = ballH / (dir == 0 ? 1f : (0.55f + 0.45f * stretch));

      blobPath.reset();
      float left = cx - bw / 2f, right = cx + bw / 2f;
      float top = (h - bh) / 2f, bottom = (h + bh) / 2f;
      float r = bh / 2f;
      if (dir == 0) {
        blobPath.addRoundRect(new RectF(left, top, right, bottom), r, r, Path.Direction.CW);
      } else {
        float tip = (stretch - 1f) * 26f * density;
        if (dir > 0) {
          blobPath.moveTo(left, top);
          blobPath.lineTo(right - r, top);
          blobPath.quadTo(right - r * 0.25f, top, right + tip, (top + bottom) / 2f);
          blobPath.quadTo(right - r * 0.25f, bottom, right - r, bottom);
          blobPath.lineTo(left, bottom);
          blobPath.close();
        } else {
          blobPath.moveTo(right, top);
          blobPath.lineTo(left + r, top);
          blobPath.quadTo(left + r * 0.25f, top, left - tip, (top + bottom) / 2f);
          blobPath.quadTo(left + r * 0.25f, bottom, left + r, bottom);
          blobPath.lineTo(right, bottom);
          blobPath.close();
        }
      }

      // 球渐变
      blobPaint.setStyle(Paint.Style.FILL);
      blobPaint.setShader(new LinearGradient(
          0, top, 0, bottom, acc1, acc2, Shader.TileMode.CLAMP));
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
