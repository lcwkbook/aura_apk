package com.Aurakernel;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.json.JSONObject;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class MainActivity extends Activity {
  private boolean[] isRunning = new boolean[1];
  private static final String REMOTE_SCRIPT_URL = "https://aura.xiaon.sbs/update/Aurakernel.sh";
  private static final String REMOTE_SCRIPT_NAME = "Aurakernel.sh";
  private static final String SCRIPT_NAME = "Aurakernel.sh";
  private int driverType = 0;
  private boolean antiRecord = false; // 防录屏，默认关闭
  private boolean noBackground = false; // 无后台，默认关闭
  private boolean scriptReady = false;
  private static final int REQUEST_MANAGE_STORAGE = 9999;
  private static final int REQ_STORAGE = 3001;
  private static final String PREFS = "app_prefs";
  private static final String PREF_FAV_DIRS = "favorite_dirs";
  private static final String PREF_LAST_RUN_DIR = "last_run_dir";
  private static final String PREF_LAST_DRIVER_DIR = "last_driver_dir";

  // 纯白启动页背景
  final int SPLASH_WHITE_BG = Color.WHITE;
  // 主淡绿色（进度条、圆环主弧线、核心元素）
  final int MAIN_GREEN = Color.rgb(81, 191, 101);
  // 浅绿（副标题、辅助文字）
  final int LIGHT_GREEN = Color.rgb(129, 199, 132);
  // 深灰主文字（白底标题专用）
  final int DARK_TEXT = Color.rgb(33, 33, 33);
  // 浅灰辅助文字
  final int GRAY_TEXT = Color.rgb(97, 97, 97);
  // 轨道浅灰色（进度条底色）
  final int TRACK_BG = Color.argb(40, 160, 160, 160);
  // 绿色光晕
  final int GREEN_GLOW = Color.argb(60, 81, 191, 101);

  private UpdateTask updateTask;
  private FrameLayout root;
  private LinearLayout pageHost;
  private TextView navHome;
  private TextView navMine;
  private TextView driverBtnKpm, driverBtnParadise, driverBtnBackup;
  private TextView antiRecordBtn, noBackgroundBtn;

  private TextView outputView;
  private ScrollView terminalScroll;
  private Button runButton;
  private Button stopButton;
  private EditText keyEdit;

  private File selectedFile;
  private String selectedName = "";
  private boolean running = false;
  private boolean nightMode = false;
  private int currentPage = 0;

  private Process runningProcess;
  private BufferedWriter processWriter;
  private String activeRootPath = "";

  private final Handler handler = new Handler();
  private final StringBuilder outputBuffer = new StringBuilder("就绪\n");
  private boolean rootDenied = false;

  // ====================== 驱动模块 常量 & 全局控件 ======================
  // 驱动ZIP下载地址
  private static final String DRIVER_ZIP_URL = "https://aura.xiaon.sbs/update/驱动.zip";
  // 应用私有files目录下 驱动根目录
  private File driverRootDir;
  // 驱动ZIP临时文件
  private File driverZipFile;

  // 底部导航新增驱动按钮
  private TextView navDriver;
  // 驱动下载任务
  private DownloadDriverTask downloadTask;

  // 驱动页面控件
  private TextView driverPageStatus; // 驱动页面状态提示
  private LinearLayout driverFileListLayout; // 统一文件列表容器
  private File currentBrowseDir; // 当前浏览目录（固定根目录，不再跳转）

  // ========== 驱动文件夹展开优化 ==========
  private Set<File> expandedDirs;
  // 手风琴：记录当前唯一展开的文件夹（解决闪烁核心）
  private File currentExpandedFolder = null;
  // 防止重复点击
  private boolean isAnimating = false;

  private interface FilePickCallback {
    void onPicked(File file);
  }

  private static final class BrowserState {
    File currentDir;
    boolean zipOnly;
    String title;
    String lastDirPref;
    Dialog dialog;
    FilePickCallback callback;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    try {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } catch (Exception ignored) {
    }

    // ========== 新增：初始化驱动私有目录（files目录） ==========
    driverRootDir = new File(getFilesDir(), "drivers");
    driverZipFile = new File(getFilesDir(), "驱动.zip");
    if (!driverRootDir.exists()) {
      driverRootDir.mkdirs();
    }
    // 初始化文件夹展开集合
    expandedDirs = new HashSet<>();
    // ========================================================

    SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
    nightMode = sp.getBoolean("night_mode", false);
    driverType = sp.getInt("driver_type", 0); // 新增
    antiRecord = sp.getBoolean("anti_record", false); // 新增
    noBackground = sp.getBoolean("no_background", false); // 新增
    setupWindow();
    root = new FrameLayout(this);

    // 检查权限，未授权则弹窗，授权后继续
    if (!hasStoragePermission()) {
      requestStoragePermission();
      // 此时 setContentView 先给一个空白等待页面，也可以直接 setContentView(root) 并显示提示
      setContentView(root);
      showPermissionGuide(); // 提示用户授权，或直接跳转设置
      return;
    }

    setContentView(root);
    // 检查 Root 权限，通过后再进入主界面
    checkRootAndProceed();
  }

  private void checkRootAndProceed() {
    // 显示检查提示
    root.removeAllViews();
    TextView tip = new TextView(this);
    tip.setText("正在检查 Root 权限...");
    tip.setTextSize(18);
    tip.setTextColor(textColor());
    tip.setGravity(Gravity.CENTER);
    root.addView(tip, new FrameLayout.LayoutParams(-1, -1));

    new Thread(
            () -> {
              boolean hasRoot = RunnerSupport.hasRoot();
              handler.post(
                  () -> {
                    if (hasRoot) {
                      // 有 Root，正常进入
                      showSplashThenMain();
                    } else {
                      // 无 Root，停留在当前提示界面
                      rootDenied = true;
                      tip.setText("未获取 Root 权限，无法使用该软件");
                      tip.setTextColor(Color.RED);
                      root.setOnClickListener(null);
                      Toast.makeText(MainActivity.this, "未获取 Root 权限", Toast.LENGTH_LONG).show();
                    }
                  });
            })
        .start();
  }

  @Override
  public void onBackPressed() {
    if (rootDenied) {
      finish(); // 允许退出
    } else {
      super.onBackPressed();
    }
  }

  private boolean hasStoragePermission() {
    if (Build.VERSION.SDK_INT >= 30) {
      return Environment.isExternalStorageManager();
    } else if (Build.VERSION.SDK_INT >= 23) {
      return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED
          && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED;
    }
    return true;
  }

  private void requestStoragePermission() {
    if (Build.VERSION.SDK_INT >= 30) {
      Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
      intent.setData(Uri.parse("package:" + getPackageName()));
      startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
    } else if (Build.VERSION.SDK_INT >= 23) {
      requestPermissions(
          new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
          },
          REQUEST_MANAGE_STORAGE);
    }
  }

  // 显示一个简单的“请授予权限”界面（可自定义）
  private void showPermissionGuide() {
    root.removeAllViews();
    TextView tip = new TextView(this);
    tip.setText("请授予“所有文件访问权限”以继续使用");
    tip.setTextSize(18);
    tip.setTextColor(textColor());
    tip.setGravity(Gravity.CENTER);
    root.addView(tip, new FrameLayout.LayoutParams(-1, -1));
    root.setOnClickListener(v -> requestStoragePermission());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_MANAGE_STORAGE) {
      if (hasStoragePermission()) {
        // 已授权，重新初始化
        recreate();
      } else {
        Toast.makeText(this, "必须授予权限才能使用", Toast.LENGTH_LONG).show();
        finish();
      }
    } else if (requestCode == 1001) {
      // 用户从安装未知源设置返回，不自动重试，需手动再次点击检测更新
      Toast.makeText(this, "已允许安装，请再次点击检测更新", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_MANAGE_STORAGE) {
      if (hasStoragePermission()) {
        recreate();
      } else {
        Toast.makeText(this, "必须授予权限才能使用", Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }

  @Override
  protected void onDestroy() {
    if (updateTask != null && !updateTask.isCancelled()) {
      updateTask.cancel(true);
    }
    // 新增：取消驱动下载任务
    if (downloadTask != null && !downloadTask.isCancelled()) {
      downloadTask.cancel(true);
    }
    stopRunningProcess(false);
    handler.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  private void setupWindow() {
    try {
      Window w = getWindow();
      w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
      if (Build.VERSION.SDK_INT >= 21) {
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(bgColor());
        w.setNavigationBarColor(bgColor());
      }
      int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      if (!nightMode && Build.VERSION.SDK_INT >= 23) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      if (!nightMode && Build.VERSION.SDK_INT >= 26)
        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      w.getDecorView().setSystemUiVisibility(flags);
      w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor()));
    } catch (Exception ignored) {
    }
  }

  // ====================== 重写启动动画（白色背景 + 淡绿色元素） ======================
  private void showSplashThenMain() {
    root.removeAllViews();

    // === 改为 纯白色背景（替换原来深色渐变） ===
    root.setBackgroundColor(SPLASH_WHITE_BG);

    FrameLayout splashRoot = new FrameLayout(this);
    splashRoot.setPadding(dp(30), dp(30), dp(30), dp(30));
    root.addView(splashRoot, new FrameLayout.LayoutParams(-1, -1));

    // ===== 1. 光环 Logo（淡绿色主题） =====
    RingView ringView = new RingView(this);
    ringView.setAlpha(0f);
    FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(dp(110), dp(110));
    ringLp.gravity = Gravity.CENTER;
    ringLp.topMargin = -dp(50);
    splashRoot.addView(ringView, ringLp);

    // ===== 2. 品牌名（深灰色文字，白底清晰） =====
    TextView nameView = text("AuraKernel", 34, DARK_TEXT, Typeface.BOLD);
    nameView.setAlpha(0f);
    nameView.setTranslationY(dp(24));
    nameView.setGravity(Gravity.CENTER);
    // 文字阴影（白底弱化阴影）
    nameView.setShadowLayer(dp(4), 0, dp(2), Color.argb(40, 0, 0, 0));
    FrameLayout.LayoutParams nameLp = new FrameLayout.LayoutParams(-1, -2);
    nameLp.gravity = Gravity.CENTER;
    nameLp.topMargin = dp(76);
    splashRoot.addView(nameView, nameLp);

    // ===== 3. 副标题（淡绿色文字） =====
    TextView subView =
        text("Binary Runner · Driver Mode · Live Terminal", 12, LIGHT_GREEN, Typeface.NORMAL);
    subView.setAlpha(0f);
    subView.setGravity(Gravity.CENTER);
    FrameLayout.LayoutParams subLp = new FrameLayout.LayoutParams(-1, -2);
    subLp.gravity = Gravity.CENTER;
    subLp.topMargin = dp(112);
    splashRoot.addView(subView, subLp);

    // ===== 4. 加载指示条（浅灰轨道 + 淡绿色进度条） =====
    final View track = new View(this);
    track.setBackground(round(TRACK_BG, 3, 0, 0));
    FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(dp(200), dp(5));
    trackLp.gravity = Gravity.CENTER;
    trackLp.topMargin = dp(180);
    splashRoot.addView(track, trackLp);

    final View bar = new View(this);
    bar.setBackground(round(MAIN_GREEN, 3, 0, 0)); // 淡绿色进度条
    FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(dp(200), dp(5));
    barLp.gravity = Gravity.CENTER;
    barLp.topMargin = dp(180);
    splashRoot.addView(bar, barLp);
    // 缩放支点：从左向右展开
    bar.setScaleX(0f);
    bar.setPivotX(0f);

    // ===== 加载条动画 =====
    final ValueAnimator barAnim = ValueAnimator.ofFloat(0f, 1f);
    barAnim.setDuration(800);
    barAnim.setStartDelay(500);
    barAnim.setInterpolator(new DecelerateInterpolator(2.0f));
    barAnim.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          public void onAnimationUpdate(ValueAnimator animation) {
            float value = (Float) animation.getAnimatedValue();
            bar.setScaleX(value);
          }
        });

    // ===== 动画序列（完全保留原有动画逻辑） =====
    // 1. 光环淡入 + 旋转
    ringView
        .animate()
        .alpha(1f)
        .setDuration(700)
        .setInterpolator(new DecelerateInterpolator(1.5f))
        .start();

    final ValueAnimator rotateAnim = ValueAnimator.ofFloat(0f, 360f);
    rotateAnim.setDuration(2500);
    rotateAnim.setRepeatCount(ValueAnimator.INFINITE);
    rotateAnim.setInterpolator(new android.view.animation.LinearInterpolator());
    rotateAnim.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          public void onAnimationUpdate(ValueAnimator animation) {
            ringView.setRotation((Float) animation.getAnimatedValue());
          }
        });
    rotateAnim.setStartDelay(200);
    rotateAnim.start();

    // 2. 品牌名弹性弹入
    nameView
        .animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(400)
        .setStartDelay(200)
        .setInterpolator(new android.view.animation.OvershootInterpolator(1.3f))
        .start();

    // 3. 副标题淡入
    subView
        .animate()
        .alpha(1f)
        .setDuration(350)
        .setStartDelay(500)
        .setInterpolator(new DecelerateInterpolator())
        .start();

    // 4. 加载条结束，跳转主界面
    barAnim.addListener(
        new android.animation.AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(android.animation.Animator animation) {
            handler.postDelayed(
                new Runnable() {
                  public void run() {
                    rotateAnim.cancel();
                    // 整体淡出
                    splashRoot
                        .animate()
                        .alpha(0f)
                        .setDuration(300)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(
                            new Runnable() {
                              @Override
                              public void run() {
                                showMainShell();
                              }
                            })
                        .start();
                  }
                },
                200);
          }
        });
    barAnim.start();
  }

  // ====================== RingView 内部类（纯白背景 + 淡绿色元素） ======================
  private class RingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF(); // 修复：添加 () 括号

    public RingView(Context context) {
      super(context);
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float w = getWidth();
      float h = getHeight();
      float cx = w / 2f;
      float cy = h / 2f;
      float radius = Math.min(w, h) / 2f - dp(6);

      // === 外环（半透明白色轮廓，适配白底） ===
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(3));
      paint.setColor(Color.argb(50, 120, 120, 120));
      paint.setStrokeCap(Paint.Cap.ROUND);
      arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
      canvas.drawArc(arcRect, -90, 360, false, paint);

      // === 主弧线：淡绿色（替换原蓝色） ===
      paint.setStrokeWidth(dp(4));
      paint.setColor(MAIN_GREEN);
      float startAngle = -90 + (System.currentTimeMillis() % 2500) / 2500f * 360f;
      float sweepAngle = 150f;
      arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius);
      canvas.drawArc(arcRect, startAngle, sweepAngle, false, paint);

      // === 内层高光弧（浅白提亮） ===
      paint.setStrokeWidth(dp(2));
      paint.setColor(Color.argb(200, 255, 255, 255));
      canvas.drawArc(arcRect, startAngle + 15, sweepAngle - 30, false, paint);

      // === 前端高亮光点 + 绿色光晕 ===
      float dotAngle = (float) Math.toRadians(startAngle + sweepAngle);
      float dotX = cx + radius * (float) Math.cos(dotAngle);
      float dotY = cy + radius * (float) Math.sin(dotAngle);

      paint.setStyle(Paint.Style.FILL);
      // 外层绿色光晕
      paint.setColor(GREEN_GLOW);
      canvas.drawCircle(dotX, dotY, dp(8), paint);
      // 中层亮圈
      paint.setColor(Color.argb(120, 255, 255, 255));
      canvas.drawCircle(dotX, dotY, dp(4), paint);
      // 核心白点
      paint.setColor(Color.WHITE);
      canvas.drawCircle(dotX, dotY, dp(3), paint);

      // === 尾端淡绿色小点 ===
      float tailAngle = (float) Math.toRadians(startAngle + 20);
      float tailX = cx + radius * (float) Math.cos(tailAngle);
      float tailY = cy + radius * (float) Math.sin(tailAngle);
      paint.setColor(Color.argb(80, 81, 191, 101));
      canvas.drawCircle(tailX, tailY, dp(3), paint);

      // === 中心绿色发光点 ===
      paint.setColor(Color.argb(30, 81, 191, 101));
      canvas.drawCircle(cx, cy, dp(12), paint);
      paint.setColor(Color.argb(60, 255, 255, 255));
      canvas.drawCircle(cx, cy, dp(5), paint);
      paint.setColor(Color.argb(120, 255, 255, 255));
      canvas.drawCircle(cx, cy, dp(2), paint);
    }
  }

  private GradientDrawable bgGradient() {
    GradientDrawable g =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            nightMode
                ? new int[] {Color.rgb(8, 12, 22), Color.rgb(17, 26, 49), Color.rgb(7, 10, 18)}
                : new int[] {
                  Color.rgb(238, 245, 255), Color.rgb(250, 252, 255), Color.rgb(235, 244, 255)
                });
    return g;
  }

  private void showMainShell() {
    root.removeAllViews();
    root.setBackgroundColor(bgColor());

    LinearLayout shell = new LinearLayout(this);
    shell.setOrientation(LinearLayout.VERTICAL);
    shell.setBackgroundColor(bgColor());
    root.addView(shell, new FrameLayout.LayoutParams(-1, -1));

    pageHost = new LinearLayout(this);
    pageHost.setOrientation(LinearLayout.VERTICAL);
    shell.addView(pageHost, new LinearLayout.LayoutParams(-1, 0, 1));

    shell.addView(createBottomNav(), new LinearLayout.LayoutParams(-1, dp(76)));
    switchPage(currentPage);
    prepareScriptIfNeeded(); // 自动检查并下载脚本

    // ============ 【新增】进入软件后自动检查更新 ============
    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            // 静默检查更新，只有发现新版本时才弹窗提示
            if (isNetworkAvailable()) {
              if (updateTask != null && !updateTask.isCancelled()) {
                updateTask.cancel(true);
              }
              updateTask = new UpdateTask(MainActivity.this);
              updateTask.execute();
            }
            // 无网络时静默跳过，不弹任何提示
          }
        },
        1500); // 延迟1.5秒等页面完全加载后再检查
    // ======================================================
  }

  private void prepareScriptIfNeeded() {
  if (scriptReady) return;

  // 检查本地文件
  File scriptDir = new File(getFilesDir(), "scripts");
  File scriptFile = new File(scriptDir, SCRIPT_NAME);
  if (scriptFile.exists()) {
    selectedFile = scriptFile;
    selectedName = SCRIPT_NAME;
    scriptReady = true;
    updateRunButton();
    append("脚本已就绪\n");
    return;
  }

  // 需要下载
  append("正在下载运行脚本...\n");
  // 下载开始：禁用按钮 + 初始化进度样式
  if (runButton != null) {
    runButton.setEnabled(false);
    runButton.setTag(round(primaryColor(), 14, 0, 0)); // 保存原始背景
    updateDownloadProgress(runButton, 0); // 初始0%进度
  }

  new Thread(
          () -> {
            try {
              if (!scriptDir.exists()) scriptDir.mkdirs();
              File tempFile = new File(scriptDir, SCRIPT_NAME + ".tmp");

              URL url = new URL("https://aura.xiaon.sbs/update/Aurakernel.sh");
              HttpURLConnection conn = (HttpURLConnection) url.openConnection();
              conn.setConnectTimeout(15000);
              conn.setReadTimeout(30000);

              // HTTPS 证书忽略
              if (conn instanceof HttpsURLConnection) {
                HttpsURLConnection sconn = (HttpsURLConnection) conn;
                TrustManager[] trustAll =
                    new TrustManager[] {
                      new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                          return new X509Certificate[0];
                        }
                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}
                      }
                    };
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAll, new SecureRandom());
                sconn.setSSLSocketFactory(sc.getSocketFactory());
                sconn.setHostnameVerifier((hostname, session) -> true);
              }

              int totalLength = conn.getContentLength();
              InputStream in = new BufferedInputStream(conn.getInputStream());
              FileOutputStream out = new FileOutputStream(tempFile);
              byte[] buf = new byte[8192];
              int len;
              long downloadedSize = 0;

              while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                downloadedSize += len;
                int progress = (int) ((downloadedSize * 100) / totalLength);
                
                // 主线程更新进度
                handler.post(() -> {
                  if (runButton != null) updateDownloadProgress(runButton, progress);
                });
              }

              out.flush();
              out.close();
              in.close();
              conn.disconnect();

              tempFile.renameTo(scriptFile);
              RunnerSupport.chmod777(scriptFile);

              handler.post(
                  () -> {
                    selectedFile = scriptFile;
                    selectedName = SCRIPT_NAME;
                    scriptReady = true;
                    append("运行脚本准备完毕\n");
                    // 恢复按钮原始样式
                    if (runButton != null) {
                      runButton.setBackground((GradientDrawable) runButton.getTag());
                    }
                    updateRunButton();
                  });
            } catch (Exception e) {
              handler.post(
                  () -> {
                    append("脚本下载失败: " + e.getMessage() + "\n");
                    // 恢复按钮原始样式
                    if (runButton != null) {
                      runButton.setBackground((GradientDrawable) runButton.getTag());
                    }
                    updateRunButton();
                  });
            }
          })
      .start();
}

/**
 * 更新按钮下载进度（左绿右灰，圆角）
 * @param btn 目标按钮
 * @param progress 进度 0-100
 */
private void updateDownloadProgress(Button btn, int progress) {
    if (btn == null) return;
    btn.post(() -> {
        // 底层灰色圆角背景
        GradientDrawable grayBg = new GradientDrawable();
        grayBg.setColor(disabledColor());
        grayBg.setCornerRadius(dp(14));

        // 上层绿色进度背景
        GradientDrawable greenBg = new GradientDrawable();
        greenBg.setColor(MAIN_GREEN);
        greenBg.setCornerRadius(dp(14));

        // ClipDrawable 控制绿色区域的水平裁剪（从左到右）
        ClipDrawable clip = new ClipDrawable(greenBg, Gravity.LEFT, ClipDrawable.HORIZONTAL);
        clip.setLevel(progress * 100); // level 范围 0～10000

        // 叠加图层
        LayerDrawable layer = new LayerDrawable(new Drawable[]{grayBg, clip});
        layer.setLayerInset(1, 0, 0, 0, 0); // 让绿色层填满按钮

        btn.setBackground(layer);
        btn.setText("加载文件脚本中 " + progress + "%");
        btn.setTextColor(Color.WHITE);
    });
}

  private View createBottomNav() {
    LinearLayout wrap = new LinearLayout(this);
    wrap.setGravity(Gravity.CENTER);
    wrap.setPadding(dp(20), dp(8), dp(20), dp(14));
    wrap.setBackgroundColor(bgColor());

    LinearLayout bar = new LinearLayout(this);
    bar.setOrientation(LinearLayout.HORIZONTAL);
    bar.setGravity(Gravity.CENTER);
    bar.setPadding(dp(6), dp(6), dp(6), dp(6));
    bar.setBackground(round(cardColor(), 28, borderColor(), 1));
    // 加宽适配3个按钮
    wrap.addView(bar, new LinearLayout.LayoutParams(dp(300), dp(56)));

    // 三个导航按钮：主页、驱动、我的
    navHome = navButton("主页", true);
    navDriver = navButton("驱动", false);
    navMine = navButton("我的", false);

    bar.addView(navHome, new LinearLayout.LayoutParams(0, -1, 1));
    bar.addView(navDriver, new LinearLayout.LayoutParams(0, -1, 1));
    bar.addView(navMine, new LinearLayout.LayoutParams(0, -1, 1));

    // 点击事件
    navHome.setOnClickListener(v -> switchPage(0));
    navDriver.setOnClickListener(v -> switchPage(1));
    navMine.setOnClickListener(v -> switchPage(2));
    return wrap;
  }

  private TextView navButton(String text, boolean active) {
    TextView v = text(text, 14, active ? Color.WHITE : subTextColor(), Typeface.BOLD);
    v.setGravity(Gravity.CENTER);
    v.setBackground(round(active ? primaryColor() : Color.TRANSPARENT, 24, 0, 0));
    return v;
  }

  private void switchPage(int page) {
    if (pageHost == null) return;
    int oldPage = currentPage;
    if (page == currentPage && pageHost.getChildCount() > 0) return;
    currentPage = page;
    pageHost.removeAllViews();
    pageHost.setBackgroundColor(bgColor());

    // 0=主页 1=驱动 2=我的
    View next = null;
    if (page == 0) next = createHomePage();
    else if (page == 1) next = createDriverPage(); // 新增驱动页面
    else next = createMinePage();

    pageHost.addView(next, new LinearLayout.LayoutParams(-1, -1));
    animatePageIn(next, page >= oldPage ? 1 : -1);
    updateNav();
  }

  private void animatePageIn(View view, int direction) {
    AnimationSet set = new AnimationSet(true);
    TranslateAnimation slide =
        new TranslateAnimation(
            Animation.RELATIVE_TO_PARENT,
            direction >= 0 ? 0.08f : -0.08f,
            Animation.RELATIVE_TO_PARENT,
            0f,
            Animation.RELATIVE_TO_SELF,
            0f,
            Animation.RELATIVE_TO_SELF,
            0f);
    AlphaAnimation alpha = new AlphaAnimation(0.18f, 1f);
    slide.setDuration(260);
    alpha.setDuration(220);
    slide.setInterpolator(new DecelerateInterpolator());
    set.addAnimation(slide);
    set.addAnimation(alpha);
    view.startAnimation(set);
  }

  private void updateNav() {
    // 主页按钮
    if (navHome != null) {
      boolean active = currentPage == 0;
      navHome.setTextColor(active ? Color.WHITE : subTextColor());
      navHome.setBackground(round(active ? primaryColor() : Color.TRANSPARENT, 24, 0, 0));
    }
    // 驱动按钮（新增）
    if (navDriver != null) {
      boolean active = currentPage == 1;
      navDriver.setTextColor(active ? Color.WHITE : subTextColor());
      navDriver.setBackground(round(active ? primaryColor() : Color.TRANSPARENT, 24, 0, 0));
    }
    // 我的按钮
    if (navMine != null) {
      boolean active = currentPage == 2;
      navMine.setTextColor(active ? Color.WHITE : subTextColor());
      navMine.setBackground(round(active ? primaryColor() : Color.TRANSPARENT, 24, 0, 0));
    }
  }

  private View createHomePage() {
     // 【修复】外层嵌套 ScrollView，整个主页支持全局滚动（兼容小窗/分屏）
    ScrollView rootScroll = new ScrollView(this);
    rootScroll.setVerticalScrollBarEnabled(false);
    rootScroll.setFillViewport(false);
    rootScroll.setBackgroundColor(bgColor());

    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(18), dp(18), dp(18), dp(8));
    page.setBackgroundColor(bgColor());
     // 把原page装入外层ScrollView
    rootScroll.addView(page, new ScrollView.LayoutParams(-1, -2));

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);
    page.addView(header, lp(-1, -2, 0, 0, 0, dp(14)));

    TextView logo = text("A", 18, Color.WHITE, Typeface.BOLD);
    logo.setGravity(Gravity.CENTER);
    logo.setBackground(round(primaryColor(), 18, 0, 0));
    header.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));

    LinearLayout ht = new LinearLayout(this);
    ht.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(0, -2, 1);
    htlp.setMargins(dp(12), 0, 0, 0);
    header.addView(ht, htlp);
    ht.addView(text("AuraKernel", 28, textColor(), Typeface.BOLD));
    TextView desc = text("选择驱动并刷入后运行脚本", 13, subTextColor(), Typeface.NORMAL);
    desc.setSingleLine(true);
    desc.setEllipsize(TextUtils.TruncateAt.END);
    ht.addView(desc);

    // ====================== 拆分开始：每个模块独立卡片 ======================
    // 1. 卡密模块 - 独立卡片
    LinearLayout cardKey = new LinearLayout(this);
    cardKey.setOrientation(LinearLayout.VERTICAL);
    cardKey.setPadding(dp(16), dp(16), dp(16), dp(16));
    cardKey.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(cardKey, lp(-1, -2, 0, 0, 0, dp(14)));

    TextView keyTitle = text("卡密", 15, textColor(), Typeface.BOLD);
    cardKey.addView(keyTitle, lp(-1, -2, 0, 0, 0, dp(8)));

    keyEdit = new EditText(this);
    keyEdit.setSingleLine(true);
    keyEdit.setTextSize(14);
    keyEdit.setTextColor(textColor());
    keyEdit.setHintTextColor(subTextColor());
    keyEdit.setHint("输入卡密");
    keyEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    keyEdit.setPadding(dp(12), dp(8), dp(12), dp(8));
    keyEdit.setBackground(round(cardColor(), 12, borderColor(), 1));
    String savedKey = getSharedPreferences(PREFS, MODE_PRIVATE).getString("key_value", "");
    if (!savedKey.isEmpty()) keyEdit.setText(savedKey);
    cardKey.addView(keyEdit, lp(-1, -2, 0, dp(6), 0, 0));

    // 2. 驱动选择模块 - 独立卡片
    LinearLayout cardDriver = new LinearLayout(this);
    cardDriver.setOrientation(LinearLayout.VERTICAL);
    cardDriver.setPadding(dp(16), dp(16), dp(16), dp(16));
    cardDriver.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(cardDriver, lp(-1, -2, 0, 0, 0, dp(14)));

    TextView driverTitle = text("选择驱动", 15, textColor(), Typeface.BOLD);
    cardDriver.addView(driverTitle, lp(-1, -2, 0, 0, 0, dp(8)));

    LinearLayout driverRow = new LinearLayout(this);
    driverRow.setOrientation(LinearLayout.HORIZONTAL);
    cardDriver.addView(driverRow, lp(-1, dp(42), 0, 0, 0, 0));

    driverBtnKpm = driverOptionButton("KMA-KPM驱动", driverType == 0);
    driverBtnParadise = driverOptionButton("Paradise驱动", driverType == 1);
    driverBtnBackup = driverOptionButton("备用驱动", driverType == 2);
    driverRow.addView(driverBtnKpm, new LinearLayout.LayoutParams(0, -1, 1));
    driverRow.addView(driverBtnParadise, new LinearLayout.LayoutParams(0, -1, 1));
    driverRow.addView(driverBtnBackup, new LinearLayout.LayoutParams(0, -1, 1));

    driverBtnKpm.setOnClickListener(v -> selectDriverType(0));
    driverBtnParadise.setOnClickListener(v -> selectDriverType(1));
    driverBtnBackup.setOnClickListener(v -> selectDriverType(2));

    // 3. 内核配置（防录屏+无后台）- 独立卡片【已修改标题】
    LinearLayout cardSwitch = new LinearLayout(this);
    cardSwitch.setOrientation(LinearLayout.VERTICAL);
    cardSwitch.setPadding(dp(16), dp(16), dp(16), dp(16));
    cardSwitch.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(cardSwitch, lp(-1, -2, 0, 0, 0, dp(14)));

    // 模块标题 内核配置
    TextView switchTitle = text("内核配置", 15, textColor(), Typeface.BOLD);
    cardSwitch.addView(switchTitle, lp(-1, -2, 0, 0, 0, dp(8)));

    // 防录屏开关
    LinearLayout antiRow = new LinearLayout(this);
    antiRow.setOrientation(LinearLayout.HORIZONTAL);
    antiRow.setGravity(Gravity.CENTER_VERTICAL);
    antiRow.setPadding(0, dp(4), 0, dp(4));
    cardSwitch.addView(antiRow, lp(-1, -2, 0, 0, 0, dp(8)));

    TextView antiLabel = text("防录屏", 15, textColor(), Typeface.NORMAL);
    antiRow.addView(antiLabel, new LinearLayout.LayoutParams(0, -2, 1));
    antiRecordBtn = switchButton(antiRecord);
    antiRow.addView(antiRecordBtn, new LinearLayout.LayoutParams(dp(64), dp(32)));
    antiRecordBtn.setOnClickListener(v -> toggleAntiRecord());

    // 无后台开关
    LinearLayout bgRow = new LinearLayout(this);
    bgRow.setOrientation(LinearLayout.HORIZONTAL);
    bgRow.setGravity(Gravity.CENTER_VERTICAL);
    bgRow.setPadding(0, dp(4), 0, dp(4));
    cardSwitch.addView(bgRow, lp(-1, -2, 0, 0, 0, 0));

    TextView bgLabel = text("无后台", 15, textColor(), Typeface.NORMAL);
    bgRow.addView(bgLabel, new LinearLayout.LayoutParams(0, -2, 1));
    noBackgroundBtn = switchButton(noBackground);
    bgRow.addView(noBackgroundBtn, new LinearLayout.LayoutParams(dp(64), dp(32)));
    noBackgroundBtn.setOnClickListener(v -> toggleNoBackground());

    // 4. 运行/停止按钮 模块 - 独立卡片
    LinearLayout cardRun = new LinearLayout(this);
    cardRun.setOrientation(LinearLayout.VERTICAL);
    cardRun.setPadding(dp(16), dp(16), dp(16), dp(16));
    cardRun.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(cardRun, lp(-1, -2, 0, 0, 0, dp(14)));

    LinearLayout runRow = new LinearLayout(this);
    runRow.setOrientation(LinearLayout.HORIZONTAL);
    cardRun.addView(runRow, lp(-1, dp(48), 0, 0, 0, 0));

    runButton = button("直接运行", true);
    stopButton = button("停止", false);
    runRow.addView(runButton, new LinearLayout.LayoutParams(0, -1, 1));
    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(88), -1);
    slp.setMargins(dp(10), 0, 0, 0);
    runRow.addView(stopButton, slp);
    stopButton.setVisibility(View.GONE);

    runButton.setOnClickListener(v -> runSelectedFile());
    stopButton.setOnClickListener(v -> stopRunningProcess(true));
    // ====================== 拆分结束 ======================

    // ====================== 半屏+毛玻璃终端 ======================
    terminalScroll = new ScrollView(this);
    terminalScroll.setFillViewport(true);
    // 毛玻璃效果背景（半透明磨砂+圆角）+ 科技绿细边框
    GradientDrawable terminalBg =
        new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            // 毛玻璃核心：半透明黑色渐变，模拟磨砂模糊效果
            new int[] {Color.argb(180, 10, 15, 22), Color.argb(160, 5, 8, 12)});
    terminalBg.setCornerRadius(dp(18));
    terminalBg.setStroke(dp(1), Color.argb(30, 81, 191, 101));
    terminalScroll.setBackground(terminalBg);
    // 隐藏原生滚动条，极简现代
    terminalScroll.setVerticalScrollBarEnabled(false);
    terminalScroll.setHorizontalScrollBarEnabled(false);

    outputView = text(outputBuffer.toString(), 12, Color.rgb(81, 191, 101), Typeface.NORMAL);
    outputView.setTypeface(Typeface.MONOSPACE);
    // 文字优化：抗锯齿、行间距、字符间距
    outputView.getPaint().setAntiAlias(true);
    outputView.setLineSpacing(dp(3), 1.0f);
    outputView.setLetterSpacing(0.02f);
    // 内边距
    outputView.setPadding(dp(14), dp(14), dp(14), dp(14));

    terminalScroll.addView(outputView, new ScrollView.LayoutParams(-1, -2));
    // 核心：半屏显示 → 权重设置为 0.5，占屏幕一半高度
    page.addView(terminalScroll, new LinearLayout.LayoutParams(-1, 0, 0.5f));
    // ==============================================================

    updateRunButton();
    scrollTerminalBottom();
    // 【最后】返回外层滚动容器，而非原page
    return rootScroll;
  }

  // 驱动选项按钮样式
  private TextView driverOptionButton(String text, boolean active) {
    TextView v = new TextView(this);
    v.setText(text);
    v.setTextSize(13);
    v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    v.setGravity(Gravity.CENTER);
    v.setTextColor(active ? Color.WHITE : subTextColor());
    v.setBackground(
        round(
            active ? primaryColor() : tagColor(), 16, active ? 0 : borderColor(), active ? 0 : 1));
    return v;
  }

  // 开关按钮（仿 iOS 风格简单文本按钮）
  private TextView switchButton(boolean on) {
    TextView v = new TextView(this);
    v.setText(on ? "开启" : "关闭");
    v.setTextSize(13);
    v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    v.setGravity(Gravity.CENTER);
    v.setTextColor(on ? Color.WHITE : subTextColor());
    v.setBackground(
        round(on ? primaryColor() : tagColor(), 16, on ? 0 : borderColor(), on ? 0 : 1));
    return v;
  }

  private void selectDriverType(int type) {
    if (running) {
      Toast.makeText(this, "运行中不能切换驱动", Toast.LENGTH_SHORT).show();
      return;
    }
    driverType = type;
    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("driver_type", type).apply();
    updateDriverButtons();
    updateRunButton();
  }

  private void updateDriverButtons() {
    if (driverBtnKpm != null) {
      driverBtnKpm.setTextColor(driverType == 0 ? Color.WHITE : subTextColor());
      driverBtnKpm.setBackground(
          round(
              driverType == 0 ? primaryColor() : tagColor(),
              16,
              driverType == 0 ? 0 : borderColor(),
              driverType == 0 ? 0 : 1));
    }
    if (driverBtnParadise != null) {
      driverBtnParadise.setTextColor(driverType == 1 ? Color.WHITE : subTextColor());
      driverBtnParadise.setBackground(
          round(
              driverType == 1 ? primaryColor() : tagColor(),
              16,
              driverType == 1 ? 0 : borderColor(),
              driverType == 1 ? 0 : 1));
    }
    if (driverBtnBackup != null) {
      driverBtnBackup.setTextColor(driverType == 2 ? Color.WHITE : subTextColor());
      driverBtnBackup.setBackground(
          round(
              driverType == 2 ? primaryColor() : tagColor(),
              16,
              driverType == 2 ? 0 : borderColor(),
              driverType == 2 ? 0 : 1));
    }
  }

  private void toggleAntiRecord() {
    antiRecord = !antiRecord;
    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("anti_record", antiRecord).apply();
    updateSwitchButton(antiRecordBtn, antiRecord);
  }

  private void toggleNoBackground() {
    noBackground = !noBackground;
    getSharedPreferences(PREFS, MODE_PRIVATE)
        .edit()
        .putBoolean("no_background", noBackground)
        .apply();
    updateSwitchButton(noBackgroundBtn, noBackground);
  }

  private void updateSwitchButton(TextView btn, boolean on) {
    if (btn == null) return;
    btn.setText(on ? "开启" : "关闭");
    btn.setTextColor(on ? Color.WHITE : subTextColor());
    btn.setBackground(
        round(on ? primaryColor() : tagColor(), 16, on ? 0 : borderColor(), on ? 0 : 1));
  }

  private TextView modeButton(String text, boolean active) {
    TextView v = text(text, 13, active ? Color.WHITE : subTextColor(), Typeface.BOLD);
    v.setGravity(Gravity.CENTER);
    v.setBackground(
        round(
            active ? primaryColor() : tagColor(), 16, active ? 0 : borderColor(), active ? 0 : 1));
    return v;
  }

  private TextView addFileRow(LinearLayout parent, String title, String value, String iconText) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(3), 0, dp(3));
    parent.addView(row, new LinearLayout.LayoutParams(-1, -2));

    TextView icon = text(iconText, 16, primaryColor(), Typeface.BOLD);
    icon.setGravity(Gravity.CENTER);
    icon.setBackground(round(tagColor(), 16, 0, 0));
    LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(44), dp(44));
    ilp.setMargins(0, 0, dp(12), 0);
    row.addView(icon, ilp);

    LinearLayout texts = new LinearLayout(this);
    texts.setOrientation(LinearLayout.VERTICAL);
    row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

    TextView t = text(title, 15, textColor(), Typeface.BOLD);
    texts.addView(t, new LinearLayout.LayoutParams(-1, -2));
    TextView v = text(value, 12, subTextColor(), Typeface.NORMAL);
    v.setSingleLine(true);
    v.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    v.setPadding(0, dp(4), 0, 0);
    texts.addView(v, new LinearLayout.LayoutParams(-1, -2));

    TextView arrow = text("选择", 12, primaryColor(), Typeface.BOLD);
    arrow.setGravity(Gravity.CENTER);
    arrow.setPadding(dp(8), dp(4), dp(8), dp(4));
    arrow.setBackground(round(tagColor(), 12, 0, 0));
    row.addView(arrow, new LinearLayout.LayoutParams(-2, -2));
    return v;
  }

  // ====================== 新版驱动页面（单文件列表 + 右侧刷入按钮） ======================
  private View createDriverPage() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(false);
     scroll.setVerticalScrollBarEnabled(false);
    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(20), dp(22), dp(20), dp(20));
    page.setBackgroundColor(bgColor());
    scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

    // 1. 页面标题
    TextView title = text("驱动中心", 28, textColor(), Typeface.BOLD);
    page.addView(title, lp(-1, -2, 0, 0, 0, dp(6)));
    // 仅保留获取驱动提示
    TextView subTip = text("点击按钮获取最新驱动文件", 13, subTextColor(), Typeface.NORMAL);
    page.addView(subTip, lp(-1, -2, 0, 0, 0, dp(18)));

    // 2. 下载卡片（完全保留原有下载/解压逻辑）
    LinearLayout downloadCard = new LinearLayout(this);
    downloadCard.setOrientation(LinearLayout.VERTICAL);
    downloadCard.setPadding(dp(18), dp(18), dp(18), dp(18));
    downloadCard.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(downloadCard, lp(-1, -2, 0, 0, 0, dp(16)));

    // 下载状态文本
    driverPageStatus = text("就绪，点击下方按钮下载驱动压缩包", 13, LIGHT_GREEN, Typeface.NORMAL);
    driverPageStatus.setGravity(Gravity.CENTER);
    downloadCard.addView(driverPageStatus, lp(-1, -2, 0, 0, 0, dp(12)));

    // 进度条轨道
    final View progressTrack = new View(this);
    progressTrack.setBackground(round(TRACK_BG, 3, 0, 0));
    progressTrack.setVisibility(View.GONE);
    downloadCard.addView(progressTrack, lp(-1, dp(5), 0, 0, 0, dp(8)));

    // 进度条动画
    final View progressBar = new View(this);
    progressBar.setBackground(round(MAIN_GREEN, 3, 0, 0));
    progressBar.setVisibility(View.GONE);
    progressBar.setScaleX(0f);
    progressBar.setPivotX(0f);
    downloadCard.addView(progressBar, lp(-1, dp(5), 0, 0, 0, dp(12)));

    // 下载按钮
    final Button downloadBtn = button("获取最新驱动", true);
    downloadCard.addView(downloadBtn, lp(-1, dp(48), 0, 0, 0, 0));

    // 下载按钮点击事件
    downloadBtn.setOnClickListener(
        v -> {
          if (!isNetworkAvailable()) {
            Toast.makeText(this, "当前无网络，请检查网络", Toast.LENGTH_SHORT).show();
            return;
          }
          if (downloadTask != null && !downloadTask.isCancelled()) {
            Toast.makeText(this, "正在下载/解压中，请勿重复操作", Toast.LENGTH_SHORT).show();
            return;
          }
          // 重置UI状态
          progressTrack.setVisibility(View.VISIBLE);
          progressBar.setVisibility(View.VISIBLE);
          downloadBtn.setText("加载文件脚本中...");
          downloadBtn.setEnabled(false);
          downloadBtn.setAlpha(0.6f);
          // 启动下载+解压任务
          downloadTask = new DownloadDriverTask(this, driverPageStatus, progressBar, downloadBtn);
          downloadTask.execute();
        });

    // 分割线
    addDivider(page);

    // 3. 驱动系列 标题
    TextView listMainTitle = text("驱动系列", 16, textColor(), Typeface.BOLD);
    listMainTitle.setPadding(0, dp(16), 0, dp(8));
    page.addView(listMainTitle, lp(-1, -2, 0, 0, 0, 0));

    // 4. 统一文件列表容器（左侧名称 + 右侧刷入按钮）
    driverFileListLayout = new LinearLayout(this);
    driverFileListLayout.setOrientation(LinearLayout.VERTICAL);
    driverFileListLayout.setBackground(round(cardColor(), 20, borderColor(), 1));
    driverFileListLayout.setPadding(dp(10), dp(8), dp(10), dp(8));
    page.addView(driverFileListLayout, new LinearLayout.LayoutParams(-1, 0, 1));

    // 初始化：默认浏览驱动根目录下的「驱动」文件夹（跳过外层）
    currentBrowseDir = new File(driverRootDir, "驱动");
    // 确保目录存在（防止解压失败时崩溃）
    if (!currentBrowseDir.exists()) {
      currentBrowseDir.mkdirs();
    }
    // 加载当前目录文件列表
    refreshDriverFileList();

    return scroll;
  }

  // ====================== 驱动下载 + 自动解压 异步任务 ======================
  private static class DownloadDriverTask extends AsyncTask<Void, Integer, Boolean> {
    private final WeakReference<MainActivity> activityRef;
    private final TextView statusText;
    private final View progressBar;
    private final Button downloadBtn;

    public DownloadDriverTask(MainActivity activity, TextView status, View bar, Button btn) {
      activityRef = new WeakReference<>(activity);
      statusText = status;
      progressBar = bar;
      downloadBtn = btn;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      statusText.setText("正在连接服务器...");
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
      MainActivity activity = activityRef.get();
      if (activity == null) return false;

      try {
        // 1. 下载ZIP文件到应用files目录
        URL url = new URL(DRIVER_ZIP_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        // 兼容HTTPS（沿用原有证书忽略逻辑）
        if (conn instanceof HttpsURLConnection) {
          HttpsURLConnection sconn = (HttpsURLConnection) conn;
          TrustManager[] trustAll =
              new TrustManager[] {
                new X509TrustManager() {
                  @Override
                  public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                  }

                  @Override
                  public void checkClientTrusted(X509Certificate[] c, String a) {}

                  @Override
                  public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
              };
          SSLContext sc = SSLContext.getInstance("TLS");
          sc.init(null, trustAll, new SecureRandom());
          sconn.setSSLSocketFactory(sc.getSocketFactory());
          sconn.setHostnameVerifier((hostname, session) -> true);
        }

        int totalLength = conn.getContentLength();
        InputStream input = new BufferedInputStream(conn.getInputStream());
        FileOutputStream output = new FileOutputStream(activity.driverZipFile);

        byte[] buffer = new byte[8192];
        long downloadSize = 0;
        int len;
        while ((len = input.read(buffer)) != -1) {
          downloadSize += len;
          int progress = (int) (downloadSize * 100.0 / totalLength);
          publishProgress(progress);
          output.write(buffer, 0, len);
        }

        output.flush();
        output.close();
        input.close();
        conn.disconnect();

        // 2. 下载完成 → 自动解压到 files/drivers 目录
        publishProgress(-1);
        boolean unzipResult = activity.unZipFile(activity.driverZipFile, activity.driverRootDir);

        // 可选：解压后删除临时ZIP包（释放空间）
        activity.driverZipFile.delete();

        return unzipResult;

      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
      super.onProgressUpdate(values);
      int progress = values[0];
      MainActivity activity = activityRef.get();
      if (activity == null) return;

      if (progress == -1) {
        // 下载完成，进入解压阶段
        statusText.setText("下载完成，正在自动解压驱动...");
      } else {
        // 下载进度
        progressBar.setScaleX(progress / 100f);
        statusText.setText("下载进度：" + progress + "%");
      }
    }

    @Override
    protected void onPostExecute(Boolean success) {
      super.onPostExecute(success);
      MainActivity activity = activityRef.get();
      if (activity == null) return;

      // 恢复按钮状态
      downloadBtn.setEnabled(true);
      downloadBtn.setAlpha(1f);
      progressBar.setVisibility(View.GONE);

      if (success) {
        statusText.setText("驱动下载并解压完成！请选择对应文件");
        downloadBtn.setText("重新下载");
        Toast.makeText(activity, "驱动解压成功，请选择驱动文件", Toast.LENGTH_LONG).show();
        // ✅ 修复：调用新版刷新方法
        activity.refreshDriverFileList();
      } else {
        statusText.setText("下载/解压失败，请检查网络后重试");
        downloadBtn.setText("重新下载");
        Toast.makeText(activity, "驱动下载或解压失败", Toast.LENGTH_SHORT).show();
      }
      // 清空任务
      activity.downloadTask = null;
    }
  }

  // ====================== 工具：ZIP解压（兼容中文，解压到files目录） ======================
  private boolean unZipFile(File zipFile, File targetDir) {
    if (!zipFile.exists() || !zipFile.getName().endsWith(".zip")) {
      return false;
    }
    // 清空旧目录
    deleteDir(targetDir);
    targetDir.mkdirs();

    java.util.zip.ZipFile zip = null;
    try {
      // 解决Android ZIP中文乱码
      zip = new java.util.zip.ZipFile(zipFile, java.nio.charset.Charset.forName("GBK"));
      java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();

      while (entries.hasMoreElements()) {
        java.util.zip.ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();
        File outFile = new File(targetDir, entryName);

        // 创建父目录
        if (!outFile.getParentFile().exists()) {
          outFile.getParentFile().mkdirs();
        }

        // 修复：同时处理文件和文件夹，确保完整解压
        if (entry.isDirectory()) {
          outFile.mkdirs();
          continue;
        }

        // 写入文件
        InputStream in = zip.getInputStream(entry);
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
          out.write(buf, 0, len);
        }
        out.flush();
        out.close();
        in.close();
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      try {
        if (zip != null) zip.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void refreshDriverFileList() {
    if (driverFileListLayout == null || currentBrowseDir == null) return;

    // 重置所有状态，防止各种锁死
    isAnimating = false;
    currentExpandedFolder = null;

    driverFileListLayout.removeAllViews();

    if (!currentBrowseDir.exists() || !currentBrowseDir.isDirectory()) {
      TextView emptyTip = text("当前驱动目录不存在", 12, subTextColor(), Typeface.NORMAL);
      emptyTip.setPadding(dp(12), dp(20), dp(12), dp(20));
      emptyTip.setGravity(Gravity.CENTER);
      driverFileListLayout.addView(emptyTip);
      return;
    }

    File[] dirFiles = currentBrowseDir.listFiles();
    if (dirFiles == null || dirFiles.length == 0) {
      TextView emptyTip = text("当前驱动目录为空，请点击「获取最新驱动」下载", 12, subTextColor(), Typeface.NORMAL);
      emptyTip.setPadding(dp(12), dp(20), dp(12), dp(20));
      emptyTip.setGravity(Gravity.CENTER);
      driverFileListLayout.addView(emptyTip);
      return;
    }

    // 排序
    java.util.Arrays.sort(
        dirFiles,
        (f1, f2) -> {
          if (f1.isDirectory() && !f2.isDirectory()) return -1;
          if (!f1.isDirectory() && f2.isDirectory()) return 1;
          return f1.getName().compareToIgnoreCase(f2.getName());
        });

    // 核心修复：为根文件项设置正确的布局参数
    LinearLayout.LayoutParams itemParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

    LinearLayout.LayoutParams dividerParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));

    // 加载所有根文件
    for (int i = 0; i < dirFiles.length; i++) {
      File item = dirFiles[i];
      View fileItem = createFileItem(item, false);
      fileItem.setLayoutParams(itemParams); // 修复：设置正确布局参数
      driverFileListLayout.addView(fileItem);

      if (i < dirFiles.length - 1) {
        View divider = new View(this);
        divider.setBackgroundColor(borderColor());
        divider.setLayoutParams(dividerParams); // 修复：设置分割线布局参数
        driverFileListLayout.addView(divider);
      }
    }
  }

  private View createFileItem(final File targetFile, boolean isChild) {
    final LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setClickable(true);

    int leftPadding = dp(12);
    if (isChild) leftPadding += dp(24);
    row.setPadding(leftPadding, dp(16), dp(16), dp(16));

    final TextView nameText = new TextView(this);
    nameText.setTextSize(14);
    nameText.setTextColor(textColor());
    nameText.setSingleLine(true);
    nameText.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    nameText.setText(targetFile.getName());
    nameText.setTag(targetFile.getAbsolutePath());
    LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -2, 1);
    row.addView(nameText, nameLp);

    if (targetFile.isDirectory()) {
      final TextView iconArrow = new TextView(this);
      iconArrow.setTextSize(18);
      iconArrow.setPadding(dp(8), 0, dp(8), 0);
      iconArrow.setTextColor(primaryColor());
      iconArrow.setText("▸");
      row.addView(iconArrow);

      // ✅ 核心：点击零延迟，箭头+列表同步动画
      View.OnClickListener click =
          v -> {
            if (isAnimating) return;
            isAnimating = true;

            if (currentExpandedFolder == targetFile) {
              // 收起：箭头转回 + 列表依次滑回（逐个消失）
              animateArrow(iconArrow, false);
              collapseFolder(targetFile, iconArrow);
              currentExpandedFolder = null;
            } else {
              // 手风琴：先收起上一个（完成后）→ 展开新的
              if (currentExpandedFolder != null) {
                View prevRow = findRowByFile(currentExpandedFolder);
                TextView prevIcon = findIconInRow(prevRow);

                collapseAllChildren(
                    currentExpandedFolder,
                    () -> {
                      animateArrow(prevIcon, false);
                      // 立即展开新文件夹
                      currentExpandedFolder = targetFile;
                      animateArrow(iconArrow, true);
                      expandFolder(targetFile, row, iconArrow);
                    });
              } else {
                // 直接展开：零延迟
                currentExpandedFolder = targetFile;
                animateArrow(iconArrow, true);
                expandFolder(targetFile, row, iconArrow);
              }
            }
          };

      iconArrow.setOnClickListener(click);
      nameText.setOnClickListener(click);
      row.setOnClickListener(click);

    } else {
      Button flashBtn = new Button(this);
      flashBtn.setText("刷入");
      flashBtn.setTextSize(12);
      flashBtn.setBackgroundColor(primaryColor());
      flashBtn.setTextColor(Color.WHITE);
      flashBtn.setOnClickListener(v -> executeDriverFile(targetFile));
      LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(dp(70), dp(36));
      btnLp.setMargins(dp(8), 0, 0, 0);
      row.addView(flashBtn, btnLp);
    }

    return row;
  }

  private void expandFolder(final File folder, final View parentRow, final TextView icon) {
    if (!folder.exists() || folder.listFiles() == null) {
      isAnimating = false;
      return;
    }

    File[] childFiles = folder.listFiles();
    int parentIndex = driverFileListLayout.indexOfChild(parentRow);
    int insertIndex = parentIndex + 1;
    final String tag = "child_" + folder.getAbsolutePath();

    LinearLayout.LayoutParams itemParams =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    LinearLayout.LayoutParams dividerParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));

    // 先静默添加全部子项（不可见）
    List<View> addedItems = new ArrayList<>();
    for (int i = 0; i < childFiles.length; i++) {
      View childItem = createFileItem(childFiles[i], true);
      childItem.setTag(tag);
      childItem.setLayoutParams(itemParams);
      childItem.setAlpha(0f); // 初始透明

      View divider = new View(this);
      divider.setBackgroundColor(borderColor());
      divider.setTag(tag);
      divider.setLayoutParams(dividerParams);
      divider.setAlpha(0f);

      driverFileListLayout.addView(childItem, insertIndex++);
      driverFileListLayout.addView(divider, insertIndex++);

      addedItems.add(childItem);
      addedItems.add(divider);
    }

    // 等布局完成后，再统一播放滑入动画（零帧延迟）
    driverFileListLayout.post(
        () -> {
          for (int i = 0; i < addedItems.size(); i++) {
            View v = addedItems.get(i);
            v.setTranslationY(-dp(12)); // 初始位移
            v.setAlpha(1f);
            v.animate()
                .translationY(0)
                .setDuration(260)
                .setStartDelay(i * 18) // 连续流水
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
          }
          // 全部动画结束再解锁
          long totalDuration = 260 + (addedItems.size() - 1) * 18;
          handler.postDelayed(() -> isAnimating = false, totalDuration);
        });
  }

  // 新建：从根目录逐层展开到指定文件夹
  private void expandPathToFolder(File target) {
    // 构建从根目录到 target 的路径列表
    List<File> path = new ArrayList<>();
    File current = target;
    File root = currentBrowseDir;
    while (current != null && !current.equals(root)) {
      path.add(0, current);
      current = current.getParentFile();
    }

    // 核心修复：使用Handler延迟展开，避免阻塞UI线程
    expandPathRecursive(path, 0);
  }

  // 辅助方法：递归展开路径
  private void expandPathRecursive(final List<File> path, final int index) {
    if (index >= path.size()) {
      return;
    }

    final File step = path.get(index);
    View row = findRowByFile(step);
    if (row == null) {
      return;
    }

    TextView icon = findIconInRow(row);
    if (icon == null) {
      return;
    }

    // 展开当前文件夹
    currentExpandedFolder = null;
    expandFolder(step, row, icon);

    // 修复：使用Handler延迟50ms展开下一层，避免UI阻塞
    handler.postDelayed(() -> expandPathRecursive(path, index + 1), 50);
  }

  // 🛠️ 在 driverFileListLayout 中按文件路径查找行
  private View findRowByFile(File file) {
    String targetPath = file.getAbsolutePath();
    for (int i = 0; i < driverFileListLayout.getChildCount(); i++) {
      View child = driverFileListLayout.getChildAt(i);
      if (child instanceof LinearLayout) {
        // 先尝试直接 tag
        if (targetPath.equals(child.getTag())) continue; // tag 被用作 child_xxx
        // 遍历子 View 找 nameText 的 tag
        View nameView = findViewWithPathTag(child, targetPath);
        if (nameView != null) return child;
      }
    }
    return null;
  }

  // 🛠️ 在行内查找有指定 tag 的 TextView
  private View findViewWithPathTag(View row, String path) {
    if (row instanceof LinearLayout) {
      LinearLayout ll = (LinearLayout) row;
      for (int i = 0; i < ll.getChildCount(); i++) {
        View v = ll.getChildAt(i);
        if (v instanceof TextView && path.equals(v.getTag())) {
          return v;
        }
      }
    }
    return null;
  }

  // 🛠️ 在文件夹行内找到图标 TextView
  private TextView findIconInRow(View row) {
    if (row instanceof LinearLayout) {
      LinearLayout ll = (LinearLayout) row;
      for (int i = 0; i < ll.getChildCount(); i++) {
        View v = ll.getChildAt(i);
        if (v instanceof TextView && v != row) {
          CharSequence text = ((TextView) v).getText();
          if (text != null && (text.equals("▸") || text.equals("▾"))) {
            return (TextView) v;
          }
        }
      }
    }
    return null;
  }

  // ====================== 终极流式滑动动画（零延迟、依次联动） ======================
  // 展开：从上到下 依次滑出（无延迟，点击即动）
  private void animateItemIn(View view, int index) {
    // 先确保 View 不可见，防止闪烁
    view.setAlpha(0f);
    view.post(
        () -> {
          // 此时 layout 已完成，拿到真正的 top/left
          view.setTranslationY(-dp(12));
          view.setAlpha(1f);
          view.animate()
              .translationY(0)
              .setDuration(260)
              .setStartDelay(index * 18) // 极短错开，依旧连续
              .setInterpolator(new DecelerateInterpolator(1.5f))
              .start();
        });
  }

  // 收起：从下到上 依次滑回（逐个消失，不瞬间清空）
  private void animateItemOut(View view, int index, Runnable onFinish) {
    view.animate()
        .translationY(-dp(12))
        .alpha(0.4f) // 淡出一点更柔和
        .setDuration(260)
        .setStartDelay(index * 18)
        .setInterpolator(new DecelerateInterpolator(1.5f))
        .withEndAction(
            () -> {
              if (onFinish != null) onFinish.run();
            })
        .start();
  }

  // 文件夹箭头 旋转动画（同步联动，零卡顿）
  private void animateArrow(TextView arrow, boolean expand) {
    arrow.animate().rotation(expand ? 180 : 0).setDuration(200).start();
  }

  private void collapseFolder(final File folder, final TextView icon) {
    final String tag = "child_" + folder.getAbsolutePath();
    List<View> removeList = new ArrayList<>();
    for (int i = 0; i < driverFileListLayout.getChildCount(); i++) {
      View child = driverFileListLayout.getChildAt(i);
      if (tag.equals(child.getTag())) {
        removeList.add(child);
      }
    }

    if (removeList.isEmpty()) {
      isAnimating = false;
      return;
    }

    // ✅ 核心：倒序依次收起，逐个消失，不瞬间清空
    int total = removeList.size();
    for (int i = total - 1; i >= 0; i--) {
      View view = removeList.get(i);
      int index = total - 1 - i;

      // 最后一个动画完成后解锁
      if (i == 0) {
        animateItemOut(
            view,
            index,
            () -> {
              driverFileListLayout.removeView(view);
              isAnimating = false;
            });
      } else {
        animateItemOut(view, index, () -> driverFileListLayout.removeView(view));
      }
    }
  }

  // 带完成回调的收起（手风琴专用，丝滑无跳动）
  private void collapseAllChildren(File folder, Runnable onCollapseComplete) {
    final String tag = "child_" + folder.getAbsolutePath();
    List<View> removeList = new ArrayList<>();
    for (int i = driverFileListLayout.getChildCount() - 1; i >= 0; i--) {
      View child = driverFileListLayout.getChildAt(i);
      if (tag.equals(child.getTag())) {
        removeList.add(child);
      }
    }

    if (removeList.isEmpty()) {
      onCollapseComplete.run();
      return;
    }

    int total = removeList.size();
    for (int i = total - 1; i >= 0; i--) {
      final View view = removeList.get(i);
      final int index = total - 1 - i; // 动画序号，最后一个动画 index = total-1
      // 用 final 变量捕获
      Runnable onEnd =
          () -> {
            driverFileListLayout.removeView(view);
            if (index == total - 1) { // 最后一个元素动画结束才回调
              onCollapseComplete.run();
            }
          };
      animateItemOut(view, index, onEnd);
    }
  }

  // ====================== 核心：刷入执行逻辑（当前页面运行文件） ======================
  private void executeDriverFile(final File file) {
    if (file == null || !file.exists() || file.isDirectory()) {
      Toast.makeText(this, "文件不存在，无法执行", Toast.LENGTH_SHORT).show();
      return;
    }
    if (running) {
      Toast.makeText(this, "当前有任务正在运行，请稍后再试", Toast.LENGTH_SHORT).show();
      return;
    }
    // 打开独立终端
    showDriverTerminalDialog(file);
  }

  private void showDriverTerminalDialog(final File originalFile) {
    // 现代圆角透明Dialog主题
    final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setCancelable(true);

    // ===================== 根布局：现代圆角卡片 + 深色渐变背景 =====================
    LinearLayout rootLayout = new LinearLayout(this);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    // 圆角渐变背景（科技黑）
    GradientDrawable terminalBg =
        new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {Color.rgb(12, 16, 24), Color.rgb(8, 10, 16)});
    terminalBg.setCornerRadius(dp(24));
    rootLayout.setBackground(terminalBg);
    // 边距：避免全屏贴边
    rootLayout.setPadding(dp(16), dp(48), dp(16), dp(24));

    dialog.setContentView(rootLayout);
    Window window = dialog.getWindow();
    if (window != null) {
      WindowManager.LayoutParams lp = window.getAttributes();
      lp.width = WindowManager.LayoutParams.MATCH_PARENT;
      lp.height = WindowManager.LayoutParams.MATCH_PARENT;
      window.setAttributes(lp);
      // 弹窗淡入动画
      window.setWindowAnimations(android.R.style.Animation_Translucent);
    }

    // ===================== 现代半透标题栏 =====================
    LinearLayout titleBar = new LinearLayout(this);
    titleBar.setOrientation(LinearLayout.HORIZONTAL);
    titleBar.setPadding(dp(20), dp(14), dp(20), dp(14));
    titleBar.setGravity(Gravity.CENTER_VERTICAL);
    // 半透渐变标题栏
    GradientDrawable titleBg =
        new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {Color.argb(40, 255, 255, 255), Color.argb(20, 255, 255, 255)});
    titleBg.setCornerRadius(dp(16));
    titleBar.setBackground(titleBg);

    // 标题
    TextView titleText = new TextView(this);
    titleText.setText("驱动执行终端");
    titleText.setTextSize(18);
    titleText.setTextColor(Color.WHITE);
    titleText.setTypeface(Typeface.DEFAULT_BOLD);
    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1);
    titleBar.addView(titleText, titleLp);

    // 文件名（霓虹绿高亮）
    TextView fileNameText = new TextView(this);
    fileNameText.setText(originalFile.getName());
    fileNameText.setTextSize(12);
    fileNameText.setTextColor(Color.rgb(81, 191, 101));
    fileNameText.setSingleLine(true);
    fileNameText.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    fileNameText.setTypeface(Typeface.MONOSPACE);
    LinearLayout.LayoutParams fileLp = new LinearLayout.LayoutParams(0, -2, 1);
    fileLp.setMargins(dp(8), 0, dp(8), 0);
    titleBar.addView(fileNameText, fileLp);

    // 关闭按钮（圆形现代风格）
    Button closeBtn = new Button(this);
    closeBtn.setText("✕");
    closeBtn.setTextSize(16);
    closeBtn.setTextColor(Color.WHITE);
    closeBtn.setBackground(round(Color.argb(60, 255, 80, 80), 100, 0, 0));
    closeBtn.setPadding(dp(2), 0, dp(2), 0);
    closeBtn.setOnClickListener(
        v -> {
          dialog.dismiss();
          isRunning[0] = false;
        });
    LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(36), dp(36));
    titleBar.addView(closeBtn, closeLp);

    LinearLayout.LayoutParams titleBarLp = new LinearLayout.LayoutParams(-1, -2);
    titleBarLp.setMargins(0, 0, 0, dp(16));
    rootLayout.addView(titleBar, titleBarLp);

    // ===================== 终端输出区域（核心美化） =====================
    final ScrollView scrollView = new ScrollView(this);
    scrollView.setFillViewport(true);
    scrollView.setBackground(round(Color.rgb(5, 8, 12), 16, 0, 0));
    scrollView.setPadding(dp(16), dp(16), dp(16), dp(16));
    scrollView.setVerticalScrollBarEnabled(false);

    final TextView outputView = new TextView(this);
    outputView.setTextSize(13);
    outputView.setTextColor(Color.rgb(220, 240, 225));
    // 专业等宽字体 + 行间距
    outputView.setTypeface(Typeface.MONOSPACE);
    outputView.setLineSpacing(dp(4), 1.0f);
    outputView.setText("▶ 准备执行：" + originalFile.getName() + "\n▶ 等待Root权限授权...\n");
    scrollView.addView(outputView);

    LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1);
    scrollLp.setMargins(0, 0, 0, dp(16));
    rootLayout.addView(scrollView, scrollLp);

    // ===================== 底部功能按钮栏（3按钮：停止/清除/复制） =====================
    LinearLayout bottomBar = new LinearLayout(this);
    bottomBar.setOrientation(LinearLayout.HORIZONTAL);
    bottomBar.setGravity(Gravity.CENTER);
    bottomBar.setPadding(dp(4), 0, dp(4), 0);

    // 停止按钮
    final Button stopButton = createModernButton("停止运行", Color.rgb(255, 80, 80));
    // 清除按钮
    final Button clearButton = createModernButton("清除日志", Color.rgb(100, 149, 237));
    // 复制按钮
    final Button copyButton = createModernButton("复制输出", Color.rgb(81, 191, 101));

    // 按钮布局权重
    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(50), 1);
    btnLp.setMargins(dp(6), 0, dp(6), 0);
    bottomBar.addView(stopButton, btnLp);
    bottomBar.addView(clearButton, btnLp);
    bottomBar.addView(copyButton, btnLp);

    rootLayout.addView(bottomBar, new LinearLayout.LayoutParams(-1, -2));

    // ===================== 执行逻辑（原逻辑不变） =====================
    final Process[] processHolder = new Process[1];
    final BufferedWriter[] writerHolder = new BufferedWriter[1];
    final boolean[] isRunning = {true};
    final Handler handler = new Handler();
    final StringBuilder outputBuffer = new StringBuilder();

    // 停止按钮
    stopButton.setOnClickListener(
        v -> {
          isRunning[0] = false;
          try {
            if (writerHolder[0] != null) writerHolder[0].close();
          } catch (Exception ignored) {
          }
          try {
            if (processHolder[0] != null) processHolder[0].destroy();
          } catch (Exception ignored) {
          }
          appendTerminalText(outputView, outputBuffer, "\n⏹️ 已手动停止运行\n", scrollView);
          stopButton.setText("已停止");
          stopButton.setEnabled(false);
        });

    // 清除日志按钮
    clearButton.setOnClickListener(
        v -> {
          outputBuffer.setLength(0);
          outputView.setText("▶ 终端已清空\n");
        });

    // 复制输出按钮
    copyButton.setOnClickListener(
        v -> {
          android.content.ClipboardManager clipboard =
              (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("驱动终端日志", outputView.getText().toString());
          clipboard.setPrimaryClip(clip);
          Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

    // 弹窗关闭销毁进程
    dialog.setOnDismissListener(
        di -> {
          isRunning[0] = false;
          try {
            if (writerHolder[0] != null) writerHolder[0].close();
          } catch (Exception ignored) {
          }
          try {
            if (processHolder[0] != null) processHolder[0].destroy();
          } catch (Exception ignored) {
          }
        });

    dialog.show();

    // 执行线程
    new Thread(
            () -> {
              try {
                handler.post(
                    () ->
                        appendTerminalText(
                            outputView, outputBuffer, "🔐 正在请求 Root 权限...\n", scrollView));
                if (!RunnerSupport.hasRoot()) {
                  handler.post(
                      () ->
                          appendTerminalText(
                              outputView, outputBuffer, "❌ Root 权限不可用，执行终止\n", scrollView));
                  return;
                }

                String fileName = originalFile.getName().toLowerCase();
                boolean isScript =
                    fileName.endsWith(".sh")
                        || fileName.endsWith(".bash")
                        || fileName.endsWith(".zsh");

                File localFile =
                    RunnerSupport.copyFromPath(
                        MainActivity.this, originalFile, originalFile.getName());
                if (localFile == null || !localFile.exists()) {
                  handler.post(
                      () ->
                          appendTerminalText(
                              outputView, outputBuffer, "❌ 复制驱动文件失败，无法访问\n", scrollView));
                  return;
                }

                handler.post(
                    () ->
                        appendTerminalText(outputView, outputBuffer, "⚙️ 准备运行环境...\n", scrollView));

                String rootCommand;
                if (isScript) {
                  rootCommand = "sh " + localFile.getAbsolutePath();
                  handler.post(
                      () ->
                          appendTerminalText(
                              outputView, outputBuffer, "📜 检测到脚本文件，使用 sh 执行\n", scrollView));
                } else {
                  try {
                    String rootPath =
                        RunnerSupport.prepareRootExecutable(localFile, originalFile.getName());
                    rootCommand = RunnerSupport.buildRootShellCommand(new File(rootPath), true);
                  } catch (Exception e) {
                    handler.post(
                        () ->
                            appendTerminalText(
                                outputView, outputBuffer, "⚠️ 标准准备失败，尝试备用执行方式...\n", scrollView));
                    rootCommand = "sh " + localFile.getAbsolutePath();
                  }
                }

                handler.post(
                    () ->
                        appendTerminalText(outputView, outputBuffer, "🚀 启动进程...\n\n", scrollView));

                Process p = new ProcessBuilder("su").redirectErrorStream(true).start();
                processHolder[0] = p;
                BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                writerHolder[0] = writer;

                writer.write(rootCommand);
                writer.newLine();
                writer.flush();

                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
                String line;
                while (isRunning[0] && (line = reader.readLine()) != null) {
                  final String outputLine = line + "\n";
                  handler.post(
                      () -> appendTerminalText(outputView, outputBuffer, outputLine, scrollView));
                }

                int exitCode = p.waitFor();
                final String exitMsg = "\n✅ 进程结束，退出码: " + exitCode + "\n";
                handler.post(
                    () -> {
                      appendTerminalText(outputView, outputBuffer, exitMsg, scrollView);
                      stopButton.post(
                          () -> {
                            stopButton.setText("运行结束");
                            stopButton.setEnabled(false);
                          });
                    });

              } catch (Exception e) {
                final String errorMsg = "⚠️ 执行异常: " + e.getMessage() + "\n";
                handler.post(
                    () -> appendTerminalText(outputView, outputBuffer, errorMsg, scrollView));
              }
            })
        .start();
  }

  // ===================== 新增：现代化按钮创建工具方法 =====================
  private Button createModernButton(String text, int bgColor) {
    Button btn = new Button(this);
    btn.setText(text);
    btn.setTextSize(14);
    btn.setTextColor(Color.WHITE);
    btn.setTypeface(Typeface.DEFAULT_BOLD);
    btn.setAllCaps(false);
    // 圆角背景
    btn.setBackground(round(bgColor, 100, 0, 0));
    // 移除默认按压阴影
    if (Build.VERSION.SDK_INT >= 21) {
      btn.setStateListAnimator(null);
      btn.setElevation(0f);
    }
    return btn;
  }

  // 辅助方法：追加终端文本并自动滚动
  private void appendTerminalText(
      TextView textView, StringBuilder buffer, String text, ScrollView scrollView) {
    buffer.append(text);
    if (buffer.length() > 50000) buffer.delete(0, buffer.length() - 40000);
    textView.setText(buffer.toString());
    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
  }

  // 递归删除目录
  private void deleteDir(File file) {
    if (file == null || !file.exists()) return;
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File f : files) {
          deleteDir(f);
        }
      }
    }
    file.delete();
  }

  private View createMinePage() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(20), dp(22), dp(20), dp(20));
    page.setBackgroundColor(bgColor());
    scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

    TextView title = text("我的", 28, textColor(), Typeface.BOLD);
    page.addView(title, lp(-1, -2, 0, 0, 0, dp(6)));
    TextView sub = text("设置、授权和快捷目录", 13, subTextColor(), Typeface.NORMAL);
    page.addView(sub, lp(-1, -2, 0, 0, 0, dp(18)));

    // ====================== 【修改后】系统信息模块（保留左图标 + 无右箭头） ======================
LinearLayout sysInfoCard = new LinearLayout(this);
sysInfoCard.setOrientation(LinearLayout.VERTICAL);
sysInfoCard.setPadding(dp(14), dp(8), dp(14), dp(8));
sysInfoCard.setBackground(round(cardColor(), 24, borderColor(), 1));
page.addView(sysInfoCard, lp(-1, -2, 0, 0, 0, dp(16)));

// 每行都保留左侧图标，右侧无箭头
View androidVer = sysInfoRow("安卓版本", getAndroidVersion(), "📱");
sysInfoCard.addView(androidVer);
addDivider(sysInfoCard);

View sdkVer = sysInfoRow("SDK版本", getSdkVersion(), "⚙️");
sysInfoCard.addView(sdkVer);
addDivider(sysInfoCard);

View deviceModel = sysInfoRow("设备型号", getDeviceModel(), "💻");
sysInfoCard.addView(deviceModel);
addDivider(sysInfoCard);

View deviceManu = sysInfoRow("设备厂商", getDeviceManufacturer(), "🏭");
sysInfoCard.addView(deviceManu);
addDivider(sysInfoCard);

View cpuArch = sysInfoRow("CPU架构", getCpuArchitecture(), "🖥️");
sysInfoCard.addView(cpuArch);
addDivider(sysInfoCard);

View kernelVer = sysInfoRow("Linux内核", getLinuxKernelVersion(), "🐧");
sysInfoCard.addView(kernelVer);
addDivider(sysInfoCard);

View selinuxStatus = sysInfoRow("SELinux状态", getSELinuxStatus(), "🛡️");
sysInfoCard.addView(selinuxStatus);
// =====================================================================================

 // ====================== 【新增 清理模块 开始】 ======================
    LinearLayout cleanCard = new LinearLayout(this);
    cleanCard.setOrientation(LinearLayout.VERTICAL);
    cleanCard.setPadding(dp(14), dp(8), dp(14), dp(8));
    cleanCard.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(cleanCard, lp(-1, -2, 0, 0, 0, dp(16)));

    // 1. 清理配置
    View cleanConfigRow = settingRow("清理配置", "自定义清理规则与目录", "⚙️");
    cleanCard.addView(cleanConfigRow);
    cleanConfigRow.setOnClickListener(v -> {
        Toast.makeText(MainActivity.this, "清理配置", Toast.LENGTH_SHORT).show();
        // 在此编写 清理配置 逻辑
    });
    addDivider(cleanCard);

    // 2. 清理内核&驱动
    View cleanKernelDriverRow = settingRow("清理内核&驱动", "清除已刷入内核、驱动文件", "🧹");
    cleanCard.addView(cleanKernelDriverRow);
    cleanKernelDriverRow.setOnClickListener(v -> {
        Toast.makeText(MainActivity.this, "开始清理内核&驱动", Toast.LENGTH_SHORT).show();
        // 在此编写 清理内核&驱动 逻辑
    });
    addDivider(cleanCard);

    // 3. 低级清理
    View cleanLowRow = settingRow("低级清理", "清理临时缓存、运行日志", "🧽");
    cleanCard.addView(cleanLowRow);
    cleanLowRow.setOnClickListener(v -> {
        Toast.makeText(MainActivity.this, "执行低级清理", Toast.LENGTH_SHORT).show();
        // 在此编写 低级清理 逻辑
    });
    addDivider(cleanCard);

    // 4. 中级清理
    View cleanMidRow = settingRow("中级清理", "清理残留配置、冗余文件", "🗑️");
    cleanCard.addView(cleanMidRow);
    cleanMidRow.setOnClickListener(v -> {
        Toast.makeText(MainActivity.this, "执行中级清理", Toast.LENGTH_SHORT).show();
        // 在此编写 中级清理 逻辑
    });
    addDivider(cleanCard);

    // 5. 高级清理
    View cleanHighRow = settingRow("高级清理", "深度清理全部残留数据", "🔥");
    cleanCard.addView(cleanHighRow);
    cleanHighRow.setOnClickListener(v -> {
        Toast.makeText(MainActivity.this, "执行高级清理", Toast.LENGTH_SHORT).show();
        // 在此编写 高级清理 逻辑
    });
    addDivider(cleanCard);

    // 6. 更改ID
    View changeIdRow = settingRow("更改ID", "修改设备/应用标识ID", "🔢");
    cleanCard.addView(changeIdRow);
    changeIdRow.setOnClickListener(v -> {
        Toast.makeText(MainActivity.this, "更改ID", Toast.LENGTH_SHORT).show();
        // 在此编写 更改ID 逻辑
    });
    addDivider(cleanCard);

    // 7. 清理说明
    View cleanDescRow = settingRow("清理说明", "查看各级清理功能介绍", "ℹ️");
    cleanCard.addView(cleanDescRow);
    cleanDescRow.setOnClickListener(v -> {
        Toast.makeText(MainActivity.this, "打开清理说明", Toast.LENGTH_SHORT).show();
        // 在此编写 清理说明 弹窗/页面逻辑
    });
    // ====================== 【新增 清理模块 结束】 ======================

    // 原有设置卡片
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(14), dp(8), dp(14), dp(8));
    card.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(card, lp(-1, -2, 0, 0, 0, dp(16)));

    View theme = settingRow("主题模式", nightMode ? "当前为夜间模式" : "当前为日间模式", "◐");
    card.addView(theme);
    theme.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            toggleTheme();
          }
        });
    addDivider(card);

    View access = settingRow("授权访问全部文件", "用于自定义文件选择器访问 /sdcard 和更多目录", "☰");
    card.addView(access);
    access.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            openAllFilesAccessSettings();
          }
        });
    addDivider(card);

    View update = settingRow("检测更新", "当前版本 v" + getVersionName(), "↑");
    card.addView(update);
    update.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            checkUpdate();
          }
        });
    addDivider(card);

    View fav = settingRow("清空快捷目录", "删除自定义文件选择器里保存的收藏目录", "×");
    card.addView(fav);
    fav.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            clearFavoriteDirs();
          }
        });
    addDivider(card);

    View exit = settingRow("退出应用", "关闭 AuraKernel", "⏻");
    card.addView(exit);
    exit.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            finish();
          }
        });

    TextView tip =
        text(
            "说明：运行程序现在采用实时管道模式，stdout/stderr 会显示在首页终端；输入框会把内容写入进程 stdin，适合公告确认、卡密输入等交互。",
            12,
            subTextColor(),
            Typeface.NORMAL);
    tip.setLineSpacing(dp(2), 1f);
    page.addView(tip, lp(-1, -2, 0, dp(4), 0, 0));
    return scroll;
}

  private View settingRow(String title, String desc, String mark) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(13), 0, dp(13));

    TextView icon = text(mark, 15, primaryColor(), Typeface.BOLD);
    icon.setGravity(Gravity.CENTER);
    icon.setBackground(round(tagColor(), 15, 0, 0));
    LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(42), dp(42));
    ilp.setMargins(0, 0, dp(12), 0);
    row.addView(icon, ilp);

    LinearLayout texts = new LinearLayout(this);
    texts.setOrientation(LinearLayout.VERTICAL);
    row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
    texts.addView(text(title, 15, textColor(), Typeface.BOLD));
    TextView d = text(desc, 12, subTextColor(), Typeface.NORMAL);
    d.setPadding(0, dp(4), 0, 0);
    texts.addView(d);
    TextView arrow = text(">", 18, subTextColor(), Typeface.BOLD);
    arrow.setGravity(Gravity.CENTER);
    row.addView(arrow, new LinearLayout.LayoutParams(dp(24), -2));
    return row;
  }

  /**
 * 系统信息专用行布局：左侧图标 + 标题 + 数值(右对齐) → 无右侧箭头
 */
private View sysInfoRow(String label, String value, String icon) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(13), 0, dp(13));

    // 🔴 保留：左侧小图标（完全不动）
    TextView tvIcon = text(icon, 16, textColor(), Typeface.BOLD);
    LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(24), -2);
    row.addView(tvIcon, iconLp);

    // 左侧标题
    TextView tvLabel = text(label, 15, textColor(), Typeface.BOLD);
    row.addView(tvLabel, new LinearLayout.LayoutParams(-2, -2));

    // 右侧数值（自动占满、右对齐）
    TextView tvValue = text(value, 14, subTextColor(), Typeface.NORMAL);
    tvValue.setGravity(Gravity.END);
    LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(0, -2, 1);
    valueLp.setMargins(dp(12), 0, 0, 0);
    row.addView(tvValue, valueLp);

    // 🟢 已移除：右侧箭头 >
    return row;
}

  private void addDivider(LinearLayout parent) {
    View v = new View(this);
    v.setBackgroundColor(borderColor());
    parent.addView(v, new LinearLayout.LayoutParams(-1, 1));
  }

  private void runSelectedFile() {
    if (selectedFile == null) {
      Toast.makeText(this, "请先等待脚本准备完毕", Toast.LENGTH_SHORT).show();
      return;
    }
    if (running) return;

    outputBuffer.setLength(0);
    if (outputView != null) outputView.setText("");
    running = true;
    updateRunButton();
    // updateInputState();
    append("开始运行脚本\n");

    new Thread(
            new Runnable() {
              public void run() {
                try {
                  post("请求 Root 权限...\n");
                  if (!RunnerSupport.hasRoot()) {
                    post("Root 权限不可用\n");
                    return;
                  }

                  post("应用触摸兼容设置...\n");
                  RunnerSupport.applyTouchCompatibility(getPackageName());

                  post("准备程序...\n");
                  String rootPath;
                  boolean isElf = false;
                  if (selectedFile.canRead()) {
                    File local =
                        RunnerSupport.copyFromPath(MainActivity.this, selectedFile, selectedName);
                    isElf = RunnerSupport.isElf(local);
                    rootPath = RunnerSupport.prepareRootExecutable(local, selectedName);
                  } else {
                    post("普通权限无法读取文件，尝试用 Root 复制...\n");
                    rootPath =
                        RunnerSupport.prepareRootExecutableFromPath(
                            selectedFile.getAbsolutePath(), selectedName);
                    isElf = RunnerSupport.looksLikeElfOrBinaryName(selectedName);
                  }

                  activeRootPath = rootPath;
                  post("启动真实终端...\n\n");
                  startInteractiveRootProcess(rootPath, isElf);
                } catch (Exception e) {
                  post("运行失败: " + safeMessage(e) + "\n");
                } finally {
                  handler.post(
                      new Runnable() {
                        public void run() {
                          running = false;
                          processWriter = null;
                          runningProcess = null;
                          activeRootPath = "";
                          updateRunButton();
                        }
                      });
                }
              }
            })
        .start();
  }

  private void startInteractiveRootProcess(String rootPath, boolean isElf) throws Exception {
    String command = RunnerSupport.buildRootShellCommand(new File(rootPath), isElf);
    Process p = new ProcessBuilder("su").redirectErrorStream(true).start();
    runningProcess = p;
    processWriter = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));

    post("[AuraKernel] root command dispatched\n");
    // 传递真实脚本路径（用于更新时写回原目录）
    String originalPath = selectedFile.getAbsolutePath();
    if (originalPath != null && !originalPath.isEmpty()) {
      processWriter.write("export AURA_REAL_SCRIPT=\"" + originalPath + "\"");
      processWriter.newLine();
      processWriter.flush();
    }

    processWriter.write(command);
    processWriter.newLine();
    processWriter.flush();

    // ===== 重要：使用不包含卡密的版本 =====
    sendDriverOptionsWithoutKami();

    // ===== 读取输出，检测标记 =====
    InputStream in = p.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    boolean kamiHandled = false;
    boolean exitDetected = false;
    String line;

    try {
      while ((line = reader.readLine()) != null) {
        post(line + "\n");

        // 检测 NEED_KAMI → 需要 App 输入卡密
        if (!kamiHandled && line.contains("[NEED_KAMI]")) {
          kamiHandled = true;
          String kami = keyEdit.getText().toString().trim();
          if (!kami.isEmpty()) {
            Thread.sleep(200); // 给 C++ 一点时间到 cin
            processWriter.write(kami);
            processWriter.newLine();
            processWriter.flush();
            post("[自动输入卡密]\n");
            // 保存
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("key_value", kami).apply();
          } else {
            post("[未设置卡密]\n");
          }
        }

        // 检测 SAVED_KAMI → C++ 已自动使用，App 什么都不做
        if (!kamiHandled && line.contains("[SAVED_KAMI]")) {
          kamiHandled = true;
          post("[C++ 端已自动使用本地卡密]\n");
          // 不做任何写入操作！
        }
        // ========== 新增：检测到 exit code 时只更新界面，不杀进程 ==========
        if (!exitDetected && line.toLowerCase().contains("exit code:")) {
          exitDetected = true;
          // 在主线程把 running 设为 false，让停止按钮变回运行按钮
          handler.post(
              () -> {
                if (running) {
                  running = false;
                  // 注意：不要在这里置空 processWriter，否则后续自动输入会失败
                  updateRunButton();
                }
              });
          // 继续读取后续输出（进程可能还会输出一点内容），不要 break
        }
      }
    } catch (Exception e) {
      post("读取异常: " + e.getMessage() + "\n");
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {
      }
    }

    int code = p.waitFor();
    if (!exitDetected) {
      // 之前没检测到 exit code 才补一条退出信息
      post("\n[AuraKernel] su shell exited: " + code + "\n");
    }
    // 最终清理状态（保证即使没检测到退出码也能恢复按钮）
    handler.post(
        () -> {
          running = false;
          processWriter = null;
          runningProcess = null;
          activeRootPath = "";
          updateRunButton();
        });
  }

  private void sendDriverOptionsWithoutKami() {
    String driverNum = driverType == 0 ? "2" : (driverType == 1 ? "3" : "1");
    String antiNum = antiRecord ? "1" : "2";
    String bgNum = noBackground ? "2" : "1";

    int baseDelay = 600;

    // 1. 驱动选择
    handler.postDelayed(
        () -> {
          if (!running || processWriter == null) return;
          try {
            processWriter.write(driverNum);
            processWriter.newLine();
            processWriter.flush();
            append(driverNum + "\n");
          } catch (Exception e) {
          }
        },
        baseDelay);

    // 2. 防录屏
    handler.postDelayed(
        () -> {
          if (!running || processWriter == null) return;
          try {
            processWriter.write(antiNum);
            processWriter.newLine();
            processWriter.flush();
            append(antiNum + "\n");
          } catch (Exception e) {
          }
        },
        baseDelay + 800);

    // 3. 无后台（非KPM时）
    if (driverType != 0) {
      handler.postDelayed(
          () -> {
            if (!running || processWriter == null) return;
            try {
              processWriter.write(bgNum);
              processWriter.newLine();
              processWriter.flush();
              append(bgNum + "\n");
            } catch (Exception e) {
            }
          },
          baseDelay + 1600);
    }
  }

  private void autoSendLine(String value, String reason) {
    if (!running || processWriter == null) return;
    try {
      if (value != null && value.length() > 0) processWriter.write(value);
      processWriter.newLine();
      processWriter.flush();
      append(reason + "：" + (value == null ? "" : value) + " + 回车\n");
    } catch (Exception e) {
      append("\n自动输入失败: " + safeMessage(e) + "\n");
    }
  }

  private void stopRunningProcess(boolean showLog) {
    String path = activeRootPath;
    try {
      if (processWriter != null) {
        processWriter.write(3);
        processWriter.flush();
      }
    } catch (Exception ignored) {
    }
    try {
      if (processWriter != null) processWriter.close();
    } catch (Exception ignored) {
    }
    try {
      if (runningProcess != null) runningProcess.destroy();
    } catch (Exception ignored) {
    }
    if (path != null && path.length() > 0) RunnerSupport.stopRootProcessByPath(path);
    if (showLog) append("\n已请求停止进程\n");
    processWriter = null;
    runningProcess = null;
    activeRootPath = "";
    running = false;
    updateRunButton();
  }

  private void updateRunButton() {
    if (runButton == null) return;
    boolean ok = selectedFile != null && !running;
    runButton.setEnabled(ok);
    runButton.setAlpha(ok ? 1f : 0.55f);
    // 按钮文字根据驱动类型变化
    String label =
        (driverType == 0 ? "直接运行 (KPM)" : driverType == 1 ? "直接运行 (Paradise)" : "直接运行 (备用)");
    runButton.setText(running ? "运行中..." : label);
    runButton.setBackground(round(ok ? primaryColor() : disabledColor(), 14, 0, 0));
    runButton.setTextColor(ok ? Color.WHITE : subTextColor());

    if (stopButton != null) {
      stopButton.setVisibility(running ? View.VISIBLE : View.GONE);
      stopButton.setEnabled(running);
      stopButton.setAlpha(running ? 1f : 0.55f);
      stopButton.setBackground(round(running ? dangerBgColor() : disabledColor(), 14, 0, 0));
      stopButton.setTextColor(running ? dangerColor() : subTextColor());
    }
    // 根据运行状态控制终端的显示
    if (terminalScroll != null) {
      terminalScroll.setVisibility(running ? View.VISIBLE : View.GONE);
    }
  }

  private void showFilePicker(
      String title, boolean zipOnly, String lastDirPref, FilePickCallback callback) {
    final BrowserState state = new BrowserState();
    state.title = title;
    state.zipOnly = zipOnly;
    state.lastDirPref = lastDirPref;
    state.callback = callback;
    state.currentDir = chooseInitialDir(lastDirPref);
    state.dialog = new Dialog(this);
    state.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    state.dialog.setContentView(buildBrowserView(state));
    Window window = state.dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
      WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
      lp.copyFrom(window.getAttributes());
      lp.width = WindowManager.LayoutParams.MATCH_PARENT;
      lp.height = WindowManager.LayoutParams.MATCH_PARENT;
      window.setAttributes(lp);
    }
    state.dialog.show();
  }

  private File chooseInitialDir(String pref) {
    SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
    String last = sp.getString(pref, "");
    if (last != null && last.length() > 0) {
      File f = new File(last);
      if (f.exists() && f.isDirectory()) return f;
    }
    File sd = Environment.getExternalStorageDirectory();
    if (sd != null && sd.exists()) return sd;
    return new File("/");
  }

  private View buildBrowserView(final BrowserState state) {
    LinearLayout rootView = new LinearLayout(this);
    rootView.setOrientation(LinearLayout.VERTICAL);
    rootView.setPadding(dp(14), dp(18), dp(14), dp(14));
    rootView.setBackground(bgGradient());

    LinearLayout top = new LinearLayout(this);
    top.setOrientation(LinearLayout.HORIZONTAL);
    top.setGravity(Gravity.CENTER_VERTICAL);
    rootView.addView(top, lp(-1, dp(50), 0, 0, 0, dp(10)));
    TextView title = text(state.title, 20, textColor(), Typeface.BOLD);
    top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    Button close = smallButton("关闭");
    top.addView(close, new LinearLayout.LayoutParams(dp(70), dp(40)));
    close.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            state.dialog.dismiss();
          }
        });

    TextView path = text(state.currentDir.getAbsolutePath(), 12, subTextColor(), Typeface.NORMAL);
    path.setSingleLine(true);
    path.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    path.setPadding(dp(12), 0, dp(12), 0);
    path.setGravity(Gravity.CENTER_VERTICAL);
    path.setBackground(round(cardColor(), 14, borderColor(), 1));
    rootView.addView(path, lp(-1, dp(42), 0, 0, 0, dp(10)));

    HorizontalScrollView actionsScroll = new HorizontalScrollView(this);
    actionsScroll.setHorizontalScrollBarEnabled(false);
    LinearLayout actions = new LinearLayout(this);
    actions.setOrientation(LinearLayout.HORIZONTAL);
    actionsScroll.addView(actions, new HorizontalScrollView.LayoutParams(-2, -1));
    rootView.addView(actionsScroll, lp(-1, dp(44), 0, 0, 0, dp(10)));

    addBrowserAction(
        actions,
        "上级",
        new View.OnClickListener() {
          public void onClick(View v) {
            File parent = state.currentDir.getParentFile();
            if (parent != null) navigateBrowser(state, parent);
          }
        });
    addBrowserAction(
        actions,
        "/sdcard",
        new View.OnClickListener() {
          public void onClick(View v) {
            navigateBrowser(state, Environment.getExternalStorageDirectory());
          }
        });
    addBrowserAction(
        actions,
        "根目录 /",
        new View.OnClickListener() {
          public void onClick(View v) {
            navigateBrowser(state, new File("/"));
          }
        });
    addBrowserAction(
        actions,
        "收藏当前",
        new View.OnClickListener() {
          public void onClick(View v) {
            addFavoriteDir(state.currentDir.getAbsolutePath());
            Toast.makeText(MainActivity.this, "已收藏目录", Toast.LENGTH_SHORT).show();
            refreshBrowser(state);
          }
        });

    List<String> favs = getFavoriteDirs();
    for (int i = 0; i < favs.size(); i++) {
      final File f = new File(favs.get(i));
      if (!f.exists() || !f.isDirectory()) continue;
      addBrowserAction(
          actions,
          shortPath(f.getAbsolutePath()),
          new View.OnClickListener() {
            public void onClick(View v) {
              navigateBrowser(state, f);
            }
          });
    }

    ScrollView listScroll = new ScrollView(this);
    listScroll.setFillViewport(true);
    listScroll.setVerticalScrollBarEnabled(false);
    listScroll.setBackground(round(cardColor(), 20, borderColor(), 1));
    LinearLayout list = new LinearLayout(this);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(dp(10), dp(8), dp(10), dp(8));
    listScroll.addView(list, new ScrollView.LayoutParams(-1, -2));
    rootView.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1));

    List<File> files = listFilesForBrowser(state.currentDir, state.zipOnly);
    if (files.size() == 0) {
      TextView empty =
          text(
              "当前目录为空，或没有读取权限。可以尝试授权全部文件访问，或切换到 /sdcard、/storage/emulated/0、根目录 /。",
              13,
              subTextColor(),
              Typeface.NORMAL);
      empty.setPadding(dp(12), dp(30), dp(12), dp(30));
      empty.setGravity(Gravity.CENTER);
      empty.setLineSpacing(dp(2), 1f);
      list.addView(empty, new LinearLayout.LayoutParams(-1, -2));
    } else {
      for (int i = 0; i < files.size(); i++) {
        final File f = files.get(i);
        View row = browserRow(f, state.zipOnly);
        list.addView(row, new LinearLayout.LayoutParams(-1, dp(58)));
        if (i < files.size() - 1) addDivider(list);
        row.setOnClickListener(
            new View.OnClickListener() {
              public void onClick(View v) {
                if (f.isDirectory()) {
                  navigateBrowser(state, f);
                } else {
                  getSharedPreferences(PREFS, MODE_PRIVATE)
                      .edit()
                      .putString(state.lastDirPref, state.currentDir.getAbsolutePath())
                      .apply();
                  state.callback.onPicked(f);
                  state.dialog.dismiss();
                }
              }
            });
      }
    }
    return rootView;
  }

  private void addBrowserAction(LinearLayout actions, String label, View.OnClickListener l) {
    TextView chip = text(label, 12, textColor(), Typeface.BOLD);
    chip.setGravity(Gravity.CENTER);
    chip.setSingleLine(true);
    chip.setEllipsize(TextUtils.TruncateAt.END);
    chip.setPadding(dp(12), 0, dp(12), 0);
    chip.setBackground(round(cardColor(), 14, borderColor(), 1));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(38));
    lp.setMargins(0, 0, dp(8), 0);
    actions.addView(chip, lp);
    chip.setOnClickListener(l);
  }

  private View browserRow(File f, boolean zipOnly) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(dp(2), 0, dp(2), 0);
    TextView icon =
        text(
            f.isDirectory() ? "目录" : (zipOnly ? "ZIP" : "FILE"), 11, primaryColor(), Typeface.BOLD);
    icon.setGravity(Gravity.CENTER);
    icon.setBackground(round(tagColor(), 13, 0, 0));
    row.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(38)));
    LinearLayout texts = new LinearLayout(this);
    texts.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, -2, 1);
    tlp.setMargins(dp(10), 0, 0, 0);
    row.addView(texts, tlp);
    TextView name =
        text(
            f.getName().length() == 0 ? f.getAbsolutePath() : f.getName(),
            14,
            textColor(),
            f.isDirectory() ? Typeface.BOLD : Typeface.NORMAL);
    name.setSingleLine(true);
    name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    texts.addView(name, new LinearLayout.LayoutParams(-1, -2));
    String info =
        f.isDirectory()
            ? f.getAbsolutePath()
            : readableSize(f.length()) + " · " + f.getAbsolutePath();
    TextView detail = text(info, 11, subTextColor(), Typeface.NORMAL);
    detail.setSingleLine(true);
    detail.setEllipsize(TextUtils.TruncateAt.MIDDLE);
    detail.setPadding(0, dp(3), 0, 0);
    texts.addView(detail, new LinearLayout.LayoutParams(-1, -2));
    return row;
  }

  private List<File> listFilesForBrowser(File dir, boolean zipOnly) {
    ArrayList<File> out = new ArrayList<File>();
    if (dir == null) return out;
    File[] arr = null;
    try {
      arr = dir.listFiles();
    } catch (Exception ignored) {
    }
    if (arr == null) return out;
    for (File f : arr) {
      if (f == null) continue;
      if (f.isHidden() && !dir.getAbsolutePath().equals("/")) continue;
      if (f.isDirectory()) out.add(f);
      else if (!zipOnly || f.getName().toLowerCase().endsWith(".zip")) out.add(f);
    }
    Collections.sort(
        out,
        new Comparator<File>() {
          public int compare(File a, File b) {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            return a.getName().compareToIgnoreCase(b.getName());
          }
        });
    return out;
  }

  private void navigateBrowser(BrowserState state, File dir) {
    if (dir == null) return;
    state.currentDir = dir;
    refreshBrowser(state);
  }

  private void refreshBrowser(BrowserState state) {
    if (state.dialog != null) state.dialog.setContentView(buildBrowserView(state));
  }

  private List<String> getFavoriteDirs() {
    String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_FAV_DIRS, "");
    ArrayList<String> list = new ArrayList<String>();
    if (raw == null || raw.length() == 0) return list;
    String[] parts = raw.split("\\n");
    for (int i = 0; i < parts.length; i++) {
      String p = parts[i].trim();
      if (p.length() > 0 && !list.contains(p)) list.add(p);
    }
    return list;
  }

  private void addFavoriteDir(String path) {
    if (path == null || path.length() == 0) return;
    List<String> list = getFavoriteDirs();
    if (!list.contains(path)) list.add(0, path);
    while (list.size() > 12) list.remove(list.size() - 1);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < list.size(); i++) b.append(list.get(i)).append('\n');
    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_FAV_DIRS, b.toString()).apply();
  }

  private void clearFavoriteDirs() {
    getSharedPreferences(PREFS, MODE_PRIVATE)
        .edit()
        .remove(PREF_FAV_DIRS)
        .remove(PREF_LAST_RUN_DIR)
        .remove(PREF_LAST_DRIVER_DIR)
        .apply();
    Toast.makeText(this, "已清空快捷目录", Toast.LENGTH_SHORT).show();
  }

  private String shortPath(String p) {
    if (p == null) return "收藏";
    if (p.length() <= 18) return p;
    return "…" + p.substring(p.length() - 17);
  }

  private String readableSize(long size) {
    if (size < 1024) return size + " B";
    if (size < 1024 * 1024) return (size / 1024) + " KB";
    return (size / (1024 * 1024)) + " MB";
  }

  private void requestStoragePermissionQuietly() {
    if (Build.VERSION.SDK_INT >= 23) {
      try {
        ArrayList<String> perms = new ArrayList<String>();
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
          perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
          perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (perms.size() > 0)
          requestPermissions(perms.toArray(new String[perms.size()]), REQ_STORAGE);
      } catch (Exception ignored) {
      }
    }
  }

  private void openAllFilesAccessSettings() {
    try {
      if (Build.VERSION.SDK_INT >= 30) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
      } else {
        requestStoragePermissionQuietly();
        Toast.makeText(this, "已请求存储权限", Toast.LENGTH_SHORT).show();
      }
    } catch (Exception e) {
      try {
        startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
      } catch (Exception ignored) {
        Toast.makeText(this, "无法打开授权页面", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void toggleTheme() {
    nightMode = !nightMode;
    SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
    sp.edit().putBoolean("night_mode", nightMode).apply();
    setupWindow();
    showMainShell();
    switchPage(1);
  }

  // ====================== 检测更新（内部类） ======================
  private static class UpdateTask extends AsyncTask<Void, Void, String> {
    private WeakReference<MainActivity> activityRef;

    UpdateTask(MainActivity activity) {
      activityRef = new WeakReference<>(activity);
    }

    @Override
    protected String doInBackground(Void... voids) {
      try {
        URL url = new URL("https://aura.xiaon.sbs/update/update.json");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return sb.toString();
      } catch (Exception e) {
        return null;
      }
    }

    @Override
    protected void onPostExecute(String result) {
      super.onPostExecute(result);
      MainActivity activity = activityRef.get();
      if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

      if (result == null || result.trim().isEmpty()) {
        Toast.makeText(activity, "检查更新失败", Toast.LENGTH_SHORT).show();
        return;
      }

      try {
        JSONObject json = new JSONObject(result);
        int newVersionCode = json.getInt("versionCode");
        String newVersionName = json.optString("versionName", "未知版本");
        String apkUrl = json.optString("apkUrl", "");
        String desc = json.optString("description", "有新版本可用");

        int currentVersionCode;
        if (Build.VERSION.SDK_INT >= 28) {
          currentVersionCode =
              (int)
                  activity
                      .getPackageManager()
                      .getPackageInfo(activity.getPackageName(), 0)
                      .getLongVersionCode();
        } else {
          currentVersionCode =
              activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode;
        }

        String currentVersionName;
        try {
          currentVersionName =
              activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
          if (currentVersionName == null) currentVersionName = "";
        } catch (Exception e) {
          currentVersionName = "";
        }

        if (newVersionName.equals(currentVersionName)) {
          return; // 静默，已是最新
        }

        if (newVersionCode > currentVersionCode) {
          activity.showUpdateDialog(newVersionName, desc, apkUrl);
        }
      } catch (Exception ignored) {
      }
    }
  }

  // 检查更新（按钮点击事件）
  private void checkUpdate() {
    if (!isNetworkAvailable()) {
      Toast.makeText(this, "当前无网络连接", Toast.LENGTH_SHORT).show();
      return;
    }
    // 新增：加载提示
    Toast.makeText(this, "正在检查更新...", Toast.LENGTH_SHORT).show();
    // 取消旧任务，防止重复执行
    if (updateTask != null && !updateTask.isCancelled()) {
      updateTask.cancel(true);
    }
    updateTask = new UpdateTask(this);
    updateTask.execute();
  }

  // 显示更新弹窗 - 修复可能的编译兼容性问题
  private void showUpdateDialog(String newVersion, String desc, final String apkUrl) {
    new android.app.AlertDialog.Builder(this)
        .setTitle("发现新版本 v" + newVersion)
        .setMessage(desc)
        .setPositiveButton(
            "立即更新",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                downloadApk(apkUrl);
              }
            })
        .setNegativeButton("稍后再说", null)
        .show();
  }

  // 下载APK - 彻底修复：使用浏览器打开下载链接，避免DownloadManager各种兼容性问题
  private void downloadApk(String apkUrl) {
    // ① 检查 apkUrl 是否有效
    if (apkUrl == null || apkUrl.trim().isEmpty()) {
      Toast.makeText(this, "更新链接无效，请稍后重试", Toast.LENGTH_SHORT).show();
      return;
    }

    // ② 使用系统浏览器打开下载链接（最稳定、最兼容的方式）
    try {
      Uri uri = Uri.parse(apkUrl);
      if (uri == null) {
        Toast.makeText(this, "更新地址格式错误", Toast.LENGTH_SHORT).show();
        return;
      }

      Intent intent = new Intent(Intent.ACTION_VIEW, uri);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      // 检查是否有浏览器能处理这个链接
      if (intent.resolveActivity(getPackageManager()) != null) {
        startActivity(intent);
        Toast.makeText(this, "已打开浏览器，请下载APK后手动安装", Toast.LENGTH_LONG).show();
      } else {
        Toast.makeText(this, "未找到浏览器，请复制链接到浏览器下载", Toast.LENGTH_LONG).show();
        // 可选：复制链接到剪贴板
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
          clipboard.setPrimaryClip(ClipData.newPlainText("下载链接", apkUrl));
          Toast.makeText(this, "链接已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
      }
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "未找到可用的浏览器应用", Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    } catch (Exception e) {
      Toast.makeText(this, "打开下载链接失败", Toast.LENGTH_SHORT).show();
      e.printStackTrace();
    }
  }

  // 判断网络是否可用
  private boolean isNetworkAvailable() {
    try {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
      NetworkInfo info = cm.getActiveNetworkInfo();
      return info != null && info.isConnected();
    } catch (Exception e) {
      return false;
    }
  }

  // ====================== 【修复结束】 ======================

  private String getVersionName() {
    try {
      String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      return v == null ? "1.0" : v;
    } catch (Exception e) {
      return "1.0";
    }
  }

  // ====================== 系统信息获取工具 ======================
private String getAndroidVersion() {
    return Build.VERSION.RELEASE;
}

private String getSdkVersion() {
    return String.valueOf(Build.VERSION.SDK_INT);
}

private String getDeviceModel() {
    return Build.MODEL;
}

private String getDeviceManufacturer() {
    return Build.MANUFACTURER;
}

private String getCpuArchitecture() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return Build.SUPPORTED_ABIS[0];
    } else {
        return Build.CPU_ABI;
    }
}

private String getLinuxKernelVersion() {
    String full = System.getProperty("os.version", "未知");
    // 优先匹配三位版本号，如 5.10.43
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+\\.\\d+\\.\\d+").matcher(full);
    if (m.find()) {
        return m.group();
    }
    // 如果只有两位，如 5.10
    m = java.util.regex.Pattern.compile("\\d+\\.\\d+").matcher(full);
    if (m.find()) {
        return m.group();
    }
    return "未知";
}

private String getSELinuxStatus() {
    try {
        // 依赖Root权限获取SELinux状态
        Process process = Runtime.getRuntime().exec("su -c getenforce");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String status = reader.readLine().trim();
        reader.close();
        process.destroy();
        return "Enforcing".equals(status) ? "开启" : "关闭";
    } catch (Exception e) {
        e.printStackTrace();
    }
    return "未知(需Root)";
}

  private void append(String s) {
    outputBuffer.append(s);
    if (outputBuffer.length() > 80000) outputBuffer.delete(0, outputBuffer.length() - 60000);
    if (outputView != null) {
      String fullText = outputBuffer.toString();
      outputView.setText(fullText);
      // 智能配色：错误红/成功绿/普通灰
      if (fullText.contains("失败")
          || fullText.contains("异常")
          || fullText.contains("错误")
          || fullText.contains("❌")) {
        outputView.setTextColor(Color.rgb(255, 99, 71));
      } else if (fullText.contains("运行")
          || fullText.contains("成功")
          || fullText.contains("✅")
          || fullText.contains("就绪")) {
        outputView.setTextColor(Color.rgb(81, 191, 101));
      } else {
        outputView.setTextColor(Color.rgb(200, 215, 225));
      }
    }
    scrollTerminalBottom();
  }

  private void post(final String s) {
    handler.post(
        new Runnable() {
          public void run() {
            append(s);
          }
        });
  }

  private void scrollTerminalBottom() {
    if (terminalScroll != null) {
      terminalScroll.post(
          new Runnable() {
            public void run() {
              terminalScroll.fullScroll(View.FOCUS_DOWN);
            }
          });
    }
  }

  private String safeMessage(Throwable e) {
    String m = e.getMessage();
    return m == null ? e.getClass().getSimpleName() : m;
  }

  private void addSpace(LinearLayout parent, int dp) {
    View v = new View(this);
    parent.addView(v, new LinearLayout.LayoutParams(1, dp(dp)));
  }

  private Button button(String text, boolean primary) {
    Button b = new Button(this);
    b.setAllCaps(false);
    b.setText(text);
    b.setTextSize(13);
    b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    b.setGravity(Gravity.CENTER);
    b.setPadding(0, 0, 0, 0);
    b.setMinHeight(0);
    b.setMinimumHeight(0);
    b.setBackground(round(primary ? primaryColor() : disabledColor(), 14, 0, 0));
    b.setTextColor(primary ? Color.WHITE : subTextColor());
    if (Build.VERSION.SDK_INT >= 21) {
      b.setStateListAnimator(null);
      b.setElevation(0f);
    }
    return b;
  }

  private Button smallButton(String text) {
    Button b = button(text, false);
    b.setTextSize(12);
    return b;
  }

  private TextView text(String s, int sp, int color, int style) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(sp);
    v.setTextColor(color);
    v.setTypeface(Typeface.DEFAULT, style);
    return v;
  }

  private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
    p.setMargins(l, t, r, b);
    return p;
  }

  private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(dp(radius));
    if (strokeWidth > 0) g.setStroke(dp(strokeWidth), strokeColor);
    return g;
  }

  private int bgColor() {
    return nightMode ? Color.rgb(10, 13, 20) : Color.rgb(250, 251, 254);
  }

  private int cardColor() {
    return nightMode ? Color.rgb(22, 26, 36) : Color.WHITE;
  }

  private int textColor() {
    return nightMode ? Color.rgb(238, 242, 248) : Color.rgb(17, 23, 34);
  }

  private int subTextColor() {
    return nightMode ? Color.rgb(144, 153, 169) : Color.rgb(112, 120, 136);
  }

  private int borderColor() {
    return nightMode ? Color.rgb(42, 48, 62) : Color.rgb(235, 240, 248);
  }

  private int primaryColor() {
    // return Color.rgb(22, 119, 255);
    return MAIN_GREEN;
  }

  private int tagColor() {
    return nightMode ? Color.rgb(26, 44, 75) : Color.rgb(239, 245, 255);
  }

  private int terminalBgColor() {
    return Color.rgb(9, 12, 18);
  }

  private int successColor() {
    return nightMode ? Color.rgb(79, 224, 146) : Color.rgb(12, 162, 92);
  }

  private int successBgColor() {
    return nightMode ? Color.rgb(18, 55, 38) : Color.rgb(229, 250, 239);
  }

  private int dangerColor() {
    return nightMode ? Color.rgb(255, 139, 148) : Color.rgb(213, 42, 55);
  }

  private int dangerBgColor() {
    return nightMode ? Color.rgb(66, 30, 38) : Color.rgb(255, 238, 240);
  }

  private int disabledColor() {
    return nightMode ? Color.rgb(37, 43, 56) : Color.rgb(224, 229, 238);
  }

  private int dp(int v) {
    return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
  }
}