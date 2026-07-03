package com.aa.ABC; 

import java.util.Base64;

public class StringGuard {
  private static final byte XOR_KEY = 0x7B;

  public static String get(int index) {
    String[] cipherArr = {
      "Ew8PC0FUVEpOT1VJSkJVSkpNVU1DQUpKSkpUOg4JGlUIEw==",  // 0: Aura.sh URL
      "Ew8PC0FUVEpOT1VJSkJVSkpNVU1DQUpKSkpUktLKnvHTVQESCw==",  // 1: 驱动.zip
      "Ew8PC0FUVEpOT1VJSkJVSkpNVU1DQUpKSkpUDgsfGg8eVREIFBU=",  // 2: update.json
      "Ew8PC0FUVEpOT1VJSkJVSkpNVU1DQUpKSkpUFR4MVRoLEA==",  // 3: new.apk
      "Og4JGlUIEw==",  // 4: Aura.sh
      "VB8aDxpUFxQYGhdUDxYLVAweCwkUJAkOFRUeCQ==",  // 5: /data/local/tmp/wepro_runner
      "LRcJAjY0DAMxGR0rQjAiCAgoDhI2UB8YSxlUNCtMTRYKTDEKGTESLTMyNkY=",  // 6: sigHash
      "MTw4Ay9ICQ49CQgfTBFPPjYCEw4SOANLIzgvFjUUNjIPAjELOgErM0MwLkY=",  // 7: binaryHash
      "Ew8PCwhBVFQWD1UDEhoUFVUPFAtU",  // 8: dashboardUrl
      "Ew8PCwhBVFQMDAxVFxQXEhoLElUYFBZUGhgcVAsLVA==",  // 9: avatarUrl
      "Ew8PC0FUVEpOT1VJSkJVSkpNVU1DQUpKSkpUDR4JEh0CVQsTCw==",  // 10: verify.php
      "Ew8PC0FUVEpOT1VJSkJVSkpNVU1DQUpKSkpUKR4VHx4JVQgT",  // 11: Render.sh URL
      "KR4VHx4JVQgT",  // 12: Render.sh
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
