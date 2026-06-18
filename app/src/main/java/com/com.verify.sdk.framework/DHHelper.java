package com.verify.sdk.framework;

import cn.hutool.core.util.HexUtil;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class DHHelper {

    public static class DHResult {
        public final String clientPublicKeyBase64;
        /** AES-128密钥（共享密钥的前16字节） */
        public final byte[] aesKeyBytes;

        public DHResult(String clientPublicKeyBase64, byte[] aesKeyBytes) {
            this.clientPublicKeyBase64 = clientPublicKeyBase64;
            this.aesKeyBytes = aesKeyBytes;
        }
    }

    public static DHResult generateAndDeriveKey(String serverPublicKeyHex) throws Exception {
        // 1. 解码服务端DH公钥（16进制）
        byte[] serverKeyBytes = HexUtil.decodeHex(serverPublicKeyHex);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(serverKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        PublicKey serverPublicKey = keyFactory.generatePublic(keySpec);

        // 2. 使用服务端的DH参数生成客户端密钥对
        DHParameterSpec dhParams = ((DHPublicKey) serverPublicKey).getParams();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhParams);
        KeyPair clientKeyPair = keyGen.generateKeyPair();

        // 3. 计算共享密钥（原始字节）
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(clientKeyPair.getPrivate());
        keyAgreement.doPhase(serverPublicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        // 4. ★ 关键修改：取前16字节作为AES-128密钥（不做SHA-256！）
        byte[] aesKey = Arrays.copyOf(sharedSecret, 16);

        // 5. 客户端公钥转16进制字符串（C++用的就是16进制）
        String clientPublicKeyHex = HexUtil.encodeHexStr(
                clientKeyPair.getPublic().getEncoded()
        );

        return new DHResult(clientPublicKeyHex, aesKey);
    }
}
