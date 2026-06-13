package com.aa.Aurakernel; // ← 注意这里的包名，检查一下你的文件路径对不对

import java.util.Base64;

public class StringGuard {
  private static final byte XOR_KEY = 0x7B;

  public static String get(int index) {
    String[] cipherArr = {
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQgZCFQOCx8aDx5UOg4JGhAeCRUeF1UIEw==", // 0: Aurakernel.sh URL
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQgZCFQOCx8aDx5UktLKnvHTVQESCw==", // 1: 驱动.zip URL
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQgZCFQOCx8aDx5UDgsfGg8eVREIFBU=", // 2: update.json URL
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQgZCFQOCx8aDx5UOg4JGhAeCRUeF1UaCxA=", // 3: Aurakernel.apk URL
      "Og4JGhAeFR4XVQgTEw==",                                         // 4: Aurakernel.sh ← 文件名
      "VB8aDxpUFxQYGhdUDxYLVAweCwkUJAkOFRUeCQ==",                     // 5: /data/local/tmp/wepro_runner ← 路径
      "LRcJAjY0DAMxGR0rQjAiCAgoDhI2UB8YSxlUNCtMTRYKTDEKGTESLTMyNkY=", // 6 ← 先放明文测试
      "Ph1LCzMKSR0eTDcvAg4ZMx05KC88AxcVDhAdFFQzQyw4Ej40E0wMVEgeT0Y="  // 7: C++二进制哈希 🔥
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
