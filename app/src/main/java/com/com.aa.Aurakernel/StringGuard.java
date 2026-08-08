package com.aa.Aurakernel;

import java.util.Base64;

public class StringGuard {
  private static final byte XOR_KEY = 0x7B;

  public static String get(int index) {
    String[] cipherArr = {
      "Ew8PCwhBVFQTD1UDEhoUFVUPFAtUDgsfGg8eVDoOCRoQHgkVHhdVCBM=", // 0: 脚本URL ✅ 已更新
      "Ew8PCwhBVFQTD1UDEhoUFVUPFAtUDgsfGg8eVB8JEg0eCQhVARIL",         // 1: 驱动URL ✅ 已更新
      "Ew8PCwhBVFQTD1UDEhoUFVUPFAtUDgsfGg8eVREIFBU=",              // 2: update.json ✅ 已更新
      "Ew8PCwhBVFQTD1UDEhoUFVUPFAtUDgsfGg8eVDoOCRoQHgkVHhdVGgsQ",  // 3: APK URL ✅ 已更新
      "Og4JGhAeCRUeF1UIEw==",                                       // 4: Aurakernel.sh（不变）
      "VB8aDxpUFxQYGhdUDxYLVAweCwkUJAkOFRUeCQ==",                   // 5: /data/local/tmp/wepro_runner（不变）
      "LRcJAjY0DAMxGR0rQjAiCAgoDhI2UB8YSxlUNCtMTRYKTDEKGTESLTMyNkY=", // 6: 签名哈希（不变）
      "Tg0PTQ0/SRc3Qi8wTRdIFg8zKg9QOCsBUC4VIiE2P0gZMgEzLBcZLQspMkY=", // 7: C++二进制哈希 ✅ 已更新
      "Ew8PCwhBVFQWD1UDEhoUFVUPFAtU",                                // 8: 监控地址（不变）
      "Ew8PCwhBVFQMDAxVFxQXEhoLElUYFBZUGhgcVAsLVA==",                // 9: 随机头像API（不变）
      "Ew8PCwhBVFQTD1UDEhoUFVUPFAtUGgsSVA0eCRIdAlULEws=",            // 10: verify.php ✅ 已更新
      "Ew8PCwhBVFQOGgsSCFUYFVQaCxJUDUpUCBoCEhUcVAkaFR8UFkQWFB8eRgkaFR8UFl0YGg8eHBQJAkacxuqd4+ifwepdGgsSJBAeAkYOGgsSVhkXVh4STBUVLBMXExoVOA4hNQo9ShwWFRQ2EAwBFQ40AUhCFxA+ETA=",                      // 11: 每日一言API
      "Ew8PCwhBVFQTD1UDEhoUFVUPFAtUGgsSVBUUDxIYHlULEws=",            // 12: 公告接口API (api/notice.php)
    };
    return decrypt(cipherArr[index]);
  }

  public static String decrypt(String cipher) {
    if (cipher == null || cipher.isEmpty()) return "";
    byte[] data = Base64.getDecoder().decode(cipher);
    for (int i = 0; i < data.length; i++) {
      data[i] ^= XOR_KEY;
    }
    return new String(data);
  }
}
