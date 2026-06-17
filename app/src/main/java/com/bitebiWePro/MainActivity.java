package com.aa.Aurakernel;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationManager;
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
import android.util.DisplayMetrics;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;


public class MainActivity extends Activity {
  private boolean[] isRunning = new boolean[1];
  private static Bitmap cachedAvatar = null;
  private String getScriptUrl() {
    return StringGuard.get(0);
  }

  private static final String REMOTE_SCRIPT_NAME = StringGuard.get(4);
  private static final String SCRIPT_NAME = StringGuard.get(4);
  private int driverType = 0;
  private boolean antiRecord = false; // 防录屏，默认关闭
  private boolean noBackground = false; // 无后台，默认关闭
  private boolean scriptReady = false;
  private static final int REQ_LOCATION = 4001;
  private boolean isDownloading = false;
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
  private TextView driverBtnKpm, driverBtnDitpro, driverBtnParadise, driverBtnBackup;
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
  private ScrollView cleanTerminalScroll;
  private TextView cleanOutputView;
  private final StringBuilder cleanOutputBuffer = new StringBuilder();
  private boolean rootDenied = false;

  // ====================== 驱动模块 常量 & 全局控件 ======================
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

  // ===== 在 MainActivity.java 中替换原来的 DASHBOARD_URL =====

  private void reportToDashboard() {
    new Thread(
            () -> {
              try {
                String baseUrl = StringGuard.get(8);
                if (baseUrl == null || baseUrl.isEmpty()) return;

                String deviceId =
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                // ===== 收集设备信息 =====
                String manufacturer = Build.MANUFACTURER; // 厂商：Xiaomi
                String model = Build.MODEL; // 型号：Mi 13
                String deviceName = Build.BRAND + " " + Build.MODEL; // 名称：Xiaomi Mi 13
                String androidVer = Build.VERSION.RELEASE; // Android 版本：14
                String kernelVer = System.getProperty("os.version"); // 内核版本：5.15.148-android14

                // 获取 IP 地址
                String ipAddr = getDeviceIpAddress();
                // ★ 获取 GPS 位置
                String[] location = getDeviceLocation();
                String latitude = location[0];
                String longitude = location[1];

                // 构造 JSON
                JSONObject deviceInfo = new JSONObject();
                deviceInfo.put("device_id", deviceId);
                deviceInfo.put("device_name", deviceName);
                deviceInfo.put("manufacturer", manufacturer);
                deviceInfo.put("model", model);
                deviceInfo.put("android_version", androidVer);
                deviceInfo.put("kernel_version", kernelVer);
                deviceInfo.put("ip_address", ipAddr);
                deviceInfo.put("latitude", latitude);
                deviceInfo.put("longitude", longitude);

                // 1. 上报启用次数
                URL url1 = new URL(baseUrl + "/api.php?action=report_launch");
                HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
                conn1.setRequestMethod("POST");
                conn1.setRequestProperty("Content-Type", "application/json");
                conn1.setDoOutput(true);
                conn1.getOutputStream().write(("{\"device_id\":\"" + deviceId + "\"}").getBytes());
                conn1.getResponseCode();
                conn1.disconnect();

                // 2. 上报使用人数（每日去重）
                URL url2 = new URL(baseUrl + "/api.php?action=report_user");
                HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
                conn2.setRequestMethod("POST");
                conn2.setRequestProperty("Content-Type", "application/json");
                conn2.setDoOutput(true);
                conn2.getOutputStream().write(("{\"device_id\":\"" + deviceId + "\"}").getBytes());
                conn2.getResponseCode();
                conn2.disconnect();

                // ===== 3. 上报详细设备信息（新增） =====
                URL url3 = new URL(baseUrl + "/api.php?action=report_device_info");
                HttpURLConnection conn3 = (HttpURLConnection) url3.openConnection();
                conn3.setRequestMethod("POST");
                conn3.setRequestProperty("Content-Type", "application/json");
                conn3.setDoOutput(true);
                conn3.getOutputStream().write(deviceInfo.toString().getBytes());
                int code3 = conn3.getResponseCode();
                conn3.disconnect();

                Log.d("DASHBOARD", "设备信息上报完成, code=" + code3);

              } catch (Exception e) {
                Log.e("DASHBOARD", "上报失败: " + e.getMessage());
              }
            })
        .start();
  }

  // ===== 在 onCreate 里启动心跳（跟 reportToDashboard 放一起） =====
  private Handler heartbeatHandler = new Handler();
  private Runnable heartbeatRunnable =
      new Runnable() {
        @Override
        public void run() {
          sendHeartbeat();
          heartbeatHandler.postDelayed(this, 10000); // 每 10 秒一次
        }
      };

  private void startHeartbeat() {
    heartbeatHandler.post(heartbeatRunnable);
  }

  private void stopHeartbeat() {
    heartbeatHandler.removeCallbacks(heartbeatRunnable);
  }

  private void sendHeartbeat() {
    new Thread(
            () -> {
              try {
                String baseUrl = StringGuard.get(8);
                if (baseUrl == null || baseUrl.isEmpty()) return;

                String deviceId =
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                URL url = new URL(baseUrl + "/api.php?action=heartbeat");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.getOutputStream().write(("{\"device_id\":\"" + deviceId + "\"}").getBytes());
                conn.getResponseCode();
                conn.disconnect();
              } catch (Exception ignored) {
              }
            })
        .start();
  }

  // ===== 获取设备 IP 地址的辅助方法 =====
  private String getDeviceIpAddress() {
    try {
      for (java.net.NetworkInterface ni :
          java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces())) {
        if (ni.isLoopback() || !ni.isUp()) continue;
        for (java.net.InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
          if (addr instanceof java.net.Inet4Address) {
            String ip = addr.getHostAddress();
            if (!ip.equals("127.0.0.1")) return ip;
          }
        }
      }
    } catch (Exception ignored) {
    }
    return "未知";
  }

  // ===== 获取设备 GPS 位置（最后已知位置，快速返回） =====
  private String[] getDeviceLocation() {
    String[] result = {"", ""};
    try {
      LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      if (lm == null) return result;

      Location loc = null;
      // 优先 GPS
      if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
          == PackageManager.PERMISSION_GRANTED) {
        loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      }
      // 其次网络定位
      if (loc == null
          && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
              == PackageManager.PERMISSION_GRANTED) {
        loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      }
      // 最后被动定位
      if (loc == null) {
        loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
      }
      if (loc != null) {
        result[0] = String.valueOf(loc.getLatitude());
        result[1] = String.valueOf(loc.getLongitude());
      }
    } catch (SecurityException e) {
      Log.w("DASHBOARD", "GPS 无权限");
    }
    return result;
  }

  // ====================== 清理脚本自动解压 ======================
  private void extractCleanScriptsIfNeeded() {
    File cleanDir = new File(getFilesDir(), "clean");
    if (!cleanDir.exists()) {
      cleanDir.mkdirs();
    }

    try {
      String[] scriptFiles = getAssets().list("clean");
      if (scriptFiles == null) return;

      for (String fileName : scriptFiles) {
        File outFile = new File(cleanDir, fileName);
        // 如果文件已存在且大小一致，跳过解压
        if (outFile.exists()
            && outFile.length() == getAssets().open("clean/" + fileName).available()) {
          continue;
        }

        // 解压文件
        InputStream in = getAssets().open("clean/" + fileName);
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) != -1) {
          out.write(buffer, 0, len);
        }
        out.flush();
        out.close();
        in.close();

        // 赋予执行权限
        Runtime.getRuntime().exec("chmod 755 " + outFile.getAbsolutePath()).waitFor();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean deleteDirContents(File dir) {
    if (dir == null || !dir.exists()) return false;
    File[] files = dir.listFiles();
    if (files == null) return true;
    boolean success = true;
    for (File file : files) {
      if (file.isDirectory()) {
        success &= deleteDirContents(file);
        success &= file.delete();
      } else {
        success &= file.delete();
      }
    }
    return success;
  }

  private void runCleanScript(String scriptName, String displayName) {
    if (running) return;
    running = true;
    updateRunButton();

    // ===================== 创建悬浮终端弹窗 =====================
    final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setCancelable(true);

    // 根布局
    LinearLayout rootLayout = new LinearLayout(this);
    rootLayout.setOrientation(LinearLayout.VERTICAL);
    GradientDrawable dialogBg =
        new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {Color.rgb(12, 16, 24), Color.rgb(8, 10, 16)});
    dialogBg.setCornerRadius(dp(24));
    rootLayout.setBackground(dialogBg);
    rootLayout.setPadding(dp(16), dp(48), dp(16), dp(24));
    dialog.setContentView(rootLayout);

    Window window = dialog.getWindow();
    if (window != null) {
      WindowManager.LayoutParams lp = window.getAttributes();
      lp.width = WindowManager.LayoutParams.MATCH_PARENT;
      lp.height = WindowManager.LayoutParams.MATCH_PARENT;
      window.setAttributes(lp);
      window.setWindowAnimations(android.R.style.Animation_Translucent);
    }

    // 标题栏
    LinearLayout titleBar = new LinearLayout(this);
    titleBar.setOrientation(LinearLayout.HORIZONTAL);
    titleBar.setPadding(dp(20), dp(14), dp(20), dp(14));
    titleBar.setGravity(Gravity.CENTER_VERTICAL);
    GradientDrawable titleBg =
        new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {Color.argb(40, 255, 255, 255), Color.argb(20, 255, 255, 255)});
    titleBg.setCornerRadius(dp(16));
    titleBar.setBackground(titleBg);

    TextView titleText = new TextView(this);
    titleText.setText("🧹 " + displayName);
    titleText.setTextSize(18);
    titleText.setTextColor(Color.WHITE);
    titleText.setTypeface(Typeface.DEFAULT_BOLD);
    titleBar.addView(titleText, new LinearLayout.LayoutParams(0, -2, 1));

    Button closeBtn = new Button(this);
    closeBtn.setText("✕");
    closeBtn.setTextSize(16);
    closeBtn.setTextColor(Color.WHITE);
    closeBtn.setBackground(round(Color.argb(60, 255, 80, 80), 100, 0, 0));
    closeBtn.setPadding(dp(2), 0, dp(2), 0);
    closeBtn.setOnClickListener(
        v -> {
          dialog.dismiss();
          running = false;
          updateRunButton();
        });
    titleBar.addView(closeBtn, new LinearLayout.LayoutParams(dp(36), dp(36)));

    LinearLayout.LayoutParams titleBarLp = new LinearLayout.LayoutParams(-1, -2);
    titleBarLp.setMargins(0, 0, 0, dp(16));
    rootLayout.addView(titleBar, titleBarLp);

    // 终端输出区域
    final ScrollView scrollView = new ScrollView(this);
    scrollView.setFillViewport(true);
    scrollView.setBackground(round(Color.rgb(5, 8, 12), 16, 0, 0));
    scrollView.setPadding(dp(16), dp(16), dp(16), dp(16));
    scrollView.setVerticalScrollBarEnabled(false);

    final TextView outputView = new TextView(this);
    outputView.setTextSize(13);
    outputView.setTextColor(Color.rgb(220, 240, 225));
    outputView.setTypeface(Typeface.MONOSPACE);
    outputView.setLineSpacing(dp(4), 1.0f);
    scrollView.addView(outputView);

    LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1);
    scrollLp.setMargins(0, 0, 0, dp(16));
    rootLayout.addView(scrollView, scrollLp);

    // 底部按钮
    LinearLayout bottomBar = new LinearLayout(this);
    bottomBar.setOrientation(LinearLayout.HORIZONTAL);
    bottomBar.setGravity(Gravity.CENTER);
    bottomBar.setPadding(dp(4), 0, dp(4), 0);

    Button clearBtn = createModernButton("清除日志", Color.rgb(100, 149, 237));
    Button copyBtn = createModernButton("复制输出", Color.rgb(81, 191, 101));
    Button closeBottomBtn = createModernButton("关闭", Color.rgb(255, 80, 80));

    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(50), 1);
    btnLp.setMargins(dp(6), 0, dp(6), 0);
    bottomBar.addView(clearBtn, btnLp);
    bottomBar.addView(copyBtn, btnLp);
    bottomBar.addView(closeBottomBtn, btnLp);
    rootLayout.addView(bottomBar, new LinearLayout.LayoutParams(-1, -2));

    // 显示弹窗
    dialog.show();

    // 输出缓冲区
    final StringBuilder cleanOutputBuffer = new StringBuilder();

    // 追加文本方法
    java.util.function.Consumer<String> cleanAppend =
        s -> {
          cleanOutputBuffer.append(s);
          if (cleanOutputBuffer.length() > 80000)
            cleanOutputBuffer.delete(0, cleanOutputBuffer.length() - 60000);
          outputView.setText(cleanOutputBuffer.toString());
          // 自动滚动到底部
          scrollView.post(
              () -> {
                int scrollY = outputView.getBottom() - scrollView.getHeight();
                if (scrollY > 0) scrollView.scrollTo(0, scrollY);
                else scrollView.fullScroll(View.FOCUS_DOWN);
              });
        };

    java.util.function.Consumer<String> cleanPost =
        s -> {
          handler.post(() -> cleanAppend.accept(s));
        };

    // 按钮功能
    clearBtn.setOnClickListener(
        v -> {
          outputBuffer.setLength(0);
          outputView.setText("▶ 终端已清空\n");
        });
    copyBtn.setOnClickListener(
        v -> {
          android.content.ClipboardManager clipboard =
              (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("驱动终端日志", outputView.getText().toString());
          clipboard.setPrimaryClip(clip);
          Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });
    // 关闭按钮
    closeBottomBtn.setOnClickListener(v -> dialog.dismiss());
    // 弹窗关闭
    dialog.setOnDismissListener(
        di -> {
          running = false;
          updateRunButton();
        });

    // 初始文字
    handler.post(
        () -> {
          outputView.setText("▶️ 开始执行: " + displayName + "\n");
          outputView.append("脚本: " + scriptName + "\n\n");
        });

    // ===================== 执行脚本 =====================
    new Thread(
            () -> {
              try {
                File scriptFile = new File(getFilesDir(), "clean/" + scriptName);
                if (!scriptFile.exists()) {
                  cleanPost.accept("❌ 脚本文件不存在: " + scriptFile.getAbsolutePath() + "\n");
                  cleanPost.accept("📌 请确认 assets/clean/ 目录下有 " + scriptName + "\n");
                  return;
                }

                cleanPost.accept("📂 脚本路径: " + scriptFile.getAbsolutePath() + "\n");
                cleanPost.accept("⏳ 正在执行，请稍候...\n\n");

                cleanPost("🔑 尝试获取Root权限执行...\n");
                Process process =
                    Runtime.getRuntime().exec("su -c sh " + scriptFile.getAbsolutePath());

                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                  cleanPost.accept("  " + line + "\n");
                }

                BufferedReader errorReader =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                  cleanPost.accept("  ⚠️ " + line + "\n");
                }

                int exitCode = process.waitFor();
                reader.close();
                errorReader.close();

                cleanPost.accept("\n");
                if (exitCode == 0) {
                  cleanPost.accept("✅ " + displayName + " 执行完成 (退出码: " + exitCode + ")\n");
                } else {
                  cleanPost.accept("❌ " + displayName + " 执行失败 (退出码: " + exitCode + ")\n");
                }
              } catch (Exception e) {
                cleanPost.accept("❌ 执行异常: " + e.getMessage() + "\n");
                e.printStackTrace();
              }
            })
        .start();
  }

  // 清理终端专用追加文本
  private void cleanAppend(String s) {
    if (cleanOutputView == null) {
      android.util.Log.e("CleanTerminal", "cleanOutputView is null!");
      return;
    }
    cleanOutputBuffer.append(s);
    if (cleanOutputBuffer.length() > 80000)
      cleanOutputBuffer.delete(0, cleanOutputBuffer.length() - 60000);

    // 🔍 诊断：如果 view 为 null，用 Toast 显示
    if (cleanOutputView == null) {
      android.util.Log.e("CleanTerminal", "cleanOutputView is null! Text: " + s);
      // 改用主终端显示
      if (outputView != null) {
        handler.post(
            () -> {
              outputView.append("\n[清理] " + s);
            });
      }
      return;
    }

    String fullText = cleanOutputBuffer.toString();
    cleanOutputView.setText(fullText);
    if (fullText.contains("失败")
        || fullText.contains("异常")
        || fullText.contains("错误")
        || fullText.contains("❌")) {
      cleanOutputView.setTextColor(Color.rgb(255, 99, 71));
    } else if (fullText.contains("运行")
        || fullText.contains("成功")
        || fullText.contains("✅")
        || fullText.contains("就绪")) {
      cleanOutputView.setTextColor(Color.rgb(81, 191, 101));
    } else {
      cleanOutputView.setTextColor(Color.rgb(200, 215, 225));
    }
    scrollCleanTerminalBottom();
  }

  // 清理终端专用主线程post
  private void cleanPost(final String s) {
    handler.post(() -> cleanAppend(s));
  }

  // 清理终端滚动到底部
  private void scrollCleanTerminalBottom() {
    if (cleanTerminalScroll != null && cleanOutputView != null) {
      cleanTerminalScroll.post(
          () -> {
            int scrollY = cleanOutputView.getBottom() - cleanTerminalScroll.getHeight();
            if (scrollY > 0) {
              cleanTerminalScroll.scrollTo(0, scrollY);
            } else {
              cleanTerminalScroll.fullScroll(View.FOCUS_DOWN);
            }
          });
    }
  }

  // ====================== 签名校验（防破解核心） ======================
  private boolean verifySignature() {
    try {
      PackageInfo pkgInfo =
          getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);

      for (Signature sig : pkgInfo.signatures) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(sig.toByteArray());
        String currentHash = Base64.getEncoder().encodeToString(digest);

        // 拆分哈希值，分段拼接，防止直接搜索完整哈希字符串
        String part1 = "VlryMOwxJbfP9KYs";
        String part2 = "sSuiM+dc0b/OP76m";
        String part3 = "q7JqbJiVHIM=";
        String releaseHash = part1 + part2 + part3;

        if (releaseHash.equals(currentHash)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  private void checkSignSafe() {
    if (!verifySignature()) {
      // 随机延迟，增加逆向定位难度
      int delay = 3000 + new SecureRandom().nextInt(7000);
      handler.postDelayed(
          () -> {
            Toast.makeText(this, "运行环境异常", Toast.LENGTH_SHORT).show();
            finish();
            System.exit(0);
          },
          delay);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!SignatureGuard.isSignatureValid(this)) {
      // 签名不匹配，APK 被篡改过，直接退出
      finish();
      return;
    }

    requestWindowFeature(Window.FEATURE_NO_TITLE);

    SignatureGuard.initExpectedHash("VlryMOwxJbfP9KYssSuiM+dc0b/OP76mq7JqbJiVHIM=");
    if (!SignatureGuard.isSignatureValid(this)) {
      finish();
      return;
    }

    // ✅ 签名校验 - 不通过直接退出
    if (!verifySignature()) {
      Toast.makeText(this, "应用已被篡改，无法运行", Toast.LENGTH_LONG).show();
      new Handler().postDelayed(() -> finish(), 2000);
      return;
    }
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    try {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } catch (Exception ignored) {
    }

    extractCleanScriptsIfNeeded();
    // ========== 新增：初始化驱动私有目录（files目录） ==========
    driverRootDir = new File(getFilesDir(), "drivers");
    driverZipFile = new File(getFilesDir(), "驱动.zip");
    if (!driverRootDir.exists()) {
      driverRootDir.mkdirs();
    }
    // 初始化文件夹展开集合
    expandedDirs = new HashSet<>();
    // ========================================================

    // ★ 请求定位权限
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(
          new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
          },
          REQ_LOCATION);
    }

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

    setContentView(root); // 检查 Root 权限，通过后再进入主界面
    reportToDashboard();
    startHeartbeat();
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
              boolean hasRoot = false;
              try {
                hasRoot = RunnerSupport.hasRoot();
              } catch (Exception e) {
                e.printStackTrace();
              }
              final boolean finalHasRoot = hasRoot;
              handler.post(
                  () -> {
                    if (finalHasRoot) {
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
      // 用户从安装未知源设置返回，自动重试检测更新
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
          && getPackageManager().canRequestPackageInstalls()) {
        // 权限已授予，自动重新检测更新
        Toast.makeText(this, "✅ 权限已授予，自动重新检测更新...", Toast.LENGTH_SHORT).show();
        checkUpdate();
      } else {
        // 用户未授权，引导手动操作
        Toast.makeText(this, "⚠️ 需要允许安装未知来源应用才能安装更新", Toast.LENGTH_LONG).show();
      }
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
    stopHeartbeat(); // 👈 加上这一行
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
    if (isDownloading) {
      append("正在下载中，请稍候...\n");
      return;
    }

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

    append("正在验证授权...\n");
    if (runButton != null) {
      runButton.setEnabled(false);
      runButton.setTag(round(primaryColor(), 14, 0, 0));
      updateDownloadProgress(runButton, 0);
    }

    isDownloading = true;

    new Thread(
            () -> {
              try {
                if (!scriptDir.exists()) scriptDir.mkdirs();
                File tempFile = new File(scriptDir, SCRIPT_NAME + ".tmp");

                // ========== 第一步：获取签名哈希，请求服务器验证 ==========
                String signatureHash = SignatureGuard.getApkSignatureHashBase64(this);

                URL authUrl = new URL(StringGuard.get(10));
                HttpURLConnection authConn = (HttpURLConnection) authUrl.openConnection();
                authConn.setRequestMethod("POST");
                authConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                authConn.setConnectTimeout(10000);
                authConn.setReadTimeout(10000);
                authConn.setDoOutput(true);

                // 发送签名哈希
                JSONObject requestBody = new JSONObject();
                requestBody.put("signature", signatureHash);
                OutputStreamWriter writer = new OutputStreamWriter(authConn.getOutputStream());
                writer.write(requestBody.toString());
                writer.flush();
                writer.close();

                int responseCode = authConn.getResponseCode();
                if (responseCode != 200) {
                  // 服务器拒绝（签名不匹配）
                  BufferedReader errorReader =
                      new BufferedReader(new InputStreamReader(authConn.getErrorStream()));
                  StringBuilder errorMsg = new StringBuilder();
                  String line;
                  while ((line = errorReader.readLine()) != null) errorMsg.append(line);
                  errorReader.close();
                  authConn.disconnect();

                  handler.post(
                      () -> {
                        append("❌ 授权验证失败：" + errorMsg.toString() + "\n");
                        if (runButton != null) {
                          runButton.setBackground((GradientDrawable) runButton.getTag());
                        }
                        isDownloading = false;
                        updateRunButton();
                      });
                  return;
                }

                // 解析服务器返回
                BufferedReader authReader =
                    new BufferedReader(new InputStreamReader(authConn.getInputStream()));
                StringBuilder authJson = new StringBuilder();
                String line;
                while ((line = authReader.readLine()) != null) authJson.append(line);
                authReader.close();
                authConn.disconnect();

                JSONObject authResult = new JSONObject(authJson.toString());
                String token = authResult.getString("token");
                String scriptDownloadUrl = authResult.getString("scriptUrl");
                String serverScriptHash = authResult.optString("scriptHash", "");

                handler.post(() -> append("✅ 授权通过，正在下载脚本...\n"));

                // ========== 第二步：用 Token 下载脚本 ==========
                URL url = new URL(scriptDownloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                // HTTPS 证书忽略（如果不需要可以删掉）
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
                  final int p = progress;
                  handler.post(
                      () -> {
                        if (runButton != null) updateDownloadProgress(runButton, p);
                      });
                }

                out.flush();
                out.close();
                in.close();
                conn.disconnect();

                // ========== 第三步：重命名并校验哈希 ==========
                tempFile.renameTo(scriptFile);
                RunnerSupport.chmod777(scriptFile);

                if (!serverScriptHash.isEmpty()) {
                  String localHash = getFileSha256(scriptFile);
                  if (!localHash.equals(serverScriptHash)) {
                    scriptFile.delete();
                    handler.post(
                        () -> {
                          append("❌ 脚本文件异常（哈希不匹配），已拦截\n");
                          if (runButton != null) {
                            runButton.setBackground((GradientDrawable) runButton.getTag());
                          }
                          isDownloading = false;
                          updateRunButton();
                        });
                    return;
                  }
                }

                // 保存哈希供运行时校验
               final String hashToSave = serverScriptHash;
handler.post(
    () -> {
      SharedPreferences.Editor editor =
          getSharedPreferences(PREFS, MODE_PRIVATE).edit();
      editor.putString("expected_script_hash", hashToSave);
      editor.apply();

                      selectedFile = scriptFile;
                      selectedName = SCRIPT_NAME;
                      scriptReady = true;
                      isDownloading = false;
                      append("运行脚本准备完毕\n");
                      if (runButton != null) {
                        runButton.setBackground((GradientDrawable) runButton.getTag());
                      }
                      updateRunButton();
                    });

              } catch (Exception e) {
                handler.post(
                    () -> {
                      append("❌ 下载失败：" + e.getMessage() + "\n");
                      if (runButton != null) {
                        runButton.setBackground((GradientDrawable) runButton.getTag());
                      }
                      isDownloading = false;
                      updateRunButton();
                    });
              }
            })
        .start();
  }

  // ========== 🛡️ 运行前校验 C++ 二进制文件身份 ==========
  private boolean verifyBinarySignature(File binaryFile) {
    try {
      FileInputStream fis = new FileInputStream(binaryFile);
      byte[] buffer = new byte[(int) binaryFile.length()];
      fis.read(buffer);
      fis.close();

      // 在二进制文件中搜索身份标记字符串
      String marker = "AURAKERNEL_V1_";
      byte[] markerBytes = marker.getBytes();

      // 在文件内容中搜索标记
      for (int i = 0; i <= buffer.length - markerBytes.length; i++) {
        boolean found = true;
        for (int j = 0; j < markerBytes.length; j++) {
          if (buffer[i + j] != markerBytes[j]) {
            found = false;
            break;
          }
        }
        if (found) {
          // 找到标记，再检查后面的签名哈希是否正确
          String sigMarker = new String(buffer, i + markerBytes.length, 44); // Base64长度
          String expectedSig = "VlryMOwxJbfP9KYssSuiM+dc0b/OP76mq7JqbJiVHIM=";
          return sigMarker.equals(expectedSig);
        }
      }
      return false; // 没找到标记
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 更新按钮下载进度（左绿右灰，圆角）
   *
   * @param btn 目标按钮
   * @param progress 进度 0-100
   */
  private void updateDownloadProgress(Button btn, int progress) {
    if (btn == null) return;
    btn.post(
        () -> {
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
          LayerDrawable layer = new LayerDrawable(new Drawable[] {grayBg, clip});
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

  private int getStatusBarHeight() {
  int result = 0;
  int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
  if (resourceId > 0) {
    result = getResources().getDimensionPixelSize(resourceId);
  }
  return result;
}


  private View createHomePage() {
    // 【修复】外层嵌套 ScrollView，整个主页支持全局滚动（兼容小窗/分屏）
    ScrollView rootScroll = new ScrollView(this);
    rootScroll.setVerticalScrollBarEnabled(false);
    rootScroll.setFillViewport(false);
    rootScroll.setBackgroundColor(bgColor());

    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(18), dp(18) + getStatusBarHeight(), dp(18), dp(8));
    page.setBackgroundColor(bgColor());
    // 把原page装入外层ScrollView
    rootScroll.addView(page, new ScrollView.LayoutParams(-1, -2));

    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.HORIZONTAL);
    header.setGravity(Gravity.CENTER_VERTICAL);
    page.addView(header, lp(-1, -2, 0, 0, 0, dp(14)));

    ImageView avatarView = new ImageView(this);
    avatarView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    avatarView.setBackground(round(cardColor(), 23, borderColor(), 1));
    LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(46), dp(46));
    header.addView(avatarView, avatarLp);
    // 异步加载随机头像
    loadRandomAvatar(avatarView);


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

    // 2. 驱动选择模块 - 独立卡片（横向滑动版）
    LinearLayout cardDriver = new LinearLayout(this);
    cardDriver.setOrientation(LinearLayout.VERTICAL);
    cardDriver.setPadding(dp(16), dp(16), dp(16), dp(16));
    cardDriver.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(cardDriver, lp(-1, -2, 0, 0, 0, dp(14)));

    TextView driverTitle = text("选择驱动", 15, textColor(), Typeface.BOLD);
    cardDriver.addView(driverTitle, lp(-1, -2, 0, 0, 0, dp(8)));

    // ✅ 横向滑动容器（解决拥挤问题）
    HorizontalScrollView driverScroll = new HorizontalScrollView(this);
    driverScroll.setHorizontalScrollBarEnabled(false); // 隐藏滚动条
    driverScroll.setOverScrollMode(View.OVER_SCROLL_NEVER); // 去掉边缘光晕
    cardDriver.addView(driverScroll, lp(-1, dp(42), 0, 0, 0, 0));

    // 滑动内部的按钮容器
    LinearLayout driverRow = new LinearLayout(this);
    driverRow.setOrientation(LinearLayout.HORIZONTAL);
    driverRow.setGravity(Gravity.CENTER_VERTICAL);
    driverRow.setPadding(dp(8), 0, dp(8), 0); // 左右内边距，避免按钮贴边
    driverScroll.addView(driverRow, new HorizontalScrollView.LayoutParams(-2, -1));

    // 按钮通用布局参数（固定宽度+间距，不再挤压）
    LinearLayout.LayoutParams driverBtnLp = new LinearLayout.LayoutParams(dp(120), -1);
    driverBtnLp.setMargins(dp(4), 0, dp(4), 0); // 按钮之间左右间距4dp

    // 初始化4个驱动按钮（顺序不变）
    driverBtnKpm = driverOptionButton("KMA-KPM驱动", driverType == 0);
    driverBtnDitpro = driverOptionButton("Ditpro_KPM驱动", driverType == 1);
    driverBtnParadise = driverOptionButton("Paradise驱动", driverType == 2);
    driverBtnBackup = driverOptionButton("备用驱动", driverType == 3);

    // 添加到滑动容器
    driverRow.addView(driverBtnKpm, driverBtnLp);
    driverRow.addView(driverBtnDitpro, driverBtnLp);
    driverRow.addView(driverBtnParadise, driverBtnLp);
    driverRow.addView(driverBtnBackup, driverBtnLp);

    // 点击事件（编号严格对应）
    driverBtnKpm.setOnClickListener(v -> selectDriverType(0));
    driverBtnDitpro.setOnClickListener(v -> selectDriverType(1));
    driverBtnParadise.setOnClickListener(v -> selectDriverType(2));
    driverBtnBackup.setOnClickListener(v -> selectDriverType(3));

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

    // ====================== 终端固定窗口大小（380dp × 240dp） ======================
    terminalScroll = new ScrollView(this);
    terminalScroll.setFillViewport(true);
    // 禁止横向滚动，确保文字自动换行
    terminalScroll.setHorizontalScrollBarEnabled(false);
    terminalScroll.setVerticalScrollBarEnabled(false);
    terminalScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

    // 毛玻璃效果背景（半透明磨砂+圆角）+ 科技绿细边框
    GradientDrawable terminalBg =
        new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {Color.argb(180, 10, 15, 22), Color.argb(160, 5, 8, 12)});
    terminalBg.setCornerRadius(dp(18));
    terminalBg.setStroke(dp(1), Color.argb(30, 81, 191, 101));
    terminalScroll.setBackground(terminalBg);

    outputView = text(outputBuffer.toString(), 12, Color.rgb(81, 191, 101), Typeface.NORMAL);
    outputView.setTypeface(Typeface.MONOSPACE);
    outputView.getPaint().setAntiAlias(true);
    outputView.setLineSpacing(dp(4), 1.0f);
    outputView.setLetterSpacing(0.02f);
    outputView.setPadding(dp(16), dp(16), dp(16), dp(16));
    // 强制文字在固定宽度内自动换行
    outputView.setSingleLine(false);
    outputView.setHorizontallyScrolling(false);

    terminalScroll.addView(outputView, new ScrollView.LayoutParams(-1, -2));

    // ✅ 核心：固定宽高 + 水平居中
    // 可直接修改以下两个数值调整终端大小（单位：dp）
    int TERMINAL_WIDTH = 380; // 固定宽度
    int TERMINAL_HEIGHT = 200; // 固定高度

    // 小屏保护：如果屏幕宽度小于设定值，则自动占满屏幕宽度
    DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
    int finalWidth =
        screenWidthDp < TERMINAL_WIDTH
            ? LinearLayout.LayoutParams.MATCH_PARENT
            : dp(TERMINAL_WIDTH);

    LinearLayout.LayoutParams terminalLp =
        new LinearLayout.LayoutParams(finalWidth, dp(TERMINAL_HEIGHT));
    terminalLp.gravity = Gravity.CENTER_HORIZONTAL; // 水平居中
    terminalLp.setMargins(0, dp(14), 0, 0); // 顶部间距与其他卡片统一
    page.addView(terminalScroll, terminalLp);

    updateRunButton();
    scrollTerminalBottom();
    // 【最后】返回外层滚动容器，而非原page
    return rootScroll;
  }

  private void loadRandomAvatar(final ImageView imageView) {
  // ★ 如果已经有缓存，直接显示，不再请求网络
  if (cachedAvatar != null) {
imageView.setImageBitmap(getCircularBitmap(cachedAvatar));
imageView.setBackground(null);
    return;
  }

  final String avatarUrl = StringGuard.get(9);
  if (avatarUrl.isEmpty()) return;

  new Thread(
      () -> {
        try {
          URL url = new URL(avatarUrl);
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setConnectTimeout(8000);
          conn.setReadTimeout(8000);
          Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());
          conn.disconnect();

          if (bitmap != null) {
            // ★ 存入缓存
            cachedAvatar = bitmap;

            runOnUiThread(
                () -> {
imageView.setImageBitmap(getCircularBitmap(bitmap));
imageView.setBackground(null);
                });
          }
        } catch (Exception e) {
          Log.w("AVATAR", "加载头像失败: " + e.getMessage());
        }
      })
      .start();
}

private Bitmap getCircularBitmap(Bitmap bitmap) {
  int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
  float r = size / 2f;
  Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
  Canvas canvas = new Canvas(output);
  Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  canvas.drawCircle(r, r, r, paint);
  paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
  canvas.drawBitmap(bitmap, new android.graphics.Rect(0, 0, size, size), new android.graphics.RectF(0, 0, size, size), paint);
  return output;
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
    // 1. KMA-KPM驱动（索引0）
    if (driverBtnKpm != null) {
      boolean active = driverType == 0;
      driverBtnKpm.setTextColor(active ? Color.WHITE : subTextColor());
      driverBtnKpm.setBackground(
          round(
              active ? primaryColor() : tagColor(),
              16,
              active ? 0 : borderColor(),
              active ? 0 : 1));
    }

    // 2. Ditpro_KPM驱动（索引1）
    if (driverBtnDitpro != null) {
      boolean active = driverType == 1;
      driverBtnDitpro.setTextColor(active ? Color.WHITE : subTextColor());
      driverBtnDitpro.setBackground(
          round(
              active ? primaryColor() : tagColor(),
              16,
              active ? 0 : borderColor(),
              active ? 0 : 1));
    }

    // 3. Paradise驱动（索引2）
    if (driverBtnParadise != null) {
      boolean active = driverType == 2;
      driverBtnParadise.setTextColor(active ? Color.WHITE : subTextColor());
      driverBtnParadise.setBackground(
          round(
              active ? primaryColor() : tagColor(),
              16,
              active ? 0 : borderColor(),
              active ? 0 : 1));
    }

    // 4. 备用驱动（索引3）
    if (driverBtnBackup != null) {
      boolean active = driverType == 3;
      driverBtnBackup.setTextColor(active ? Color.WHITE : subTextColor());
      driverBtnBackup.setBackground(
          round(
              active ? primaryColor() : tagColor(),
              16,
              active ? 0 : borderColor(),
              active ? 0 : 1));
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
    page.setPadding(dp(20), dp(22) + getStatusBarHeight(), dp(20), dp(20));
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
        URL url = new URL(StringGuard.get(1));
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

    // 清除日志按钮
    final Button clearBtn = createModernButton("清除日志", Color.rgb(100, 149, 237));
    // 复制输出按钮
    final Button copyBtn = createModernButton("复制输出", Color.rgb(81, 191, 101));
    // 关闭按钮
    final Button closeBottomBtn = createModernButton("关闭", Color.rgb(255, 80, 80));

    // 按钮布局权重
    LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, dp(50), 1);
    btnLp.setMargins(dp(6), 0, dp(6), 0);
    bottomBar.addView(clearBtn, btnLp);
    bottomBar.addView(copyBtn, btnLp);
    bottomBar.addView(closeBottomBtn, btnLp);

    rootLayout.addView(bottomBar, new LinearLayout.LayoutParams(-1, -2));

    closeBottomBtn.setOnClickListener(v -> dialog.dismiss());
    // 清除日志按钮
    clearBtn.setOnClickListener(
        v -> {
          outputBuffer.setLength(0);
          outputView.setText("▶ 终端已清空\n");
        });

    // 复制输出按钮
    copyBtn.setOnClickListener(
        v -> {
          android.content.ClipboardManager clipboard =
              (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("驱动终端日志", outputView.getText().toString());
          clipboard.setPrimaryClip(clip);
          Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

    // 关闭按钮
    closeBottomBtn.setOnClickListener(v -> dialog.dismiss());

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
        });

    // 清除日志按钮
    clearBtn.setOnClickListener(
        v -> {
          outputBuffer.setLength(0);
          outputView.setText("▶ 终端已清空\n");
        });

    // 复制输出按钮
    copyBtn.setOnClickListener(
        v -> {
          android.content.ClipboardManager clipboard =
              (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("驱动终端日志", outputView.getText().toString());
          clipboard.setPrimaryClip(clip);
          Toast.makeText(this, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });

    // 弹窗关闭销毁进程
    // 弹窗关闭
    dialog.setOnDismissListener(
        di -> {
          running = false;
          updateRunButton();
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
    page.setPadding(dp(20), dp(22) + getStatusBarHeight(), dp(20), dp(20));
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
    View cleanConfigRow = settingRow("清理配置", "删除AuraKernel选择配置文件", "⚙️");
    cleanCard.addView(cleanConfigRow);
    cleanConfigRow.setOnClickListener(
        v -> {
          new Thread(
                  () -> {
                    File configFile = new File("/storage/emulated/0/AuraKernel/Aura选择配置.json");
                    boolean deleted = false;
                    if (configFile.exists()) {
                      deleted = configFile.delete();
                    }
                    final boolean finalDeleted = deleted;
                    handler.post(
                        () -> {
                          if (finalDeleted) {
                            Toast.makeText(MainActivity.this, "配置文件已删除", Toast.LENGTH_SHORT).show();
                          } else {
                            Toast.makeText(MainActivity.this, "配置文件不存在或删除失败", Toast.LENGTH_SHORT)
                                .show();
                          }
                        });
                  })
              .start();
        });
    addDivider(cleanCard);

    // 2. 清理内核&驱动
    View cleanKernelDriverRow = settingRow("清理内核&驱动", "清除已刷入内核、驱动文件", "🧹");
    cleanCard.addView(cleanKernelDriverRow);
    cleanKernelDriverRow.setOnClickListener(
        v -> {
          new Thread(
                  () -> {
                    // 删除drivers目录下所有内容，但保留clean目录
                    File driversDir = new File(getFilesDir(), "drivers");
                    boolean success = deleteDirContents(driversDir);
                    handler.post(
                        () -> {
                          if (success) {
                            Toast.makeText(MainActivity.this, "内核和驱动已清理", Toast.LENGTH_SHORT)
                                .show();
                            // 刷新驱动页面列表
                            if (currentPage == 1) {
                              refreshDriverFileList();
                            }
                          } else {
                            Toast.makeText(MainActivity.this, "清理失败", Toast.LENGTH_SHORT).show();
                          }
                        });
                  })
              .start();
        });
    addDivider(cleanCard);

    // 3. 低级清理
    View cleanLowRow = settingRow("低级清理", "清理临时缓存、运行日志", "🧽");
    cleanCard.addView(cleanLowRow);
    cleanLowRow.setOnClickListener(
        v -> {
          runCleanScript("clean_low.sh", "低级清理");
        });
    addDivider(cleanCard);

    // 4. 中级清理
    View cleanMidRow = settingRow("中级清理", "清理残留配置、冗余文件", "🗑️");
    cleanCard.addView(cleanMidRow);
    cleanMidRow.setOnClickListener(
        v -> {
          runCleanScript("clean_mid.sh", "中级清理");
        });
    addDivider(cleanCard);

    // 5. 高级清理
    View cleanHighRow = settingRow("高级清理", "深度清理全部残留数据", "🔥");
    cleanCard.addView(cleanHighRow);
    cleanHighRow.setOnClickListener(
        v -> {
          runCleanScript("clean_high.sh", "高级清理");
        });
    addDivider(cleanCard);

    // 6. 更改ID
    View changeIdRow = settingRow("更改ID", "修改设备/应用标识ID", "🔢");
    cleanCard.addView(changeIdRow);
    changeIdRow.setOnClickListener(
        v -> {
          runCleanScript("change_id.sh", "更改ID");
        });
    addDivider(cleanCard);

    // 7. 清理说明
    View cleanDescRow = settingRow("清理说明", "查看各级清理功能介绍", "ℹ️");
    cleanCard.addView(cleanDescRow);
    cleanDescRow.setOnClickListener(
        v -> {
          Toast.makeText(MainActivity.this, "清理说明开发中", Toast.LENGTH_SHORT).show();
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

  /** 系统信息专用行布局：左侧图标 + 标题 + 数值(右对齐) → 无右侧箭头 */
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

    // ===== 🛡️ 运行前再校验一次签名 =====
    if (!SignatureGuard.isSignatureValid(this)) {
      append("❌ 应用签名校验失败，已阻止执行\n");
      return;
    }
    // ====================================
    if (selectedFile == null) {
      if (!scriptReady && isNetworkAvailable()) {
        // 脚本未就绪，触发重新下载
        prepareScriptIfNeeded();
        return;
      }
      Toast.makeText(this, "请先等待脚本准备完毕", Toast.LENGTH_SHORT).show();
      return;
    }

    if (running) return;

  // ================= 🛡️ 运行时哈希校验 =================
// 只校验一次，从 prefs 读取服务端下发的哈希
SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
String expectedHash = prefs.getString("expected_script_hash", "");

if (!expectedHash.isEmpty()) {
  try {
    String localHash = getFileSha256(selectedFile);
    if (!localHash.equals(expectedHash)) {
      append("ℹ️ 检测到脚本已更新，正在重新获取...\n");
      selectedFile.delete();
      selectedFile = null;
      scriptReady = false;
      updateRunButton();
      prepareScriptIfNeeded();  // 重新下载
      return;
    }
  } catch (Exception e) {
    append("❌ 无法校验文件完整性: " + e.getMessage() + "\n");
    return;
  }
} else {
  append("⚠️ 跳过校验（首次运行或网络不可用）\n");
}
    // ==========================================================

    outputBuffer.setLength(0);
    if (outputView != null) outputView.setText("");
    running = true;
    updateRunButton();
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
    String driverNum;
    switch (driverType) {
      case 0:
        driverNum = "1";
        break; // KMA-KPM驱动 → 输出1
      case 1:
        driverNum = "2";
        break; // Ditpro_KPM驱动 → 输出2
      case 2:
        driverNum = "3";
        break; // Paradise驱动 → 输出3
      case 3:
        driverNum = "4";
        break; // 备用驱动 → 输出4
      default:
        driverNum = "1";
    }
    int baseDelay = 600;

    String antiNum = antiRecord ? "1" : "2";
    String bgNum = noBackground ? "2" : "1";

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
    String label;
    switch (driverType) {
      case 0:
        label = "直接运行 (KPM)";
        break;
      case 1:
        label = "直接运行 (Ditpro_KPM)";
        break;
      case 2:
        label = "直接运行 (Paradise)";
        break;
      case 3:
        label = "直接运行 (备用)";
        break;
      default:
        label = "直接运行";
    }
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

  // 计算文件SHA-256哈希
  private String getFileSha256(File file) throws Exception {
    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
    FileInputStream fis = new FileInputStream(file);
    byte[] buffer = new byte[8192];
    int len;
    while ((len = fis.read(buffer)) != -1) {
      md.update(buffer, 0, len);
    }
    fis.close();
    byte[] digest = md.digest();
    return Base64.getEncoder().encodeToString(digest);
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
        MainActivity activity = activityRef.get();
        if (activity == null) return null;

        // 获取签名哈希
        String signatureHash = SignatureGuard.getApkSignatureHashBase64(activity);

        // 请求服务器验证
        URL url = new URL(StringGuard.get(10));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);

        JSONObject requestBody = new JSONObject();
        requestBody.put("signature", signatureHash);
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(requestBody.toString());
        writer.flush();
        writer.close();

        if (conn.getResponseCode() != 200) {
          conn.disconnect();
          return null; // 盗版，不返回任何更新信息
        }

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
        String apkUrlRaw = json.optString("apkUrl", "");
        String apkUrl =
            apkUrlRaw.startsWith("http") ? apkUrlRaw : StringGuard.decrypt(apkUrlRaw); // ← 解密后再用
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

        // 修复版本比较：优先用 versionCode 数字比较
        if (newVersionCode > currentVersionCode) {
          activity.showUpdateDialog(newVersionName, desc, apkUrl);
        }
        // 如果 versionCode 相同但 versionName 不同，也视为有新版本
        else if (!newVersionName.equals(currentVersionName)) {
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

  // ====================== 【美化版】显示更新弹窗 ======================
  private void showUpdateDialog(String newVersion, String desc, final String apkUrl) {
    // 创建自定义弹窗
    final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

    // 根布局
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.TRANSPARENT);
    root.setGravity(Gravity.CENTER);
    root.setPadding(dp(24), dp(24), dp(24), dp(24));

    // 卡片容器
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(
        round(
            nightMode ? Color.rgb(28, 32, 44) : Color.rgb(250, 250, 252),
            dp(20),
            nightMode ? Color.rgb(48, 54, 70) : Color.rgb(225, 230, 240),
            1));
    card.setPadding(dp(24), dp(28), dp(24), dp(24));
    card.setGravity(Gravity.CENTER_HORIZONTAL);

    int accentColor = Color.rgb(22, 119, 255); // 主题蓝

    // ===== 顶部图标 =====
    TextView iconView = new TextView(this);
    iconView.setText("🎉");
    iconView.setTextSize(48);
    iconView.setGravity(Gravity.CENTER);
    card.addView(iconView, lp(-1, -2, 0, 0, 0, dp(8)));

    // ===== 标题 =====
    TextView titleView = new TextView(this);
    titleView.setText("发现新版本");
    titleView.setTextSize(22);
    titleView.setTypeface(Typeface.DEFAULT_BOLD);
    titleView.setTextColor(nightMode ? Color.rgb(225, 228, 235) : Color.rgb(30, 32, 42));
    titleView.setGravity(Gravity.CENTER);
    card.addView(titleView, lp(-1, -2, 0, 0, 0, dp(4)));

    // ===== 版本号标签 =====
    TextView versionView = new TextView(this);
    versionView.setText("v" + newVersion);
    versionView.setTextSize(14);
    versionView.setTypeface(Typeface.DEFAULT_BOLD);
    versionView.setTextColor(Color.WHITE);
    versionView.setGravity(Gravity.CENTER);
    versionView.setBackground(round(accentColor, dp(14), 0, 0));
    versionView.setPadding(dp(20), dp(6), dp(20), dp(6));
    LinearLayout.LayoutParams vlp = lp(-2, -2, 0, 0, 0, dp(16));
    card.addView(versionView, vlp);

    // ===== 更新内容区域 =====
    ScrollView descScroll = new ScrollView(this);
    descScroll.setVerticalScrollBarEnabled(false);

    TextView descView = new TextView(this);
    descView.setText(desc);
    descView.setTextSize(14);
    descView.setLineSpacing(dp(4), 1f);
    descView.setTextColor(nightMode ? Color.rgb(180, 188, 200) : Color.rgb(80, 86, 102));
    descView.setPadding(dp(4), dp(8), dp(4), dp(8));
    descScroll.addView(descView, lp(-1, -2, 0, 0, 0, 0));
    card.addView(descScroll, lp(-1, -2, 0, 0, 0, dp(20)));

    // ===== 按钮区域 =====
    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.CENTER);

    // 稍后再说按钮
    Button laterBtn = new Button(this);
    laterBtn.setText("稍后再说");
    laterBtn.setTextSize(14);
    laterBtn.setTypeface(Typeface.DEFAULT_BOLD);
    laterBtn.setTextColor(nightMode ? Color.rgb(148, 157, 174) : Color.rgb(103, 114, 132));
    laterBtn.setBackground(
        round(
            Color.TRANSPARENT,
            dp(12),
            nightMode ? Color.rgb(48, 54, 70) : Color.rgb(215, 220, 230),
            1));
    laterBtn.setPadding(0, dp(12), 0, dp(12));
    laterBtn.setOnClickListener(v -> dialog.dismiss());

    LinearLayout.LayoutParams laterLp = new LinearLayout.LayoutParams(0, dp(46), 1);
    laterLp.setMargins(0, 0, dp(8), 0);
    btnRow.addView(laterBtn, laterLp);

    // 立即更新按钮
    Button updateBtn = new Button(this);
    updateBtn.setText("🚀 立即更新");
    updateBtn.setTextSize(14);
    updateBtn.setTypeface(Typeface.DEFAULT_BOLD);
    updateBtn.setTextColor(Color.WHITE);
    updateBtn.setBackground(round(accentColor, dp(12), 0, 0));
    updateBtn.setPadding(0, dp(12), 0, dp(12));
    updateBtn.setOnClickListener(
        v -> {
          dialog.dismiss();
          downloadApk(apkUrl);
        });

    LinearLayout.LayoutParams updateLp = new LinearLayout.LayoutParams(0, dp(46), 1);
    updateLp.setMargins(dp(8), 0, 0, 0);
    btnRow.addView(updateBtn, updateLp);

    card.addView(btnRow, lp(-1, -2, 0, 0, 0, 0));

    root.addView(card, lp(-1, -2, 0, 0, 0, 0));

    dialog.setContentView(root);
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    dialog
        .getWindow()
        .setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
  }

  // ====================== 带进度条的下载更新（修复版） ======================
  private void downloadApk(String apkUrl) {
    if (apkUrl == null || apkUrl.isEmpty()) {
      Toast.makeText(this, "下载链接无效", Toast.LENGTH_SHORT).show();
      return;
    }

    // 创建进度弹窗
    final Dialog progressDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
    progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    progressDialog.setCancelable(false);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.TRANSPARENT);
    root.setGravity(Gravity.CENTER);
    root.setPadding(dp(24), dp(24), dp(24), dp(24));

    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackground(
        round(
            nightMode ? Color.rgb(28, 32, 44) : Color.rgb(250, 250, 252),
            dp(20),
            nightMode ? Color.rgb(48, 54, 70) : Color.rgb(225, 230, 240),
            1));
    card.setPadding(dp(28), dp(28), dp(28), dp(28));
    card.setGravity(Gravity.CENTER);

    // 标题
    TextView titleView = new TextView(this);
    titleView.setText("⏬ 正在下载更新");
    titleView.setTextSize(18);
    titleView.setTypeface(Typeface.DEFAULT_BOLD);
    titleView.setTextColor(nightMode ? Color.rgb(225, 228, 235) : Color.rgb(30, 32, 42));
    titleView.setGravity(Gravity.CENTER);
    card.addView(titleView, lp(-1, -2, 0, 0, 0, dp(16)));

    // 进度条
    ProgressBar progressBar =
        new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progressBar.setMax(100);
    progressBar.setProgress(0);
    progressBar
        .getProgressDrawable()
        .setColorFilter(Color.rgb(22, 119, 255), android.graphics.PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams pblp =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(22));
    pblp.setMargins(0, 0, 0, dp(10));
    card.addView(progressBar, pblp);

    // 百分比文字
    TextView percentView = new TextView(this);
    percentView.setText("0%");
    percentView.setTextSize(14);
    percentView.setTypeface(Typeface.DEFAULT_BOLD);
    percentView.setTextColor(Color.rgb(22, 119, 255));
    percentView.setGravity(Gravity.CENTER);
    card.addView(percentView, lp(-1, -2, 0, 0, 0, dp(4)));

    // 提示文字
    TextView tipView = new TextView(this);
    tipView.setText("请勿关闭此页面...");
    tipView.setTextSize(12);
    tipView.setTextColor(Color.rgb(107, 114, 128));
    tipView.setGravity(Gravity.CENTER);
    card.addView(tipView, lp(-1, -2, 0, dp(4), 0, 0));

    root.addView(card, lp(-1, -2, 0, 0, 0, 0));
    progressDialog.setContentView(root);
    progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    progressDialog
        .getWindow()
        .setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
    progressDialog.show();

    // 开始下载
    new AsyncTask<Void, Integer, File>() {
      private Exception error = null;

      @Override
      protected File doInBackground(Void... voids) {
        try {
          File downloadDir = new File(getCacheDir(), "update");
          if (!downloadDir.exists()) downloadDir.mkdirs();
          File apkFile = new File(downloadDir, "Aurakernel_update.apk");
          if (apkFile.exists()) apkFile.delete();

          URL url = new URL(apkUrl);
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setConnectTimeout(15000);
          conn.setReadTimeout(30000);
          conn.connect();

          int totalSize = conn.getContentLength();
          FileOutputStream fos = new FileOutputStream(apkFile);
          BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());

          byte[] buf = new byte[8192];
          int len;
          int downloaded = 0;
          while ((len = bis.read(buf)) != -1) {
            fos.write(buf, 0, len);
            downloaded += len;
            if (totalSize > 0) {
              int progress = (int) ((downloaded / (float) totalSize) * 100);
              publishProgress(progress);
            }
          }
          fos.flush();
          fos.close();
          bis.close();
          conn.disconnect();

          return apkFile;
        } catch (Exception e) {
          error = e;
          return null;
        }
      }

      @Override
      protected void onProgressUpdate(Integer... values) {
        int p = values[0];
        progressBar.setProgress(p);
        percentView.setText(p + "%");
        tipView.setText(p < 50 ? "正在下载，请稍候..." : "即将下载完成...");
      }

      @Override
      protected void onPostExecute(File apkFile) {
        progressDialog.dismiss();

        if (error != null || apkFile == null || !apkFile.exists()) {
          Toast.makeText(
                  MainActivity.this,
                  "❌ 下载失败: " + (error != null ? error.getMessage() : "未知错误"),
                  Toast.LENGTH_LONG)
              .show();
          return;
        }

        Toast.makeText(MainActivity.this, "✅ 下载完成，准备安装...", Toast.LENGTH_SHORT).show();
        installApk(apkFile);
      }
    }.execute();
  }

  // ====================== 安装 APK（兼容所有版本） ======================
  private void installApk(File apkFile) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Android 8.0+ 用 PackageInstaller
        // 先检查是否有安装未知应用的权限
        if (!getPackageManager().canRequestPackageInstalls()) {
          // 没有权限，引导用户去开启
          Toast.makeText(this, "⚠️ 需要允许安装未知来源应用", Toast.LENGTH_LONG).show();
          Intent intent =
              new Intent(
                  Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                  Uri.parse("package:" + getPackageName()));
          startActivityForResult(intent, 1001);
          return;
        }
      }

      // 使用 FileProvider 安装
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // Android 7.0+ 必须用 FileProvider
        Uri apkUri =
            androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", apkFile);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } else {
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
      }

      startActivity(intent);

    } catch (Exception e) {
      // 如果 FileProvider 方式失败，试试用旧方式
      try {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
      } catch (Exception e2) {
        Toast.makeText(
                this, "⚠️ 安装失败，请在文件管理器中手动安装：\n" + apkFile.getAbsolutePath(), Toast.LENGTH_LONG)
            .show();
        Log.e("INSTALL", "安装失败", e2);
      }
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
    if (terminalScroll != null && outputView != null) {
      terminalScroll.post(
          () -> {
            // 精确滚动到底部，修复固定宽度后滚动不准的问题
            int scrollY = outputView.getBottom() - terminalScroll.getHeight();
            if (scrollY > 0) {
              terminalScroll.scrollTo(0, scrollY);
            } else {
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