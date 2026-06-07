package com.Aurakernel;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import android.view.animation.ScaleAnimation;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends Activity {
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

  private FrameLayout root;
  private LinearLayout pageHost;
  private TextView navHome;
  private TextView navMine;
  private TextView driverBtnKpm, driverBtnParadise, driverBtnBackup;
  private TextView antiRecordBtn, noBackgroundBtn;
  //   private TextView fileNameView;
  //   private TextView driverNameView;
  //   private TextView modeNoDriverView;
  //   private TextView modeDriverView;
  //   private LinearLayout driverBlock;
  private TextView outputView;
  private ScrollView terminalScroll;
  //   private EditText inputEdit;
  private Button runButton;
  //   private Button sendButton;
  private Button stopButton;
  private EditText keyEdit;

  private File selectedFile;
  //   private File driverZipFile;
  private String selectedName = "";
  //   private String driverZipName = "";
  //   private String matchedDriverName = "";
  private boolean running = false;
  private boolean nightMode = false;
  //   private boolean driverMode = false;
  private int currentPage = 0;

  private Process runningProcess;
  private BufferedWriter processWriter;
  private String activeRootPath = "";

  private final Handler handler = new Handler();
  private final StringBuilder outputBuffer = new StringBuilder("就绪\n");

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

  // /**
  //  * 异步下载远程脚本并自动运行
  //  */
  // private void downloadAndRunRemoteScript() {
  //     if (running) {
  //         Toast.makeText(this, "已有程序在运行", Toast.LENGTH_SHORT).show();
  //         return;
  //     }

  //     // 更新UI反馈
  //     append("正在下载远程脚本...\n");
  //     if (runButton != null) runButton.setText("下载中...");
  //     if (runButton != null) runButton.setEnabled(false);

  //     new Thread(new Runnable() {
  //         @Override
  //         public void run() {
  //             File tempFile = null;
  //             try {
  //                 // 下载到应用私有临时目录
  //                 File dir = new File(getCacheDir(), "remote_runner");
  //                 if (!dir.exists()) dir.mkdirs();
  //                 tempFile = new File(dir, REMOTE_SCRIPT_NAME + ".download");

  //                 // 使用 HttpURLConnection 下载
  //                 java.net.URL url = new java.net.URL(REMOTE_SCRIPT_URL);
  //                 java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
  // url.openConnection();
  //                 conn.setConnectTimeout(15000);
  //                 conn.setReadTimeout(30000);
  //                 conn.setRequestMethod("GET");
  //                 conn.setInstanceFollowRedirects(true);

  //                 int code = conn.getResponseCode();
  //                 if (code != 200) {
  //                     throw new Exception("服务器返回非200状态码: " + code);
  //                 }

  //                 InputStream in = new BufferedInputStream(conn.getInputStream());
  //                 FileOutputStream out = new FileOutputStream(tempFile);
  //                 byte[] buf = new byte[8192];
  //                 int len;
  //                 while ((len = in.read(buf)) != -1) {
  //                     out.write(buf, 0, len);
  //                 }
  //                 out.flush();
  //                 out.close();
  //                 in.close();
  //                 conn.disconnect();

  //                 // 重命名为正式文件名
  //                 File finalFile = new File(dir, REMOTE_SCRIPT_NAME);
  //                 if (finalFile.exists()) finalFile.delete();
  //                 tempFile.renameTo(finalFile);

  //                 // 确保可执行（虽然 .sh 最终由 sh 解释执行，但无害）
  //                 RunnerSupport.chmod777(finalFile);

  //                 // 在主线程更新 selectedFile 并触发运行
  //                 handler.post(new Runnable() {
  //                     @Override
  //                     public void run() {
  //                         selectedFile = finalFile;
  //                         selectedName = REMOTE_SCRIPT_NAME;
  //                         if (fileNameView != null) {
  //                             fileNameView.setText(finalFile.getAbsolutePath());
  //                         }
  //                         append("远程脚本下载完成: " + finalFile.getAbsolutePath() + "\n");
  //                         updateRunButton();
  //                         // 自动开始运行（如果你想点击按钮后再运行，可以注释掉下面这行）
  //                         runSelectedFile();
  //                     }
  //                 });

  //             } catch (final Exception e) {
  //                 final String errMsg = safeMessage(e);
  //                 handler.post(new Runnable() {
  //                     @Override
  //                     public void run() {
  //                         append("远程脚本下载失败: " + errMsg + "\n");
  //                         updateRunButton();
  //                         Toast.makeText(MainActivity.this, "下载失败: " + errMsg,
  // Toast.LENGTH_LONG).show();
  //                     }
  //                 });
  //                 // 清理失败的临时文件
  //                 if (tempFile != null && tempFile.exists()) tempFile.delete();
  //             }
  //         }
  //     }).start();
  // }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    try {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    } catch (Exception ignored) {
    }

    SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
    nightMode = sp.getBoolean("night_mode", false);
    driverType = sp.getInt("driver_type", 0); // 新增
    antiRecord = sp.getBoolean("anti_record", false); // 新增
    noBackground = sp.getBoolean("no_background", false); // 新增
    // driverMode = sp.getBoolean("driver_mode", false);
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
    // 显示正在检查的提示
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
                      // 有 Root 权限，正常进入
                      showSplashThenMain();
                    } else {
                      Toast.makeText(MainActivity.this, "设备未获取 Root 权限，无法使用", Toast.LENGTH_LONG)
                          .show();
                      finish();
                    }
                  });
            })
        .start();
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

  private void showSplashThenMain() {
    root.removeAllViews();
    root.setBackground(bgGradient());

    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER);
    box.setPadding(dp(30), dp(30), dp(30), dp(30));
    root.addView(box, new FrameLayout.LayoutParams(-1, -1));

    TextView logo = text("W", 44, Color.WHITE, Typeface.BOLD);
    logo.setGravity(Gravity.CENTER);
    logo.setBackground(round(primaryColor(), 30, Color.argb(80, 255, 255, 255), 1));
    LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(86), dp(86));
    box.addView(logo, logoLp);

    TextView name = text("AuraKernel", 32, textColor(), Typeface.BOLD);
    name.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams nlp = lp(-1, -2, 0, dp(22), 0, 0);
    box.addView(name, nlp);

    TextView sub =
        text("Binary Runner · Driver Mode · Live Terminal", 13, subTextColor(), Typeface.NORMAL);
    sub.setGravity(Gravity.CENTER);
    box.addView(sub, lp(-1, -2, 0, dp(8), 0, dp(24)));

    LinearLayout dots = new LinearLayout(this);
    dots.setGravity(Gravity.CENTER);
    box.addView(dots, lp(-1, dp(18), 0, 0, 0, 0));
    for (int i = 0; i < 3; i++) {
      TextView dot = text("", 1, Color.WHITE, Typeface.NORMAL);
      dot.setBackground(round(i == 0 ? primaryColor() : tagColor(), 20, 0, 0));
      LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(28), dp(6));
      dlp.setMargins(dp(4), 0, dp(4), 0);
      dots.addView(dot, dlp);
    }

    AnimationSet set = new AnimationSet(true);
    ScaleAnimation scale =
        new ScaleAnimation(
            0.82f,
            1f,
            0.82f,
            1f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f);
    scale.setDuration(560);
    AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
    alpha.setDuration(560);
    set.addAnimation(scale);
    set.addAnimation(alpha);
    logo.startAnimation(set);
    name.startAnimation(alpha);
    sub.startAnimation(alpha);

    handler.postDelayed(
        new Runnable() {
          public void run() {
            showMainShell();
          }
        },
        900);
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
    if (runButton != null) runButton.setEnabled(false);

    new Thread(
            () -> {
              try {
                if (!scriptDir.exists()) scriptDir.mkdirs();
                File tempFile = new File(scriptDir, SCRIPT_NAME + ".tmp");

                URL url = new URL("https://aura.xiaon.sbs/update/Aurakernel.sh");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                // 如果证书有问题，临时忽略（上线前请申请正规证书）
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

                InputStream in = new BufferedInputStream(conn.getInputStream());
                FileOutputStream out = new FileOutputStream(tempFile);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                out.flush();
                out.close();
                in.close();
                conn.disconnect();

                // 重命名为正式文件
                tempFile.renameTo(scriptFile);
                RunnerSupport.chmod777(scriptFile);

                handler.post(
                    () -> {
                      selectedFile = scriptFile;
                      selectedName = SCRIPT_NAME;
                      scriptReady = true;
                      append("运行脚本准备完毕\n");
                      updateRunButton();
                    });
              } catch (Exception e) {
                handler.post(
                    () -> {
                      append("脚本下载失败: " + e.getMessage() + "\n");
                      updateRunButton();
                    });
              }
            })
        .start();
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
    wrap.addView(bar, new LinearLayout.LayoutParams(dp(230), dp(56)));

    navHome = navButton("主页", true);
    navMine = navButton("我的", false);
    bar.addView(navHome, new LinearLayout.LayoutParams(0, -1, 1));
    bar.addView(navMine, new LinearLayout.LayoutParams(0, -1, 1));
    navHome.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            switchPage(0);
          }
        });
    navMine.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            switchPage(1);
          }
        });
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
    View next = page == 0 ? createHomePage() : createMinePage();
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
    if (navHome != null) {
      boolean active = currentPage == 0;
      navHome.setTextColor(active ? Color.WHITE : subTextColor());
      navHome.setBackground(round(active ? primaryColor() : Color.TRANSPARENT, 24, 0, 0));
    }
    if (navMine != null) {
      boolean active = currentPage == 1;
      navMine.setTextColor(active ? Color.WHITE : subTextColor());
      navMine.setBackground(round(active ? primaryColor() : Color.TRANSPARENT, 24, 0, 0));
    }
  }

  private View createHomePage() {
    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(18), dp(18), dp(18), dp(8));
    page.setBackgroundColor(bgColor());

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

    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(16), dp(16), dp(16), dp(16));
    card.setBackground(round(cardColor(), 24, borderColor(), 1));
    page.addView(card, lp(-1, -2, 0, 0, 0, dp(14)));

    // ======= 1. 卡密（独立一行） =======
    TextView keyTitle = text("卡密", 15, textColor(), Typeface.BOLD);
    card.addView(keyTitle, lp(-1, -2, 0, 0, 0, dp(8)));

    keyEdit = new EditText(this);
    keyEdit.setSingleLine(true);
    keyEdit.setTextSize(14);
    keyEdit.setTextColor(textColor());
    keyEdit.setHintTextColor(subTextColor());
    keyEdit.setHint("输入卡密");
    keyEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    keyEdit.setPadding(dp(12), dp(8), dp(12), dp(8));
    keyEdit.setBackground(round(cardColor(), 12, borderColor(), 1));
    // 恢复上次保存的卡密
    String savedKey = getSharedPreferences(PREFS, MODE_PRIVATE).getString("key_value", "");
    if (!savedKey.isEmpty()) keyEdit.setText(savedKey);
    card.addView(keyEdit, lp(-1, -2, 0, dp(6), 0, dp(16)));

    // ======= 2. 驱动选择（独立板块） =======
    TextView driverTitle = text("选择驱动", 15, textColor(), Typeface.BOLD);
    card.addView(driverTitle, lp(-1, -2, 0, 0, 0, dp(8)));

    LinearLayout driverRow = new LinearLayout(this);
    driverRow.setOrientation(LinearLayout.HORIZONTAL);
    card.addView(driverRow, lp(-1, dp(42), 0, 0, 0, dp(16)));

    driverBtnKpm = driverOptionButton("KPM", driverType == 0);
    driverBtnParadise = driverOptionButton("Paradise", driverType == 1);
    driverBtnBackup = driverOptionButton("备用", driverType == 2);
    driverRow.addView(driverBtnKpm, new LinearLayout.LayoutParams(0, -1, 1));
    driverRow.addView(driverBtnParadise, new LinearLayout.LayoutParams(0, -1, 1));
    driverRow.addView(driverBtnBackup, new LinearLayout.LayoutParams(0, -1, 1));

    driverBtnKpm.setOnClickListener(v -> selectDriverType(0));
    driverBtnParadise.setOnClickListener(v -> selectDriverType(1));
    driverBtnBackup.setOnClickListener(v -> selectDriverType(2));

    // ======= 3. 防录屏开关 =======
    LinearLayout antiRow = new LinearLayout(this);
    antiRow.setOrientation(LinearLayout.HORIZONTAL);
    antiRow.setGravity(Gravity.CENTER_VERTICAL);
    antiRow.setPadding(0, dp(4), 0, dp(4));
    card.addView(antiRow, lp(-1, -2, 0, 0, 0, dp(8)));

    TextView antiLabel = text("防录屏", 15, textColor(), Typeface.NORMAL);
    antiRow.addView(antiLabel, new LinearLayout.LayoutParams(0, -2, 1));
    antiRecordBtn = switchButton(antiRecord);
    antiRow.addView(antiRecordBtn, new LinearLayout.LayoutParams(dp(64), dp(32)));
    antiRecordBtn.setOnClickListener(v -> toggleAntiRecord());

    // ======= 4. 无后台开关 =======
    LinearLayout bgRow = new LinearLayout(this);
    bgRow.setOrientation(LinearLayout.HORIZONTAL);
    bgRow.setGravity(Gravity.CENTER_VERTICAL);
    bgRow.setPadding(0, dp(4), 0, dp(4));
    card.addView(bgRow, lp(-1, -2, 0, 0, 0, dp(16)));

    TextView bgLabel = text("无后台", 15, textColor(), Typeface.NORMAL);
    bgRow.addView(bgLabel, new LinearLayout.LayoutParams(0, -2, 1));
    noBackgroundBtn = switchButton(noBackground);
    bgRow.addView(noBackgroundBtn, new LinearLayout.LayoutParams(dp(64), dp(32)));
    noBackgroundBtn.setOnClickListener(v -> toggleNoBackground());

    // ======= 5. 运行 / 停止按钮（直接运行，不显示驱动名称） =======
    LinearLayout runRow = new LinearLayout(this);
    runRow.setOrientation(LinearLayout.HORIZONTAL);
    card.addView(runRow, lp(-1, dp(48), 0, dp(8), 0, 0));

    runButton = button("直接运行", true); // 移除了驱动名称
    stopButton = button("停止", false);
    runRow.addView(runButton, new LinearLayout.LayoutParams(0, -1, 1));
    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(88), -1);
    slp.setMargins(dp(10), 0, 0, 0);
    runRow.addView(stopButton, slp);
    stopButton.setVisibility(View.GONE);

    runButton.setOnClickListener(v -> runSelectedFile());
    stopButton.setOnClickListener(v -> stopRunningProcess(true));

    terminalScroll = new ScrollView(this);
    terminalScroll.setFillViewport(true);
    terminalScroll.setBackground(round(terminalBgColor(), 18, 0, 0));
    outputView = text(outputBuffer.toString(), 12, Color.rgb(218, 231, 223), Typeface.NORMAL);
    outputView.setTypeface(Typeface.MONOSPACE);
    outputView.setPadding(dp(14), dp(14), dp(14), dp(14));
    terminalScroll.addView(outputView, new ScrollView.LayoutParams(-1, -2));
    page.addView(terminalScroll, new LinearLayout.LayoutParams(-1, 0, 1));

    // LinearLayout inputRow = new LinearLayout(this);
    // inputRow.setOrientation(LinearLayout.HORIZONTAL);
    // inputRow.setGravity(Gravity.CENTER_VERTICAL);
    // inputRow.setPadding(0, dp(10), 0, 0);
    // page.addView(inputRow, new LinearLayout.LayoutParams(-1, dp(58)));

    // inputEdit = new EditText(this);
    // inputEdit.setSingleLine(true);
    // inputEdit.setTextSize(14);
    // inputEdit.setTextColor(textColor());
    // inputEdit.setHintTextColor(subTextColor());
    // inputEdit.setHint("输入法");
    // // 不使用密码/安全键盘类型，避免部分系统弹出安全键盘导致触摸和输入异常。
    // inputEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    // inputEdit.setPadding(dp(14), 0, dp(14), 0);
    // inputEdit.setBackground(round(cardColor(), 16, borderColor(), 1));
    // inputRow.addView(inputEdit, new LinearLayout.LayoutParams(0, -1, 1));

    // sendButton = button("回车", true);
    // LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(dp(82), -1);
    // sendLp.setMargins(dp(10), 0, 0, 0);
    // inputRow.addView(sendButton, sendLp);
    // sendButton.setOnClickListener(
    //     new View.OnClickListener() {
    //       public void onClick(View v) {
    //         sendTerminalInput();
    //       }
    //     });

    // updateModeButtons();
    updateRunButton();
    // updateInputState();
    scrollTerminalBottom();
    return page;
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

  private View createMinePage() {
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(20), dp(22), dp(20), dp(20));
    page.setBackgroundColor(bgColor());
    scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

    TextView title = text("我的", 28, textColor(), Typeface.BOLD);
    page.addView(title, lp(-1, -2, 0, 0, 0, dp(6)));
    TextView sub = text("设置、授权和快捷目录", 13, subTextColor(), Typeface.NORMAL);
    page.addView(sub, lp(-1, -2, 0, 0, 0, dp(18)));

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

    // View group = settingRow("加入 QQ 群", "1080220886", "Q");
    // card.addView(group);
    // group.setOnClickListener(
    //     new View.OnClickListener() {
    //       public void onClick(View v) {
    //         joinGroupChat();
    //       }
    //     });
    // addDivider(card);

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

  private void addDivider(LinearLayout parent) {
    View v = new View(this);
    v.setBackgroundColor(borderColor());
    parent.addView(v, new LinearLayout.LayoutParams(-1, 1));
  }

  //   private void setDriverMode(boolean enable) {
  //     if (running) {
  //       Toast.makeText(this, "运行中不能切换模式", Toast.LENGTH_SHORT).show();
  //       return;
  //     }
  //     driverMode = enable;
  //     getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("driver_mode",
  // driverMode).apply();
  //     append("已切换为" + (driverMode ? "驱动模式" : "无驱动模式") + "\n");
  //     switchPage(0);
  //   }

  //   private void updateModeButtons() {
  //     if (modeNoDriverView != null) {
  //       modeNoDriverView.setTextColor(!driverMode ? Color.WHITE : subTextColor());
  //       modeNoDriverView.setBackground(
  //           round(
  //               !driverMode ? primaryColor() : tagColor(),
  //               16,
  //               !driverMode ? 0 : borderColor(),
  //               !driverMode ? 0 : 1));
  //     }
  //     if (modeDriverView != null) {
  //       modeDriverView.setTextColor(driverMode ? Color.WHITE : subTextColor());
  //       modeDriverView.setBackground(
  //           round(
  //               driverMode ? primaryColor() : tagColor(),
  //               16,
  //               driverMode ? 0 : borderColor(),
  //               driverMode ? 0 : 1));
  //     }
  //     if (driverBlock != null) driverBlock.setVisibility(driverMode ? View.VISIBLE : View.GONE);
  //   }

  // private void chooseProgramFile() {
  //     showFilePicker("选择运行文件", false, PREF_LAST_RUN_DIR, new FilePickCallback() {
  //         public void onPicked(File file) {
  //             selectedFile = file;
  //             selectedName = file.getName();
  //             if (fileNameView != null) fileNameView.setText(file.getAbsolutePath());
  //             append("已选择运行文件: " + file.getAbsolutePath() + "\n");
  //             updateRunButton();
  //         }
  //     });
  // }

  //   private void chooseDriverZip() {
  //     showFilePicker(
  //         "选择驱动 ZIP",
  //         true,
  //         PREF_LAST_DRIVER_DIR,
  //         new FilePickCallback() {
  //           public void onPicked(File file) {
  //             driverZipFile = file;
  //             driverZipName = file.getName();
  //             matchedDriverName = "";
  //             if (driverNameView != null) driverNameView.setText("正在匹配内核...");
  //             append("已选择驱动 ZIP: " + file.getAbsolutePath() + "\n");
  //             verifyDriverZipAsync(file);
  //             updateRunButton();
  //           }
  //         });
  //   }

  //   private void verifyDriverZipAsync(final File file) {
  //     new Thread(
  //             new Runnable() {
  //               public void run() {
  //                 try {
  //                   final String match = RunnerSupport.findMatchingDriverEntryName(file);
  //                   handler.post(
  //                       new Runnable() {
  //                         public void run() {
  //                           if (file != driverZipFile) return;
  //                           if (match == null || match.length() == 0) {
  //                             matchedDriverName = "";
  //                             if (driverNameView != null)
  //                               driverNameView.setText(driverZipName + " · 未匹配");
  //                             append("未找到当前内核对应驱动: " + RunnerSupport.getKernelVersion() + "\n");
  //                             Toast.makeText(
  //                                     MainActivity.this, "驱动 ZIP 没有匹配当前内核版本的文件",
  // Toast.LENGTH_LONG)
  //                                 .show();
  //                           } else {
  //                             matchedDriverName = new File(match).getName();
  //                             if (driverNameView != null)
  //                               driverNameView.setText(driverZipName + " · " +
  // matchedDriverName);
  //                             append("已匹配驱动: " + matchedDriverName + "\n");
  //                           }
  //                           updateRunButton();
  //                         }
  //                       });
  //                 } catch (final Exception e) {
  //                   handler.post(
  //                       new Runnable() {
  //                         public void run() {
  //                           matchedDriverName = "";
  //                           if (driverNameView != null)
  //                             driverNameView.setText(driverZipName + " · 读取失败");
  //                           append("驱动 ZIP 读取失败: " + safeMessage(e) + "\n");
  //                           updateRunButton();
  //                         }
  //                       });
  //                 }
  //               }
  //             })
  //         .start();
  //   }

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
                  //   post("本地路径: " + selectedFile.getAbsolutePath() + "\n");
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
                  //   post("Root 运行路径: " + rootPath + "\n");
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
                          //   updateInputState();
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
    handler.post(
        new Runnable() {
          public void run() {
            // updateInputState();
          }
        });

    post("[AuraKernel] root command dispatched\n");
    processWriter.write(command);
    processWriter.newLine();
    processWriter.flush();
    sendDriverOptions();

    InputStream in = p.getInputStream();
    byte[] buf = new byte[1024];
    int len;
    try {
      while ((len = in.read(buf)) != -1) {
        String chunk;
        try {
          chunk = new String(buf, 0, len, "UTF-8");
        } catch (Exception e) {
          chunk = new String(buf, 0, len); // 使用系统默认编码兜底
        }
        post(chunk);
      }
    } finally {
      try {
        in.close();
      } catch (Exception ignored) {
      }
    }
    int code = p.waitFor();
    post("\n[AuraKernel] su shell exited: " + code + "\n");
  }

  //   private void sendTerminalInput() {
  //     if (!running || processWriter == null) {
  //       Toast.makeText(this, "程序未运行，无法发送", Toast.LENGTH_SHORT).show();
  //       return;
  //     }
  //     String value = inputEdit == null ? "" : inputEdit.getText().toString();
  //     try {
  //       if (value.length() > 0) processWriter.write(value);
  //       processWriter.newLine();
  //       processWriter.flush();
  //       if (inputEdit != null) inputEdit.setText("");
  //       append(value.length() > 0 ? "\n[已输入并回车]\n" : "\n[已回车]\n");
  //     } catch (Exception e) {
  //       append("\n发送失败: " + safeMessage(e) + "\n");
  //       Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show();
  //     }
  //   }

  //   private String getAutoDriverChoiceForWePro() {
  //     if (!driverMode) return null;
  //     if (selectedName == null || !"AuraKernel.sh".equalsIgnoreCase(selectedName.trim())) return
  // null;
  //     String zip = driverZipName == null ? "" : driverZipName.toLowerCase();
  //     String path = driverZipFile == null ? "" : driverZipFile.getName().toLowerCase();
  //     String all = zip + " " + path;
  //     if (all.contains("rt")) return "2";
  //     if (all.contains("qx")) return "1";
  //     return null;
  //   }

  //   private String describeAutoDriverChoice(String choice) {
  //     if ("2".equals(choice)) return "Rt 驱动";
  //     if ("1".equals(choice)) return "Qx 驱动";
  //     return "未知驱动";
  //   }

  private void sendDriverOptions() {
    String key = keyEdit.getText().toString().trim();

    String driverNum = driverType == 0 ? "2" : (driverType == 1 ? "3" : "1");
    String antiNum = antiRecord ? "1" : "2";
    String bgNum = noBackground ? "2" : "1";

    int baseDelay = 600; // 驱动选择延迟

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
    int bgDelay = baseDelay + 1600;
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
          bgDelay);
    }

    // 4. 卡密（放到最后，延迟足够长）
    if (!key.isEmpty()) {
      getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("key_value", key).apply();
      // 在无后台（或防录屏）之后 1.2 秒发送
      int keyDelay = (driverType != 0 ? bgDelay : baseDelay + 800) + 3000;
      handler.postDelayed(
          () -> {
            if (!running || processWriter == null) return;
            try {
              processWriter.write(key);
              processWriter.newLine();
              processWriter.flush();
              append(key + "\n");
            } catch (Exception e) {
            }
          },
          keyDelay);
    }
  }

  //   private void scheduleAutoDriverChoiceIfNeeded() {
  //     final String choice = getAutoDriverChoiceForWePro();
  //     if (choice == null) return;
  //     post("[AuraKernel] 已安排自动输入：" + choice + " + 回车\n");
  //     handler.postDelayed(
  //         new Runnable() {
  //           public void run() {
  //             autoSendLine(choice, "AuraKernel.sh 自动选择 " + describeAutoDriverChoice(choice));
  //           }
  //         },
  //         1200);
  //   }

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
    // updateInputState();
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
  }

  //   private void updateInputState() {
  //     boolean enabled = running && processWriter != null;
  //     if (sendButton != null) {
  //       sendButton.setEnabled(enabled);
  //       sendButton.setAlpha(enabled ? 1f : 0.55f);
  //       sendButton.setBackground(round(enabled ? primaryColor() : disabledColor(), 14, 0, 0));
  //       sendButton.setTextColor(enabled ? Color.WHITE : subTextColor());
  //     }
  //     if (inputEdit != null) inputEdit.setEnabled(enabled);
  //   }

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

  private void joinGroupChat() {
    final String groupNumber = "1080220886";
    try {
      Intent intent =
          new Intent(
              Intent.ACTION_VIEW,
              Uri.parse(
                  "mqqapi://card/show_pslcard?src_type=internal&version=1&uin="
                      + groupNumber
                      + "&card_type=group&source=qrcode"));
      startActivity(intent);
    } catch (Exception e) {
      try {
        android.content.ClipboardManager clipboard =
            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("QQ群号", groupNumber));
        Toast.makeText(this, "未检测到 QQ，群号已复制: " + groupNumber, Toast.LENGTH_LONG).show();
      } catch (Exception ignored) {
        Toast.makeText(this, "QQ群: " + groupNumber, Toast.LENGTH_LONG).show();
      }
    }
  }

  private void checkUpdate() {
    Toast.makeText(this, "当前已是最新版本 v" + getVersionName(), Toast.LENGTH_SHORT).show();
  }

  private String getVersionName() {
    try {
      String v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      return v == null ? "1.0" : v;
    } catch (Exception e) {
      return "1.0";
    }
  }

  private void append(String s) {
    outputBuffer.append(s);
    if (outputBuffer.length() > 80000) outputBuffer.delete(0, outputBuffer.length() - 60000);
    if (outputView != null) outputView.setText(outputBuffer.toString());
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
    return Color.rgb(22, 119, 255);
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