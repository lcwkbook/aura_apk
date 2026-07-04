package com.aa.ABC;

/** 内核验证器接口 — 每种内核/验证系统实现一个 */
public interface KernelVerifier {
    /**
     * 验证卡密
     * @param card     卡密字符串
     * @param callback 回调（主线程）
     */
    void verifyCard(String card, VerifyCallback callback);

    /** 验证回调接口 */
    interface VerifyCallback {
        void onSuccess(String type, String endTime, String status);
        void onError(String errorMsg);
    }
}
