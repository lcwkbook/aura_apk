// ★ 新建一个工具类：CipherTool.java（仅开发时用，不用编入APK）
import java.util.Base64;

public class CipherTool {
  private static final byte XOR_KEY = 0x7B; // 必须与 StringGuard 一致

  public static String encrypt(String raw) {
    byte[] data = raw.getBytes();
    for (int i = 0; i < data.length; i++) {
      data[i] ^= XOR_KEY;
    }
    return Base64.getEncoder().encodeToString(data);
  }

  public static void main(String[] args) {
    // ═══════ 在这里填入你的真实地址 ═══════
    String scriptUrl = "https://aura.xiaon.sbs/update/Aurakernel.sh";
    String driverUrl = "https://aura.xiaon.sbs/update/驱动.zip";
    String updateUrl = "https://aura.xiaon.sbs/update/update.json";
    String apkUrl = "https://aura.xiaon.sbs/update/Aurakernel.apk";
    String apkUrl2 = "https://aura.xiaon.sbs/update/Aurakernel.apk"; // update.json里的apkUrl
    String scriptHash = "uqF0wVKycYjR5rFBk9sa0wmaNNHzuC+k2vDoqf7z6Ww="; // 脚本SHA256
    String sigHash = "VlryMOwxJbfP9KYssSuiM+dc0b/OP76mq7JqbJiVHIM="; //签名哈希
    String binaryHash = "uqF0wVKycYjR5rFBk9sa0wmaNNHzuC+k2vDoqf7z6Ww="; //C++二进制哈希
    String dashboardUrl = "https://mt.xiaon.sbs/";
    String avatarUrl = "https://www.loliapi.com/acg/pp/";
    String verifyUrl = "https://aura.xiaon.sbs/update/verify.php";
    

    System.out.println("=== 填入 StringGuard.java ===");
    System.out.println("索引0 (脚本): " + encrypt(scriptUrl));
    System.out.println("索引1 (驱动): " + encrypt(driverUrl));
    System.out.println("索引2 (update.json): " + encrypt(updateUrl));
    System.out.println("索引3 (APK备用): " + encrypt(apkUrl));

    System.out.println("\n=== 填入 update.json ===");
    System.out.println("apkUrl (明文): " + apkUrl2);
    System.out.println("apkUrl_enc (加密): " + encrypt(apkUrl2));
    System.out.println("scriptHash: " + scriptHash);
    System.out.println("索引6 (签名哈希): " + encrypt(sigHash));
    System.out.println("索引7 (C++二进制哈希): " + encrypt(binaryHash));
    System.out.println("索引8 (Dashboard): " + encrypt(dashboardUrl));
    System.out.println("索引9 (头像API): " + encrypt(avatarUrl));
    System.out.println("索引10 (verify.php): " + encrypt(verifyUrl));
  }
}
