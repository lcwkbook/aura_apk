package com.verify.sdk.framework;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import com.verify.sdk.config.ApiBaseEntity;
import com.verify.sdk.config.VerifyConfig;

import java.lang.reflect.Field;
import java.util.*;

public class ReqBuilder {

    public static ApiBaseEntity builderReq(ApiBaseEntity apiBaseEntity) {
        if (VerifyConfig.verifyConfig == null) {
            throw new RuntimeException("请先初始化VerifyConfig");
        }

        boolean isDHMode = VerifyConfig.verifyConfig.getSecretType() != null
                && VerifyConfig.verifyConfig.getSecretType().length > 0
                && "DH密钥交换加密".equals(VerifyConfig.verifyConfig.getSecretType()[0]);

        // ===== DH模式：使用query string格式（与C++一致） =====
        if (isDHMode) {
            return builderReqDH(apiBaseEntity);
        }

        // ===== 非DH模式：原有逻辑（JSON格式） =====
        if ("开".equals(VerifyConfig.verifyConfig.getRandomType())) {
            apiBaseEntity.setSafeCode(RandomUtil.randomNumbers(32));
        }
        apiBaseEntity.setTimestamp(System.currentTimeMillis() + "");
        apiBaseEntity.setAppId(VerifyConfig.verifyConfig.getAppId());

        Map<String, Object> paramsMap = toMap(apiBaseEntity);
        if (!paramsMap.containsKey("mac")) paramsMap.put("mac", "");
        if (!paramsMap.containsKey("token")) paramsMap.put("token", "");

        String signature = SignBuilder.builderSign(new JSONObject(paramsMap));
        apiBaseEntity.setSignature(signature);

        JSONObject entries = new JSONObject(toMap(apiBaseEntity));
        entries.remove("apiUrl");
        entries.remove("encryptParams");
        String sorted = SignBuilder.sortByAscii(entries);
        String paramsEncrypt = EncryptBuilder.builderEncryptParams(sorted);
        apiBaseEntity.setEncryptParams(paramsEncrypt);

        return apiBaseEntity;
    }

    private static ApiBaseEntity builderReqDH(ApiBaseEntity apiBaseEntity) {
    String safeCode = RandomUtil.randomNumbers(32);
    String timestamp = System.currentTimeMillis() + "";
    apiBaseEntity.setSafeCode(safeCode);
    apiBaseEntity.setTimestamp(timestamp);
    apiBaseEntity.setAppId(VerifyConfig.verifyConfig.getAppId());

    // ★ 1. 动态获取实体类的字段（只包含非空、有值的字段，排除 apiUrl/encryptParams） ★
    Map<String, Object> allFields = toMap(apiBaseEntity);
    allFields.remove("apiUrl");
    allFields.remove("encryptParams");
    allFields.remove("appId");       // C++ 签名不含 appId
    allFields.remove("signature");   // 签名自己不算自己

    // ★ 2. DH 密钥交换 ★
    try {
        String serverPubKey = VerifyConfig.verifyConfig.getSecretKey()[0];
        DHHelper.DHResult dhResult = DHHelper.generateAndDeriveKey(serverPubKey);
        apiBaseEntity.setBob(dhResult.clientPublicKeyBase64);
        VerifyConfig.verifyConfig.setCurrentDHKey(dhResult.aesKeyBytes);

        // 3. bob 参与签名（C++ 如此）
        allFields.put("bob", dhResult.clientPublicKeyBase64);

        // 4. ★ ASCII排序签名（同C++ sort_dict_req + app_secret）★
        Map<String, String> sortedMap = new TreeMap<>();
        for (Map.Entry<String, Object> e : allFields.entrySet()) {
            sortedMap.put(e.getKey(), e.getValue() == null ? "" : e.getValue().toString());
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sortedMap.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        String signedStr = sb.toString();
        String signature = cn.hutool.crypto.digest.DigestUtil.md5Hex(
            signedStr + VerifyConfig.verifyConfig.getAppKey()
        );
        apiBaseEntity.setSignature(signature);

        // 5. ★ 加密数据 = (除bob外的字段) + &signature=... ★
        //    C++ 加密的是: 原始params + "&signature=" + md5
        //    原始params不含bob和appId
        StringBuilder encryptSb = new StringBuilder();
        for (Map.Entry<String, String> e : sortedMap.entrySet()) {
            if (!e.getKey().equals("bob")) {  // bob不加密，仅签名
                encryptSb.append(e.getKey()).append("=").append(e.getValue()).append("&");
            }
        }
        encryptSb.append("signature=").append(signature);
        String toEncrypt = encryptSb.toString();

        // 6. DH加密 → Base64
        String encrypted = EncryptBuilder.builderEncryptParams(toEncrypt, dhResult.aesKeyBytes);
        apiBaseEntity.setEncryptParams(encrypted);

    } catch (Exception e) {
        throw new RuntimeException("DH密钥交换失败", e);
    }

    return apiBaseEntity;
}


    private static String getFieldValue(ApiBaseEntity entity, String fieldName) {
        try {
            Class<?> clazz = entity.getClass();
            while (clazz != null && !clazz.equals(Object.class)) {
                try {
                    Field f = clazz.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object val = f.get(entity);
                    return val != null ? val.toString() : "";
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static Map<String, Object> toMap(ApiBaseEntity entity) {
        Map<String, Object> result = new HashMap<>();
        if (entity == null) return result;

        Class<?> clazz = entity.getClass();
        while (clazz != null && !clazz.equals(Object.class)) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    result.put(field.getName(), value != null ? value : "");
                } catch (IllegalAccessException e) {
                    Console.error(e);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return result;
    }
}
