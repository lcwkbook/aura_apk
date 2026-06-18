package com.verify.sdk.framework;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.verify.sdk.config.ApiBaseEntity;
import com.verify.sdk.config.VerifyConfig;

import java.util.HashMap;
import android.util.Log;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName FrameworkTool.java
 * @Description 核心工具
 * @createTime 2025年08月02日 22:44
 */
public class FrameworkTool {

    /**
     * 调用此方法仅进行接口请求，不进行接口验签等操作
     *
     * @param entity
     * @return
     */
        public static String send(ApiBaseEntity entity) {
        boolean isDHMode = VerifyConfig.verifyConfig.getSecretType() != null
                && VerifyConfig.verifyConfig.getSecretType().length > 0
                && "DH密钥交换加密".equals(VerifyConfig.verifyConfig.getSecretType()[0]);

        // ===== DH模式：POST + appId + params(加密) + bob =====
        if (isDHMode) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("appId", entity.getAppId());
            hashMap.put("params", entity.getEncryptParams());
            hashMap.put("bob", entity.getBob() != null ? entity.getBob() : "");
            Log.d("VERIFY_SEND", "DH POST: " + hashMap.toString());
            return HttpUtil.post(entity.getApiUrl(), hashMap);
        }

        // ===== 非DH模式：POST所有字段 =====
        java.util.Map<String, Object> allFields = new java.util.HashMap<>();
        java.lang.reflect.Field[] fields = entity.getClass().getDeclaredFields();
        for (java.lang.reflect.Field f : fields) {
            try { f.setAccessible(true); Object val = f.get(entity); if (val != null) allFields.put(f.getName(), val); } catch (Exception ignored) {}
        }
        java.lang.reflect.Field[] parentFields = entity.getClass().getSuperclass().getDeclaredFields();
        for (java.lang.reflect.Field f : parentFields) {
            try { f.setAccessible(true); Object val = f.get(entity); if (val != null && !f.getName().equals("apiUrl") && !f.getName().equals("encryptParams")) allFields.put(f.getName(), val); } catch (Exception ignored) {}
        }
        if (!allFields.containsKey("timestamp")) allFields.put("timestamp", "");
        if (!allFields.containsKey("safeCode")) allFields.put("safeCode", "");
        if (!allFields.containsKey("signature")) allFields.put("signature", "");
        if (!allFields.containsKey("mac")) allFields.put("mac", "");
        if (!allFields.containsKey("token")) allFields.put("token", "");
        if (!allFields.containsKey("bob")) allFields.put("bob", "");

        return HttpUtil.post(entity.getApiUrl(), allFields);
    }






    /**
     * 调用此方法进行接口请求和响应验证
     *
     * @param entity
     * @return
     * @throws RuntimeException
     */
    public static JSONObject sendWithRes(ApiBaseEntity entity) throws RuntimeException {
        String response = send(entity);
        // ===== 加日志看原始响应 =====
        android.util.Log.d("VERIFY_RAW", "原始响应: " + response);
         // ===== 如果是HTML错误，直接抛出来 =====
    if (response.startsWith("<html")) {
        throw new RuntimeException("服务端返回HTML错误，请检查请求参数格式");
    }
        String responseDecryptStr;
        boolean isDHMode = VerifyConfig.verifyConfig.getSecretType() != null
                && VerifyConfig.verifyConfig.getSecretType().length > 0
                && "DH密钥交换加密".equals(VerifyConfig.verifyConfig.getSecretType()[0]);
        byte[] currentDHKey = VerifyConfig.verifyConfig.getCurrentDHKey();

        if (isDHMode && currentDHKey != null) {
            responseDecryptStr = DecryptBuilder.builderDecryptParams(response, currentDHKey);
        } else {
            responseDecryptStr = DecryptBuilder.builderDecryptParams(response);
        }
        JSONObject jsonObject = new JSONObject(responseDecryptStr);

        // 第一层业务逻辑成功
        if (jsonObject.getInt("code") != Integer.parseInt(VerifyConfig.verifyConfig.getLogicCode())) {
            Console.error(jsonObject.getStr("msg"));
            throw new RuntimeException(jsonObject.getStr("msg"));
        }

        // 第二层随机数校验
        if (!StrUtil.isEmpty(VerifyConfig.verifyConfig.getRandomType()) && VerifyConfig.verifyConfig.getRandomType().equals("开")) {
            if (!entity.getSafeCode().equals(jsonObject.getStr("safeCode"))) {
                Console.error("随机数校验失败，本次请求失效");
                throw new RuntimeException("随机数校验失败，本次请求过期");
            }
        }

        // 第三层本地时间戳验证
        if (!StrUtil.isEmpty(VerifyConfig.verifyConfig.getLocalTimeVerify()) && !VerifyConfig.verifyConfig.getLocalTimeVerify().equals("空")) {
            if (System.currentTimeMillis() - jsonObject.getLong("timestamp") > Long.parseLong(VerifyConfig.verifyConfig.getLocalTimeVerify())) {
                // 大于能容忍的时间差
                Console.error("时间戳验证失败，请检查时间戳是否正确");
                throw new RuntimeException("时间戳验证失败，响应过期");
            }
        }

        // 第四层验签
        if (!StrUtil.isEmpty(VerifyConfig.verifyConfig.getSignRule()) && !VerifyConfig.verifyConfig.getSignRule().equals("不计算")) {
            String resSignature = SignBuilder.builderSign(jsonObject);
            if (!resSignature.equals(jsonObject.getStr("signature"))) {
                Console.error("验签失败，请检查验签规则");
                throw new RuntimeException("验签失败，请检查验签规则");
            }
        }
        return jsonObject;
    }

    /**
     * 调用此方法进行自动化心跳
     * ** 注意：必须在登录成功 即 VerifyConfig 中的 LOGIN_TOKEN 被赋值后才能进行自动化心跳 **
     *
     * @param entity
     * @return
     * @throws RuntimeException
     */
    public static void beginHeart(ApiBaseEntity entity) throws RuntimeException {
        if (StrUtil.isEmpty(VerifyConfig.LOGIN_TOKEN)) {
            throw new RuntimeException("没有登录 ->（VerifyConfig 中 LOGIN_TOKEN 未被赋值）");
        }

        // 开始心跳
        if (!StrUtil.isEmpty(VerifyConfig.verifyConfig.getHeartOpen()) &&
                VerifyConfig.verifyConfig.getHeartOpen().equals("开") &&
                !StrUtil.isEmpty(VerifyConfig.verifyConfig.getServerHeartTime())) {

            // 声明计算心跳时间
            long heartTime = VerifyConfig.verifyConfig.getHeartTime().equals("自动") ?
                    Long.valueOf(VerifyConfig.verifyConfig.getServerHeartTime()) / Long.valueOf(VerifyConfig.verifyConfig.getHeartRetryCount()) - 1L :
                    Long.valueOf(VerifyConfig.verifyConfig.getHeartTime());

            // 声明重试次数
            int retryCount = Integer.parseInt(VerifyConfig.verifyConfig.getHeartRetryCount());

            new Thread(() -> {
                while (true) {
                    if (retryCount < 0) {
                        System.out.println("心跳容错次数达到最大，心跳进程结束");
                        System.exit(1);
                    }

//                    sendWithRes()

                    try {
                        Thread.sleep(heartTime);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

}
