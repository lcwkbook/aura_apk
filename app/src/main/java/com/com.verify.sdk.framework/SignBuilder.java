package com.verify.sdk.framework;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import com.verify.sdk.config.VerifyConfig;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName SignBuilder.java
 * @Description
 * @createTime 2025年08月02日 22:47
 */
public class SignBuilder {

    /**
     * 传入字符串，计算签名
     * 其中必须存在字段 timestamp
     *
     * @return
     */
    public static String builderSign(JSONObject originalJson) {
        JSONObject params = new JSONObject();
        try {
            params = originalJson.clone();
        } catch (CloneNotSupportedException e) {
            Console.error("JSON CLONE 失败");
        }
        // params.remove("appId");
        params.remove("signature");
        params.remove("apiUrl");
        params.remove("encryptParams");

        String waitSignStr = sortByAscii(params);

        // 签名规则
        if (StrUtil.isEmpty(VerifyConfig.verifyConfig.getSignRule()) || VerifyConfig.verifyConfig.getSignRule().equals("不计算")) {
            return "";
        } else if (VerifyConfig.verifyConfig.getSignRule().equals("方式一")) {
            waitSignStr = params.getStr("timestamp") + VerifyConfig.verifyConfig.getAppKey();
        } else if (VerifyConfig.verifyConfig.getSignRule().equals("方式二")) {
            waitSignStr = waitSignStr + VerifyConfig.verifyConfig.getAppKey();
                        // ===== 看签名原始字符串 =====
            android.util.Log.d("VERIFY_SIGN", "签名字符串: " + waitSignStr);
        } else {
            return "";
        }

        // 签名算法
        if (StrUtil.isEmpty(VerifyConfig.verifyConfig.getSignType()) || VerifyConfig.verifyConfig.getSignType().equals("不计算")) {
            Console.log("没有选择正确的 签名方式 （signType 选择 不计算 或 MD5 或 SHA1 或 SHA256）");
            return "";
        }

        if (VerifyConfig.verifyConfig.getSignType().equals("MD5")) {
            return DigestUtil.md5Hex(waitSignStr);
        }
        if (VerifyConfig.verifyConfig.getSignType().equals("SHA1")) {
            return DigestUtil.sha1Hex(waitSignStr);
        }
        if (VerifyConfig.verifyConfig.getSignType().equals("SHA256")) {
            return DigestUtil.sha256Hex(waitSignStr);
        } else {
            return "";
        }
    }


    public static String sortByAscii(JSONObject jsonObject) {
        StringBuilder sb = new StringBuilder();
        // 使用TreeMap来确保排序是按字典顺序的
        Map<String, String> sortedMap = new TreeMap<>();

        // 递归提取键值对
        extractParams(jsonObject, sortedMap);

        // 拼接成字符串
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }

        // 去掉最后一个多余的 "&"
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }


    // 递归方法，提取所有的参数
    private static void extractParams(JSONObject jsonObject, Map<String, String> map) {
        Iterator<String> keys = jsonObject.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);

            if (value instanceof JSONObject) {
                // 如果是嵌套的JSONObject，递归调用
                extractParams((JSONObject) value, map);
            } else {
                // 如果是基本类型，直接加入到排序的map中
                value = value == null ? "" : value;
                map.put(key, value.toString());
            }
        }
    }
}
