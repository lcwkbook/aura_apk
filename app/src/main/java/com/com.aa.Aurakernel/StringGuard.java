package com.aa.Aurakernel; 

import java.util.Base64;

public class StringGuard {
  private static final byte XOR_KEY = 0x7B;

  public static String get(int index) {
    String[] cipherArr = {
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UOg4JGhAeCRUeF1UIEw==", // 0: Aurakernel.sh URL
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UktLKnvHTVQESCw==", // 1: 驱动.zip URL
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UDgsfGg8eVREIFBU=", // 2: update.json URL
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UOg4JGhAeCRUeF1UaCxA=", // 3: Aurakernel.apk URL
      "Og4JGhAeCRUeF1UIEw==",                                         // 4: Aurakernel.sh ← 文件名
      "VB8aDxpUFxQYGhdUDxYLVAweCwkUJAkOFRUeCQ==",                     // 5: /data/local/tmp/wepro_runner ← 路径
      "LRcJAjY0DAMxGR0rQjAiCAgoDhI2UB8YSxlUNCtMTRYKTDEKGTESLTMyNkY=", // 6 ← 先放明文测试
      "",  // 7: C++二进制哈希 🔥
      "Ew8PCwhBVFQWD1UDEhoUFVUPFAtU",                                  // 8: 监控地址
      "Ew8PCwhBVFQMDAxVFxQXEhoLElUYFBZUGhgcVAsLVA==",                  // 9: 随机头像API ← 新增
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UDR4JEh0CVQsTCw==",     // 10: verify.php
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
