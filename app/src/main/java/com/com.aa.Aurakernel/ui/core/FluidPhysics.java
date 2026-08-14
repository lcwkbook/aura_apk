package com.aa.Aurakernel.ui.core;

/**
 * 流体物理数学库（纯 Java，无 Android 依赖，可独立验证）。
 * 提供液态 UI 所需的：阻尼弹簧、能量池（连点搅动）、正弦波浪、回弹缓动。
 */
public final class FluidPhysics {

  private FluidPhysics() {}

  /**
   * 阻尼弹簧：x'' = -k*(x - target) - c*x'
   * k=320, c=26 时欠阻尼 → 带 2~3 次自然回弹（液态手感）。
   * dt 单位为秒。
   */
  public static final class Spring {
    public float x;
    public float v;

    public Spring(float initial) {
      x = initial;
      v = 0f;
    }

    /** 推进一帧；返回当前位置（便于链式调用）。 */
    public float step(float target, float k, float c, float dt) {
      v += (-k * (x - target) - c * v) * dt;
      x += v * dt;
      return x;
    }

    /** 注入初速（点击甩动：v += Δx * 6）。 */
    public void impulse(float dv) {
      v += dv;
    }

    /** 是否已基本静止（|v| 与 |x-target| 都小于阈值）。 */
    public boolean settled(float target, float eps) {
      return Math.abs(x - target) < eps && Math.abs(v) < eps * 20f;
    }

    public void snap(float value) {
      x = value;
      v = 0f;
    }
  }

  /**
   * 能量池：一次性注入（连点能量），随时间线性衰减。
   * 典型用法：连点 Tab 时 add(0.35)，抖动幅度 = sin(t*freq) * energy * 3.2。
   */
  public static final class EnergyPool {
    private float value = 0f;
    private final float decayPerSec;
    private final float cap;

    public EnergyPool(float decayPerSec, float cap) {
      this.decayPerSec = decayPerSec;
      this.cap = cap;
    }

    public void add(float v) {
      value = Math.min(cap, value + v);
    }

    public void step(float dt) {
      value = Math.max(0f, value - decayPerSec * dt);
    }

    public float value() {
      return value;
    }

    public void reset() {
      value = 0f;
    }
  }

  /** 正弦波：y(x, t) = amp * sin(x*freq + t*speed + phase)。相位随时间推进即产生无缝流动。 */
  public static final class Wave {
    public float amp;
    public float freq;
    public float speed;
    public float phase;

    public Wave(float amp, float freq, float speed, float phase) {
      this.amp = amp;
      this.freq = freq;
      this.speed = speed;
      this.phase = phase;
    }

    public float y(float x, float t) {
      return (float) (amp * Math.sin(x * freq + t * speed + phase));
    }
  }

  /** 回弹缓动（近似原型 cubic-bezier(.34, 1.56, .64, 1)）：t ∈ [0,1] → 先冲过头再回落。 */
  public static float overshoot(float t) {
    if (t <= 0f) return 0f;
    if (t >= 1f) return 1f;
    final float s = 1.70158f;
    t -= 1f;
    return t * t * ((s + 1f) * t + s) + 1f;
  }

  /** 线性插值（float）。 */
  public static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  /** clamp */
  public static float clamp(float v, float min, float max) {
    return v < min ? min : (v > max ? max : v);
  }
}
