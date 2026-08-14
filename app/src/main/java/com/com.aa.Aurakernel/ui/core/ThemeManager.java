package com.aa.Aurakernel.ui.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 液态主题管理器：3 色系（绿/青蓝/紫粉）× 明暗 = 6 套 token。
 * - 默认跟随系统深色模式（Activity.onConfigurationChanged 转发），手动选择后覆盖。
 * - 全部偏好持久化到 SharedPreferences。
 * - 主题变更通过 Listener 通知（自绘组件做颜色插值过渡；静态页面由 Activity 重建）。
 */
public final class ThemeManager {

  public enum Palette { GREEN, CYAN, PINK }

  /** 一套主题的全部 token（色值均为 ARGB int）。 */
  public static final class Theme {
    public final int w1, w2, w3;    // 波浪渐变三色
    public final int acc;           // 强调色
    public final int grad, grad3, grad4; // 渐变搭档色（按钮/液体球/进度条多色渐变的第二、三、四色，相近色相）
    public final int text, sub;     // 主文字 / 次级文字
    public final int glass, border; // 玻璃底色 / 玻璃高光描边
    public final int chipBg;        // 浅色标签底（原 tagColor 语义）

    public Theme(int w1, int w2, int w3, int acc, int grad, int grad3, int grad4, int text, int sub, int glass, int border, int chipBg) {
      this.w1 = w1; this.w2 = w2; this.w3 = w3; this.acc = acc;
      this.grad = grad;
      this.grad3 = grad3;
      this.grad4 = grad4;
      this.text = text; this.sub = sub; this.glass = glass; this.border = border;
      this.chipBg = chipBg;
    }
  }

  public interface Listener {
    /** 主题变化（色系或明暗变化都会触发）。 */
    void onThemeChanged(Theme theme, boolean dark);
  }

  private static final String PREFS = "liquid_theme";
  private static final String KEY_PALETTE = "palette";
  private static final String KEY_FOLLOW_SYSTEM = "follow_system";
  private static final String KEY_DARK_OVERRIDE = "dark_override";

  // ==================== 6 套主题色值（spec §6，原型确认） ====================
  private static final Theme GREEN_LIGHT = new Theme(
      0xFF1E8A4F, 0xFF35B76A, 0xFF5FDB8F, 0xFF23A55A, 0xFF2EC4B6, 0xFF3ED6C8, 0xFF45E0D4,
      0xFF0E2B1A, 0xFF3A5C49, 0x8CFFFFFF, 0xBFFFFFFF, 0x1E51BF65);
  private static final Theme GREEN_DARK = new Theme(
      0xFF0B2E1B, 0xFF14532D, 0xFF1E8A4F, 0xFF35C77B, 0xFF2BB59B, 0xFF26C4BC, 0xFF22D6CC,
      0xFFE9FFF2, 0xFF9CC9AE, 0x8A0C141A, 0x24FFFFFF, 0x2E14532D);
  private static final Theme CYAN_LIGHT = new Theme(
      0xFF0B7FA3, 0xFF12A8D1, 0xFF4FD4F2, 0xFF0FA3D8, 0xFF1FA8E8, 0xFF3D7BF0, 0xFF5A8CFF,
      0xFF062B38, 0xFF33586A, 0x8CFFFFFF, 0xBFFFFFFF, 0x1E0B7FA3);
  private static final Theme CYAN_DARK = new Theme(
      0xFF06242F, 0xFF0B3A4A, 0xFF0B7FA3, 0xFF18BCE8, 0xFF1F9FE8, 0xFF3466D6, 0xFF4A7AF0,
      0xFFE4F8FF, 0xFF8FC6D9, 0x8A08141A, 0x24FFFFFF, 0x2E0B3A4A);
  private static final Theme PINK_LIGHT = new Theme(
      0xFF8B3FBF, 0xFFB14FE0, 0xFFE27BF0, 0xFFA84FE0, 0xFFC96AF0, 0xFFF062C0, 0xFFFF7AD4,
      0xFF2A0B3D, 0xFF5C4A6E, 0x8CFFFFFF, 0xBFFFFFFF, 0x1E8B3FBF);
  private static final Theme PINK_DARK = new Theme(
      0xFF240A38, 0xFF3B1457, 0xFF8B3FBF, 0xFFC06AF0, 0xFFD070F0, 0xFFD84FA8, 0xFFF06AC0,
      0xFFF9ECFF, 0xFFC3A8D9, 0x8A12081C, 0x24FFFFFF, 0x2E3B1457);

  private static volatile ThemeManager instance;

  private final SharedPreferences sp;
  private final List<Listener> listeners = new CopyOnWriteArrayList<>();
  private Palette palette = Palette.GREEN;
  private boolean followSystem = true;
  private boolean darkOverride = false; // 手动覆盖（true=深色）
  private boolean dark = false;         // 实际生效明暗

  private ThemeManager(Context app) {
    sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    try {
      palette = Palette.valueOf(sp.getString(KEY_PALETTE, Palette.GREEN.name()));
    } catch (Exception ignored) {
      palette = Palette.GREEN;
    }
    followSystem = sp.getBoolean(KEY_FOLLOW_SYSTEM, true);
    darkOverride = sp.getBoolean(KEY_DARK_OVERRIDE, false);
    dark = followSystem ? dark : darkOverride;
  }

  public static ThemeManager init(Context app) {
    if (instance == null) {
      synchronized (ThemeManager.class) {
        if (instance == null) instance = new ThemeManager(app.getApplicationContext());
      }
    }
    return instance;
  }

  public static ThemeManager get() {
    if (instance == null) throw new IllegalStateException("ThemeManager.init() 未调用");
    return instance;
  }

  // ==================== 查询 ====================

  public Theme getTheme() {
    return theme(palette, dark);
  }

  public Palette getPalette() {
    return palette;
  }

  public boolean isDark() {
    return dark;
  }

  public boolean isFollowSystem() {
    return followSystem;
  }

  private static Theme theme(Palette p, boolean dark) {
    switch (p) {
      case CYAN: return dark ? CYAN_DARK : CYAN_LIGHT;
      case PINK: return dark ? PINK_DARK : PINK_LIGHT;
      default:   return dark ? GREEN_DARK : GREEN_LIGHT;
    }
  }

  // ==================== 修改 ====================

  /** 切换色系。 */
  public void setPalette(Palette p) {
    if (palette == p) return;
    palette = p;
    sp.edit().putString(KEY_PALETTE, p.name()).apply();
    notifyChanged();
  }

  /** 切换色系（按当前索引循环，GREEN→CYAN→PINK→GREEN）。返回新色系。 */
  public Palette cyclePalette() {
    Palette next = Palette.values()[(palette.ordinal() + 1) % Palette.values().length];
    setPalette(next);
    return next;
  }

  /** 是否跟随系统深色模式。 */
  public void setFollowSystem(boolean follow) {
    if (followSystem == follow) return;
    followSystem = follow;
    sp.edit().putBoolean(KEY_FOLLOW_SYSTEM, follow).apply();
    if (follow) {
      dark = SystemUiMode.currentDark; // 立即采用系统值
    } else {
      dark = darkOverride;
    }
    notifyChanged();
  }

  /** 手动覆盖明暗（自动关闭跟随系统）。dark=true 表示深色。 */
  public void setDarkOverride(boolean dark) {
    darkOverride = dark;
    sp.edit().putBoolean(KEY_DARK_OVERRIDE, dark).apply();
    if (!followSystem) {
      this.dark = dark;
      notifyChanged();
    }
  }

  /** 系统深色模式变化时由 Activity.onConfigurationChanged 转发。 */
  public void onSystemDarkChanged(boolean systemDark) {
    SystemUiMode.currentDark = systemDark;
    if (followSystem && dark != systemDark) {
      dark = systemDark;
      notifyChanged();
    }
  }

  // ==================== 监听 ====================

  public void addListener(Listener l) {
    if (l != null && !listeners.contains(l)) listeners.add(l);
  }

  public void removeListener(Listener l) {
    listeners.remove(l);
  }

  private void notifyChanged() {
    Theme theme = getTheme();
    for (Listener l : listeners) {
      try {
        l.onThemeChanged(theme, dark);
      } catch (Exception ignored) {
      }
    }
  }

  // ==================== 颜色工具 ====================

  /** ARGB 插值（theme 过渡用）。 */
  public static int lerpColor(int a, int b, float t) {
    int aa = Color.alpha(a), ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
    int ba = Color.alpha(b), br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
    return Color.argb(
        Math.round(aa + (ba - aa) * t),
        Math.round(ar + (br - ar) * t),
        Math.round(ag + (bg - ag) * t),
        Math.round(ab + (bb - ab) * t));
  }

  /** 当前系统深色模式缓存（Activity 创建时初始化）。 */
  public static final class SystemUiMode {
    public static boolean currentDark = false;
  }
}
