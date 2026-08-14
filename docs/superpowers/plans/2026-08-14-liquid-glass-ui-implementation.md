# 液态玻璃 UI 实现计划（Implementation Plan）

日期：2026-08-14
依据：`docs/superpowers/specs/2026-08-14-liquid-glass-ui-design.md`（已确认）
项目：AuraKernel / YeQiuLauncher（`com.aa.Aurakernel`，MainActivity.java 单文件 9218 行，纯代码构建 UI，零第三方依赖）

## 目标

在**不改变任何业务逻辑**的前提下，把全部 UI 视觉层替换为液态玻璃风格：
流体背景（LiquidBackgroundView）、LiquidTab、流体进度条（FluidProgressView）、玻璃卡片/按钮、3 色系 × 明暗主题、内置+相册壁纸、极致交互（连点搅动 / 拖动回弹 / 下拉拉扯）。

## 任务分解（按依赖顺序，TDD 优先）

### 阶段 0：代码侦察（1 个任务）
- [ ] T0 精读 MainActivity.java 关键区段，输出"UI 构建点清单"：
  - 根布局结构与层级（背景色、各页面容器）
  - CapsuleNavBar 全量逻辑（绘制、点击、长按拖拽、与 MainActivity 的接口）
  - 3 处进度条的确切位置与更新入口（2178/3386/8312/8747 行附近）
  - 所有硬编码颜色清单（MAIN_GREEN、白底、灰字等）
  - 卡片圆角背景的构建方式（GradientDrawable 工厂？）
  - AndroidManifest（主题、activity 配置）、minSdk/targetSdk、依赖文件
- 产出：`docs/superpowers/notes/ui-surface-map.md`
- 验证：清单覆盖全部 UI 构建点，无遗漏

### 阶段 1：核心库（2 个任务，无依赖）
- [ ] T1 `ui/core/FluidPhysics.java` —— 纯 Java 数学库（无 Android 依赖，可 JVM 单测）：
  - `Spring { x, v; step(target, k, c, dt) }` 阻尼弹簧
  - `EnergyPool { add(v); decay(rate); value() }` 能量注入/衰减
  - `Wave { y(x, phase) }` 正弦波、`WaveLayer`（振幅/频率/速度/相位）
  - 拖动回弹缓动函数（cubic-bezier 近似）
- [ ] T2 `ui/core/ThemeManager.java` —— 单例：
  - 6 套 token（spec §6 色值表），`Theme { w1,w2,w3,acc,text,sub,glass,border }`
  - `setPalette(色系)/setModeOverride(明暗)` 持久化 SharedPreferences
  - 注册 UiModeManager 深色广播 → 自动切明暗；手动选择覆盖自动
  - 主题变更广播（本地 Listener 集合），组件实现 `onThemeChanged(Theme)`
  - 颜色插值工具 `ArgbEvaluator` 封装（600ms 过渡由各组件自行驱动）
- 验证：T1 单测（弹簧收敛、不振荡发散）；T2 在临时 Activity 冒烟（可后置）

### 阶段 2：流体背景（2 个任务）
- [ ] T3 `ui/LiquidBackgroundView.java` —— 全屏自绘 View：
  - Canvas 3 层正弦波（振幅 26/17/8、频率 0.0085/0.014/0.022、速度递增）+ 渐变填充 + 波顶高光细线
  - 2 个光斑（径向渐变 Drawable 漂移）+ 6 个气泡上浮
  - 帧率策略：Choreographer 60fps；onWindowFocusChanged(false) 暂停；2s 静止降 30fps；连续 3 帧 > 20ms 降层数 3→2
  - `setWallpaperLayer(Bitmap)` 壁纸层（低透明度叠加在波浪之下）
  - `onThemeChanged` 颜色过渡（ArgbEvaluator 插值）
- [ ] T4 接入 MainActivity：根布局最底层挂 LiquidBackgroundView，页面容器改为透明
- 验证：真机/模拟器目视（波浪无缝流动、切后台暂停、主题切换颜色平滑过渡）

### 阶段 3：玻璃组件（2 个任务）
- [ ] T5 `ui/GlassCard.java` —— 自绘圆角玻璃容器（Layout 包装，子 View 内容不变）：
  - 玻璃底（浅色 rgba(255,255,255,.55) / 深色 rgba(12,20,26,.5)）
  - 1px 白高光描边 + 顶部内高光线 + 内侧柔和阴影（Layer 绘制）
  - 圆角 24dp；API 31+ RenderEffect 模糊 / 26–30 预模糊位图降级
  - 按压弹性：scale 0.97 → 回弹（仅可点击卡片）
- [ ] T6 `ui/GlassButton.java` —— 渐变底 + 高光 + 按压 scale 0.93 回弹（cubic-bezier(.34,1.8,.64,1)），替换现有主要按钮
- 验证：目视 + 按压手感

### 阶段 4：LiquidTabBar（1 个任务）
- [ ] T7 `ui/LiquidTabBar.java`（基于现有 CapsuleNavBar 改造，保留接口）：
  - 玻璃底栏 + 4 个 tab（图标+文字）
  - 液体指示球：弹簧 x'' = -320x - 26x'，点击注入 v += Δx×6，沿速度方向拉伸 scale(1.85, 0.15)
  - 连点能量池（封顶 1.6）叠加抖动 sin(t)×E×3.2，每帧衰减 0.018
  - 点击涟漪（圆形扩散 0.7s）
  - 保留长按拖拽切页 + 新增点击切页（现有交互不破坏）
- 验证：连点 10 次不卡顿；拖拽切页正常

### 阶段 5：流体进度条（1 个任务）
- [ ] T8 `ui/FluidProgressView.java`：
  - 玻璃凹槽 + 波浪填充（SVG/Path 两段波无缝滚动，主波 + 高光波）
  - `setProgress(float)`：显示值弹簧逼近目标值；变化瞬间流速 ×4.7 持续 0.55s 回落（涌起）
  - `setDraggable(bool)`：拖动改值，松手涌起；下载中禁拖
  - 替换 3 处：驱动下载（原 scaleX View）、按钮内 ClipDrawable、2 个系统 ProgressBar
- 验证：与真实下载进度同步（回调不动）、拖动禁用在下载中生效

### 阶段 6：壁纸（1 个任务）
- [ ] T9 `ui/core/WallpaperStore.java`：
  - 内置 3 张壁纸（渐变/风景，打包 drawable 或 assets）
  - `ActivityResultContracts.PickVisualMedia` 相册选择（免权限）
  - 降采样 + 高斯模糊生成壁纸层，缓存本地；失败回退内置 + Toast
  - 接入 LiquidBackgroundView.setWallpaperLayer
- 验证：内置切换、相册选择、失败回退（可测：选损坏图片）

### 阶段 7：MainActivity 集成（2 个任务）
- [ ] T10 视觉层替换（按 T0 清单逐点）：
  - 根布局挂背景、页面容器透明
  - 卡片 → GlassCard、按钮 → GlassButton、导航 → LiquidTabBar、进度 → FluidProgressView
  - 全部硬编码颜色 → ThemeManager 取色
  - 页面切换改液态过渡（淡入+上移 10px，380ms）；长按拖拽切页保留
- [ ] T11 "我的"页面新增设置区：主题色系切换（绿/青蓝/紫粉）、明暗手动覆盖、壁纸选择入口、当前主题预览
- 验证：6 套主题下所有页面颜色一致；功能全量冒烟

### 阶段 8：极致交互（1 个任务）
- [ ] T12 下拉拉扯感：内容区 overscroll 时背景波浪被向下拉伸（波浪基线上移 + 振幅增大），松手弹簧回弹
- 验证：各页面下拉手感统一、不干扰正常滚动

### 阶段 9：性能与回归（2 个任务）
- [ ] T13 性能收尾：帧率自适应验证（低端机降层）、RenderEffect 降级路径在 API 26 模拟器验证、内存泄漏检查（切后台/切主题循环 20 次）
- [ ] T14 全量回归（spec §10 测试计划 6 项）+ 修复

## 提交策略

每个任务完成后单独 commit（`feat(liquid): <task>`），保持主分支随时可构建。

## 里程碑

- M1（T0–T6）：核心库 + 背景 + 玻璃组件就位，主界面初具液态玻璃观感
- M2（T7–T8）：LiquidTab + 流体进度条，交互核心完成
- M3（T9–T11）：壁纸 + 设置入口，功能完整
- M4（T12–T14）：极致交互 + 性能 + 回归，验收

## 风险与预案

| 风险 | 预案 |
|---|---|
| 9218 行单文件改造冲突 | 严格按 T0 清单逐点替换；每任务后构建验证 |
| 低版本模糊观感差异 | 预模糊位图层方案（spec §8），验收时 API 26 模拟器对比 |
| 动画耗电/发热 | 帧率自适应 + 失焦暂停，真机验证 |
| 长按拖拽与点击手势冲突 | LiquidTabBar 手势仲裁（down 移动阈值区分点击/拖拽） |
