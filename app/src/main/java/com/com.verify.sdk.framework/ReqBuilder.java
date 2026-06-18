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

    // ===== DH模式专用请求构建（query string格式） =====
    private static ApiBaseEntity builderReqDH(ApiBaseEntity apiBaseEntity) {
        // 1. 生成安全参数
        String safeCode = RandomUtil.randomNumbers(32);
        String timestamp = System.currentTimeMillis() + "";
        apiBaseEntity.setSafeCode(safeCode);
        apiBaseEntity.setTimestamp(timestamp);
        apiBaseEntity.setAppId(VerifyConfig.verifyConfig.getAppId());

        // 2. 构建 query string 格式的 params（不含bob和signature）
        Map<String, String> paramMap = new TreeMap<>(); // TreeMap自动排序
        paramMap.put("appId", VerifyConfig.verifyConfig.getAppId());
        paramMap.put("timestamp", timestamp);
        paramMap.put("safeCode", safeCode);
        paramMap.put("card", getFieldValue(apiBaseEntity, "card"));
        paramMap.put("mac", getFieldValue(apiBaseEntity, "mac"));
        paramMap.put("token", "");

        // 3. DH密钥交换，生成bob
        try {
            String serverPubKey = VerifyConfig.verifyConfig.getSecretKey()[0];
            DHHelper.DHResult dhResult = DHHelper.generateAndDeriveKey(serverPubKey);
            apiBaseEntity.setBob(dhResult.clientPublicKeyBase64);
            VerifyConfig.verifyConfig.setCurrentDHKey(dhResult.aesKeyBytes);

            // 4. bob加入排序（C++代码：sort_dict_req(params + "&bob=" + bob_hex)）
            paramMap.put("bob", dhResult.clientPublicKeyBase64);

            // 5. 构建签名原文并计算签名
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : paramMap.entrySet()) {
                sb.append(e.getKey()).append("=").append(e.getValue() == null ? "" : e.getValue()).append("&");
            }
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            String sortedParams = sb.toString();
            String signStr = sortedParams + VerifyConfig.verifyConfig.getAppKey();
            String signature = cn.hutool.crypto.digest.DigestUtil.md5Hex(signStr);
            apiBaseEntity.setSignature(signature);

            // 6. 构建待加密字符串: params + "&signature=" + md5_str
            // 注意：待加密的不含appId和bob
            StringBuilder encryptSb = new StringBuilder();
            encryptSb.append("card=").append(paramMap.get("card"));
            encryptSb.append("&mac=").append(paramMap.get("mac"));
            encryptSb.append("&safeCode=").append(safeCode);
            encryptSb.append("&timestamp=").append(timestamp);
            encryptSb.append("&token=").append(paramMap.get("token"));
            encryptSb.append("&signature=").append(signature);
            String toEncrypt = encryptSb.toString();

            // 7. DH加密（AES-128-ECB → Base64）
            String encrypted = EncryptBuilder.builderEncryptParams(toEncrypt, dhResult.aesKeyBytes);
            apiBaseEntity.setEncryptParams(encrypted);

        } catch (Exception e) {
            Console.error("DH密钥交换失败: " + e.getMessage());
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
