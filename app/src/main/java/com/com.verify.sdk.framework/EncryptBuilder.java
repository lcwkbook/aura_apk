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
 * @ClassName EncryptBuilder.java
 * @Description
 * @createTime 2025年08月02日 22:30
 */
public class EncryptBuilder {

    public static String builderEncryptParams(JSONObject originalJson) {

        byte[] bytes = encryptStr(originalJson.toJSONString(0));

        if (bytes == null || bytes.length == 0 || new String(bytes).isEmpty()) {
            return originalJson.toJSONString(0);
        }

        if (VerifyConfig.verifyConfig.getEncodeType().equals("16进制编码")) {
            return HexUtil.encodeHexStr(bytes);
        }
        if (VerifyConfig.verifyConfig.getEncodeType().equals("Base64编码")) {
            return Base64.encode(bytes);
        } else {
            Console.error("没有选择编码模式 encodeType 应为 16进制编码 或 Base64编码");
            return "";
        }
    }

    /**
 * 使用指定的AES密钥进行加密并编码（用于DH密钥交换模式）
 *
 * @param params     待加密的明文字符串
 * @param aesKeyBytes AES密钥字节数组
 * @return 编码后的加密字符串（16进制或Base64）
 */
public static String builderEncryptParams(String params, byte[] aesKeyBytes) {
        byte[] bytes = encryptStr(params, aesKeyBytes);

        if (bytes == null || bytes.length == 0 || new String(bytes).isEmpty()) {
            return params;
        }

        if (VerifyConfig.verifyConfig.getEncodeType().equals("16进制编码")) {
            return HexUtil.encodeHexStr(bytes);
        }
        if (VerifyConfig.verifyConfig.getEncodeType().equals("Base64编码")) {
            return Base64.encode(bytes);
        } else {
            Console.error("没有选择编码模式 encodeType 应为 16进制编码 或 Base64编码");
            return "";
        }
    }

    public static String builderEncryptParams(String params) {
        byte[] bytes = encryptStr(params);
        if (bytes == null || bytes.length == 0 || new String(bytes).isEmpty()) {
            return params;
        }
        if (VerifyConfig.verifyConfig.getEncodeType().equals("16进制编码")) {
            return HexUtil.encodeHexStr(bytes);
        }
        if (VerifyConfig.verifyConfig.getEncodeType().equals("Base64编码")) {
            return Base64.encode(bytes);
        } else {
            Console.error("没有选择编码模式 encodeType 应为 16进制编码 或 Base64编码");
            return "";
        }
    }



    public static byte[] encryptStr(String params) {
        byte[] encrypt = null;
        // 判断加密方式
        if (VerifyConfig.verifyConfig.getSecretType().length < 1) {
            Console.error("请选择加密算法");
        }
        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("不加密")) {
            return "".getBytes(StandardCharsets.UTF_8);
        }
        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("AES对称加密")) {
            if (StrUtil.isEmpty(VerifyConfig.verifyConfig.getSecretType()[1]) || VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/ECB/PKCS5Padding(无需IV-偏移)")) {
                AES aes = new AES(VerifyConfig.verifyConfig.getSecretKey()[0].getBytes());
                encrypt = aes.encrypt(params);
            }

            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CTS/PKCS5Padding(IV长度一般为16位)")) {
                AES aes = new AES(Mode.CTS, Padding.PKCS5Padding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.encrypt(params);
            }
            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CBC/PKCS5Padding(IV长度一般为16位)")) {
                AES aes = new AES(Mode.CBC, Padding.PKCS5Padding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.encrypt(params);
            }
            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CTS/PKCS5Padding(IV长度一般为16位)")) {
                AES aes = new AES(Mode.CTS, Padding.PKCS5Padding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.encrypt(params);
            }
            if (VerifyConfig.verifyConfig.getSecretType()[1].equals("AES/CBC/ZeroPadding(密钥和IV长度一般为 16位)&按键精灵")) {
                AES aes = new AES(Mode.CBC, Padding.ZeroPadding,
                        VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(),
                        VerifyConfig.verifyConfig.getSecretKey()[1].getBytes());
                encrypt = aes.encrypt(params);
            }

        }

        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("DES对称加密")) {
            DES des = new DES(VerifyConfig.verifyConfig.getSecretKey()[0].getBytes(StandardCharsets.UTF_8));
            encrypt = des.encrypt(params);
        }

        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("RC4对称加密")) {
            RC4 rc4 = new RC4(VerifyConfig.verifyConfig.getSecretKey()[0]);
            encrypt = rc4.encrypt(params);
        }

        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("RSA非对称加密")) {
            RSA rsa = new RSA(new RSA().getPrivateKeyBase64(), VerifyConfig.verifyConfig.getSecretKey()[0]);
            encrypt = rsa.encrypt(params, KeyType.PublicKey);
        }
         // 【修改】DH密钥交换加密 -> AES-128-ECB模式（密钥16字节）
        if (VerifyConfig.verifyConfig.getSecretType()[0].equals("DH密钥交换加密")) {
            byte[] dhKey = VerifyConfig.verifyConfig.getCurrentDHKey();
            if (dhKey != null) {
                AES aes = new AES(dhKey); // 16字节密钥自动用AES-128
                encrypt = aes.encrypt(params);
            } else {
                Console.error("DH密钥交换模式但未生成派生密钥，请检查DHHelper调用");
            }
        }
        return encrypt;
    }

    /**
 * 使用指定的AES密钥进行加密（用于DH密钥交换模式）
 *
 * @param params     待加密的明文字符串
 * @param aesKeyBytes AES密钥字节数组
 * @return 加密后的字节数组
 */
public static byte[] encryptStr(String params, byte[] aesKeyBytes) {
    if (aesKeyBytes == null || aesKeyBytes.length == 0) {
        return encryptStr(params);
    }
    AES aes = new AES(aesKeyBytes);
    return aes.encrypt(params);
}



}
