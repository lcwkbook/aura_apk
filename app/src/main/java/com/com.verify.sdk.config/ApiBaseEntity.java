package com.verify.sdk.config;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName ApiBaseEntity.java
 * @Description API基建
 * @createTime 2025年08月02日 22:02
 * <p>
 * 此处是API基建参数
 * 不要修改！！！
 */
public class ApiBaseEntity {

    private String apiUrl;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = VerifyConfig.verifyConfig.getApiUrl() + apiUrl;
    }

    private String appId;
    private String safeCode;
    private String signature;
    private String timestamp;

    private String encryptParams;
    private String bob;

    // 添加 getter/setter：
public String getBob() {
    return bob;
}

public void setBob(String bob) {
    this.bob = bob;
}

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSafeCode() {
        return safeCode;
    }

    public void setSafeCode(String safeCode) {
        this.safeCode = safeCode;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEncryptParams() {
        return encryptParams;
    }

    public void setEncryptParams(String encryptParams) {
        this.encryptParams = encryptParams;
    }
}
