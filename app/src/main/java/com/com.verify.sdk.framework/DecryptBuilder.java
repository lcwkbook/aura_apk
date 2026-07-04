package com.verify.sdk.framework;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.DES;
import cn.hutool.crypto.symmetric.RC4;
import cn.hutool.json.JSONObject;
import com.verify.sdk.config.VerifyConfig;

import java.nio.charset.StandardCharsets;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName DecryptBuilder.java
 * @Description
 * @createTime 2025年08月02日 22:30
 */
public class DecryptBuilder {

    /**
     * 默认解密（根据VerifyConfig中的加密算法）
     */
    public static String builderDecryptParams(String params) {
        String encrypt = null;
        // 判断加密方式
        if (VerifyConfig.verifyConfig.getSecretType().length < 1) {
            Console.error("请选择加密算法");
        }
        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("不加密")) {
            return params;
        }
        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("AES对称加密")) {
            if (StrUtil.isEmpty(VerifyConfig.verifyConfig.getSecretType()[1]) || VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/ECB/PKCS5Padding(无需IV-偏移)")) {
                AES aes = new AES(VerifyConfig.verifyConfig.getSecretKey()[0].getBytes());
                encrypt = aes.decryptStr(params);
            }

            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CTS/PKCS5Padding(IV长度一般为16位)")) {
                AES aes = new AES(Mode.CTS, Padding.PKCS5Padding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.decryptStr(params);
            }
            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CBC/PKCS5Padding(IV长度一般为16位)")) {
                AES aes = new AES(Mode.CBC, Padding.PKCS5Padding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.decryptStr(params);
            }
            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CTS/PKCS5Padding(IV长度一般为16位)")) {
                AES aes = new AES(Mode.CTS, Padding.PKCS5Padding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.decryptStr(params);
            }
            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CBC/ZeroPadding(密钥和IV长度一般为 16位)&按键精灵")) {
                AES aes = new AES(Mode.CBC, Padding.ZeroPadding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.decryptStr(params);
            }

        }

        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("DES对称加密")) {
            DES des = new DES(VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(StandardCharsets.UTF_8));
            encrypt = des.decryptStr(params);
        }

        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("RC4对称加密")) {
            RC4 rc4 = new RC4(VerifyConfig.verifyConfig.getSecretKey()[0]);
            encrypt = rc4.decrypt(params);
        }

        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("RSA非对称加密")) {
            try {
                String publicKeyStr = VerifyConfig.verifyConfig.getSecretKey()[0];
                byte[] keyBytes = java.util.Base64.getDecoder().decode(publicKeyStr);
                java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(keyBytes);
                java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
                java.security.PublicKey pubKey = keyFactory.generatePublic(keySpec);
                javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA/ECB/PKCS1Padding");
                
                byte[] decoded = java.util.Base64.getDecoder().decode(params);
                int maxBlock = 128; // 1024位RSA解密块大小128字节
                
                // ★ 分段解密
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                int offset = 0;
                while (offset < decoded.length) {
                    int len = Math.min(decoded.length - offset, maxBlock);
                    cipher.init(javax.crypto.Cipher.DECRYPT_MODE, pubKey);
                    byte[] block = cipher.doFinal(decoded, offset, len);
                    out.write(block);
                    offset += len;
                }
                encrypt = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                android.util.Log.d("VERIFY_RSA", "RSA分段解密成功");
            } catch (Exception e) {
                android.util.Log.e("VERIFY_RSA", "RSA解密失败", e);
                Console.error("RSA解密失败: " + e.getMessage());
            }
        }

                // 【修改】DH密钥交换加密 -> AES-128-ECB模式
        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("DH密钥交换加密")) {
            byte[] dhKey = VerifyConfig.verifyConfig.getCurrentDHKey();
            if (dhKey != null) {
                byte[] decodeBytes = Base64.decode(params); // DH响应用Base64
                AES aes = new AES(dhKey); // 16字节密钥
                encrypt = aes.decryptStr(decodeBytes);
                return encrypt;
            }
        }
        return encrypt;
    }

    /**
     * 使用指定的AES密钥进行解密（用于DH密钥交换模式）
     *
     * @param params      待解密的密文字符串（已编码的16进制或Base64）
     * @param aesKeyBytes AES密钥字节数组
     * @return 解密后的明文字符串
     */
        public static String builderDecryptParams(String params, byte[] aesKeyBytes) {
        if (aesKeyBytes == null || aesKeyBytes.length == 0) {
            return builderDecryptParams(params);
        }
        // DH响应始终用Base64解码（C++代码用的Base64）
        byte[] decodeBytes = Base64.decode(params);
        AES aes = new AES(aesKeyBytes);
        return aes.decryptStr(decodeBytes);
    }

}
