import java.util.Base64;

public class CipherTool {
  private static final byte XOR_KEY = 0x7B;

  public static String encrypt(String raw) {
    byte[] data = raw.getBytes();
    for (int i = 0; i < data.length; i++) {
      data[i] ^= XOR_KEY;
    }
    return Base64.getEncoder().encodeToString(data);
  }

  public static void main(String[] args) {
    // ═══════ 新服务器信息（154.219.116.68:1111，根目录）═══════
    String scriptUrl = "http://154.219.116.68:1111/Aura.sh";
    String renderScriptUrl = "http://154.219.116.68:1111/Render.sh";
    String driverUrl = "http://154.219.116.68:1111/驱动.zip";
    String updateUrl = "http://154.219.116.68:1111/update.json";
    String apkUrl = "http://154.219.116.68:1111/new.apk";
    String scriptHash = "JGCxT3ruFrsd7j4EMyhuiCx0XCTmNoMItyJpAzPH8KU=";
    String sigHash = "VlryMOwxJbfP9KYssSuiM+dc0b/OP76mq7JqbJiVHIM=";
    String binaryHash = "JGCxT3ruFrsd7j4EMyhuiCx0XCTmNoMItyJpAzPH8KU=";
    String dashboardUrl = "https://mt.xiaon.top/";
    String avatarUrl = "https://www.loliapi.com/acg/pp/";
    String verifyUrl = "http://154.219.116.68:1111/verify.php";

    System.out.println("=== 填入 StringGuard.java ===");
    System.out.println("索引0 (单透脚本URL): " + encrypt(scriptUrl));
    System.out.println("索引1 (驱动): " + encrypt(driverUrl));
    System.out.println("索引2 (update.json): " + encrypt(updateUrl));
    System.out.println("索引3 (APK): " + encrypt(apkUrl));
    System.out.println("索引4 (脚本文件名): " + encrypt("Aura.sh"));
    System.out.println("索引5 (路径): " + encrypt("/data/local/tmp/wepro_runner"));
    System.out.println("索引6 (签名哈希): " + encrypt(sigHash));
    System.out.println("索引7 (二进制哈希): " + encrypt(binaryHash));
    System.out.println("索引8 (Dashboard): " + encrypt(dashboardUrl));
    System.out.println("索引9 (头像API): " + encrypt(avatarUrl));
    System.out.println("索引10 (verify.php): " + encrypt(verifyUrl));
    
    System.out.println("\n=== 新增渲染内核相关（可选加在 StringGuard 末尾）===");
    System.out.println("索引11 (渲染脚本URL): " + encrypt(renderScriptUrl));
    System.out.println("索引12 (渲染脚本文件名): " + encrypt("Render.sh"));
  }
}
