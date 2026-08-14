# UI 构建点清单（T0 侦察报告）

日期：2026-08-14
范围：MainActivity.java（9218 行）、CapsuleNavBar.java、AndroidManifest.xml、build.gradle、res/styles.xml

## 1. 构建配置

| 项 | 值 | 影响 |
|---|---|---|
| minSdk / targetSdk / compileSdk | 26 / 36 / 34 | RenderEffect（API 31+）需降级方案；Photo Picker 可用 |
| Java | 1.8 | 无 lambda 限制（现有代码已用 lambda） |
| 依赖 | androidx.core 1.13.1、bcprov、hutool | 零 UI 依赖，维持不变 |
| 主题 | `Theme.Material.Light.NoActionBar`（values + values-v21） | 无 values-night；深色由代码 nightMode 控制 |
| 签名 | my-release-key.jks（local.properties 读密码） | 改代码不影响签名；release 开启 minify |
| 权限 | 存储/定位/安装包/网络 | 相册选图走 Photo Picker，**无需新权限** |

## 2. 总体结构

```
onCreate → 启动校验（签名×3、卡密、权限、Root）→ showMainShell()
showMainShell()（1593）: LinearLayout shell
  ├─ pageHost（weight=1，页面容器，背景 bgColor()）
  └─ createBottomNav()（2230，高 64dp + padding）
switchPage(page)（2438）: removeAllViews → createXxxPage() → AlphaAnimation 淡入 → updateNavCapsule
⚠️ 主题切换 toggleTheme()（7890）= nightMode 取反 + 保存 + showMainShell() 整体重建
```

## 3. UI 构建点清单

### 3.1 导航（createBottomNav，2230–2408）★ 核心改造
- `navBarFrame` FrameLayout：圆角 30dp 玻璃底 `rgba(255,255,255,200)`/`rgba(20,24,36,200)` + borderColor 描边 + elevation
- `capsule` View：左→右渐变绿 `MAIN_GREEN→rgb(99,209,119)`，圆角 26dp，alpha 0.22，初始宽度=tabW-12dp
- 4 个 `buildNavTab(emoji,label,active)`：图标+文字，选中绿/未选 subTextColor
- 手势：**长按 200ms 进入拖拽模式**（震动20ms、capsule alpha 0.45）→ 拖动实时切页（switchPageContentOnly + updateNavTabPreview）；短按 performClick → switchPage(i)
- `updateNavCapsule(targetIndex)`：ValueAnimator 350ms 位置+宽度滑动
- `updateSingleTab`：选中图标 0.7→1 Overshoot 弹入
- **替换为 LiquidTabBar**（保留：4 tab 结构、onSelect 接口、长按拖拽、悬停预览；新增：液体球弹簧、连点搅动、涟漪）

### 3.2 页面容器与背景
- 每页：`rootScroll.setBackgroundColor(bgColor())` + `page.setBackgroundColor(bgColor())` + `pageHost.setBackgroundColor(bgColor())`（switchPage 内）
- **替换**：全部置透明 → LiquidBackgroundView 挂在 shell 最底层（pageHost 之后 addView index 0）

### 3.3 进度条（5 处）
| # | 位置 | 现状 | 替换 |
|---|---|---|---|
| P1 | 驱动页 3316/3573 | View + `setScaleX(overall/100f)`，DownloadDriverTask.onProgressUpdate 驱动 | FluidProgressView（主案例，下载中禁拖） |
| P2 | updateDownloadProgress 2185 | 按钮内 ClipDrawable+LayerDrawable（prepareScriptIfNeeded 调用，主页运行按钮） | 按钮下方内联 FluidProgressView（下载时 visible） |
| P3 | 公告弹窗 countPb 8311 | 系统 ProgressBar + ClipDrawable + ValueAnimator 递减 | FluidProgressView（倒计时递减 + 波浪常流） |
| P4 | 更新弹窗 progressBar 8746 | 系统 ProgressBar + ColorFilter（更新 APK 用） | FluidProgressView |
| P5 | 启动页加载条 1421 | View + scaleX（启动动画，非进度） | 保留（splash 简化，可选流体化） |

### 3.4 卡片背景（玻璃化统一入口）
- `createCardBackground()`（6594）：TOP_BOTTOM 渐变 cardColor→微差 + 圆角 22dp + borderColor 描边 ← **所有 createGlassCard 卡片**
- `createGlassCard(title)`（6583）：卡片工厂（accent 竖条 + 标题）→ **改造为真玻璃（半透明+高光+blur）**
- 主页内联卡片：header（rgba(255,255,255,180) 圆角28 + 绿描边 argb(60,81,191,101)）、公告卡片（绿渐变 argb(20/5) 圆角24）、chip（argb(30,81,191,101) 圆角12）
- 我的页 headerArea（argb(18,81,191,101) 渐变圆角24）
- 弹窗卡片：公告弹窗（深色/浅色圆角24）、更新弹窗（圆角20）等

### 3.5 颜色体系（集中区 9052–9095）
- `MAIN_GREEN=rgb(81,191,101)`、`LIGHT_GREEN`、`TRACK_BG`、`DARK_TEXT`/`GRAY_TEXT`（常量区 160–190 附近）
- 方法：bgColor/cardColor/textColor/subTextColor/borderColor/primaryColor/tagColor/terminalBgColor/success*/danger*/disabledColor —— 全部 nightMode 三元
- **替换**：全部收敛到 ThemeManager（3 色系 × 明暗），nightMode 保留为"手动覆盖"，新增"跟随系统"默认值

### 3.6 按钮
- `button(text, primary)`（9003）：round(primaryColor,14) 圆角实心 → GlassButton（渐变+高光+按压弹性）
- `createModernButton(text, bgColor)`（4448）：驱动页/其他
- `switchButton(on)`（3115）：内核配置开关（保持，仅换色）
- updateDownloadProgress 中按钮背景动态替换

### 3.7 页面切换
- switchPage/switchPageContentOnly：AlphaAnimation 0.3→1（300ms/200ms）→ 液态过渡（淡入+上移 10px 380ms）
- dispatchTouchEvent（9100+）：左右滑动切页（阈值 100dp；导航栏/横向滚动区起点豁免）→ 保留
- animatePageIn（2549）：备用方向滑入

### 3.8 我的页面（createMinePage 6149）
- headerArea + 卡密卡片 + 设备信息卡片 + 清理工具卡片 + **createThemeToggleRow（6360，主题模式行）** + 后续行（6329–6520 未全读，实现时补读）
- **新增设置区（T11）**：色系切换（绿/青蓝/紫粉）、明暗跟随/手动、壁纸选择、主题预览

### 3.9 其他
- CapsuleNavBar.java：**遗留未使用**（2-tab 旧组件，MainActivity 未引用）→ 不动，LiquidTabBar 新建
- splash：纯白/渐变启动页 + 加载条 → 背景可换液态（低优先级）
- 弹窗：公告弹窗（倒计时+进度）、更新弹窗（下载 APK）、确认弹窗 → 玻璃化 + P3/P4

## 4. 改造映射表（T10 执行清单）

| 现有 | 新组件 | 关键保留 |
|---|---|---|
| createBottomNav + capsule + tab 手势 | LiquidTabBar | 长按拖拽切页、悬停预览、onSelect |
| pageHost/各页 setBackgroundColor(bgColor) | 透明 + LiquidBackgroundView | — |
| createCardBackground / createGlassCard | GlassCard（真玻璃） | 卡片子内容构建不动 |
| P1–P4 进度 | FluidProgressView | DownloadDriverTask 回调、ValueAnimator 递减逻辑 |
| button/createModernButton | GlassButton | 文本/点击事件 |
| 颜色方法族 | ThemeManager.get() | nightMode 语义保留为 override |
| switchPage 淡入 | 液态过渡（淡入+上移） | switchPageContentOnly 拖拽路径 |

## 5. 风险点

1. **主题切换整体重建**（showMainShell）：新 LiquidBackgroundView 挂 shell 底层会被重建——需在重建时保留背景实例（背景提升为 Activity 级字段，重建 shell 时 detach 后复用）→ 主题切换才有平滑过渡
2. **minify 开启**：新组件类需确认混淆规则（现有未配置 keep，自绘 View 无反射一般安全）
3. **下载中禁拖**：FluidProgressView 需 draggable=false 状态，DownloadDriverTask 期间锁定
4. **dispatchTouchEvent 滑动切页**与 LiquidTabBar 手势并存：拖拽模式起点在导航栏时豁免逻辑已存在（isTouchOnNavBar），LiquidTabBar 内部手势不冲突
5. **.gitignore 未忽略 .superpowers**：视觉伴侣会话目录会被 git 跟踪，需加入忽略
