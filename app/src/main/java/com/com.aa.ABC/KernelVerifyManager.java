package com.aa.ABC;

import android.util.SparseArray;

/**
 * 内核验证管理器
 * 管理所有内核的验证器，支持动态注册
 */
public class KernelVerifyManager {

    // kernelId → 验证器
    private final SparseArray<KernelVerifier> verifiers = new SparseArray<>();

    /** 注册一个内核的验证器 */
    public void register(int kernelId, KernelVerifier verifier) {
        verifiers.put(kernelId, verifier);
    }

    /** 获取指定内核的验证器 */
    public KernelVerifier getVerifier(int kernelId) {
        return verifiers.get(kernelId);
    }

    /** 快捷验证 */
    public void verify(int kernelId, String card, KernelVerifier.VerifyCallback callback) {
        KernelVerifier verifier = verifiers.get(kernelId);
        if (verifier == null) {
            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .post(() -> callback.onError("未注册的内核验证器: kernelId=" + kernelId));
            }
            return;
        }
        verifier.verifyCard(card, callback);
    }
}
