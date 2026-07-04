package com.aa.ABC;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.verify.sdk.config.ApiBaseEntity;
import com.verify.sdk.api.SingleInfoEntity;
import com.verify.sdk.config.VerifyConfig;
import com.verify.sdk.framework.FrameworkTool;
import com.verify.sdk.framework.ReqBuilder;

/** 基于 JS 网络验证 SDK 的验证器 */
public class JsVerifySdkVerifier implements KernelVerifier {

    private final VerifyConfigProfile profile;
    private final String apiUrl;
    private final String appId;
    private final String appKey;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public JsVerifySdkVerifier(VerifyConfigProfile profile,
                               String apiUrl, String appId, String appKey) {
        this.profile = profile;
        this.apiUrl = apiUrl;
        this.appId = appId;
        this.appKey = appKey;
    }

    @Override
    public void verifyCard(String card, VerifyCallback callback) {
        // 先查缓存
        if (callback != null && CacheManager.hasCachedCard(card)) {
            CacheManager.CardCache cached = CacheManager.getCachedCard(card);
            mainHandler.post(() -> callback.onSuccess(cached.type, cached.endTime, cached.status));
            return;
        }

        new Thread(() -> {
            // ★ 备份当前全局配置
            VerifyConfigProfile backup = VerifyConfigProfile.snapshot();
            try {
                // ★ 应用本内核的配置
                VerifyConfig.verifyConfig.setApiUrl(apiUrl);
                VerifyConfig.verifyConfig.setAppId(appId);
                VerifyConfig.verifyConfig.setAppKey(appKey);
                profile.apply();

                // ★ 调用 SDK
                SingleInfoEntity req = new SingleInfoEntity(card);
                ApiBaseEntity builderReq = ReqBuilder.builderReq(req);
                cn.hutool.json.JSONObject entries = FrameworkTool.sendWithRes(builderReq);
                cn.hutool.json.JSONObject data = entries.getJSONObject("data");

                final int typeInt = data.getInt("type");
                final String typeStr = getCardTypeName(typeInt);
                final String endTime = data.getStr("endTime");
                final String status = getCardStatus(endTime);

                // 写入缓存
                CacheManager.cacheCard(card, typeStr, endTime, status);

                // 主线程回调
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(typeStr, endTime, status));
                }

            } catch (final Throwable e) {
                Log.e("VERIFY_SDK", "卡密验证失败", e);
                final String errMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(errMsg));
                }
            } finally {
                // ★ 恢复全局配置
                backup.apply();
            }
        }).start();
    }

    private String getCardTypeName(int type) {
        switch (type) {
            case 0: return "小时卡";
            case 1: return "天卡";
            case 2: return "周卡";
            case 3: return "半月卡";
            case 4: return "月卡";
            case 5: return "季卡";
            case 6: return "半年卡";
            case 7: return "年卡";
            case 8: return "永久卡";
            case 9: return "次数卡";
            default: return "未知(" + type + ")";
        }
    }

    private String getCardStatus(String endTime) {
        if (endTime == null || endTime.isEmpty()) return "正常";
        if (endTime.contains("永久")) return "正常";
        try {
            String[] formats = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd",
                                "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd"};
            java.util.Date endDate = null;
            for (String fmt : formats) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault());
                    sdf.setLenient(false);
                    endDate = sdf.parse(endTime);
                    break;
                } catch (Exception ignored) {}
            }
            if (endDate == null) return "正常";
            if (endDate.before(new java.util.Date())) return "已到期";
            return "正常";
        } catch (Exception e) {
            return "正常";
        }
    }
}
