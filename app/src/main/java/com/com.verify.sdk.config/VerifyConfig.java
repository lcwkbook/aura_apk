package com.verify.sdk.config;

import cn.hutool.core.lang.Singleton;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName VerifyConfig.java
 * @Description 核心配置类
 * @createTime 2025年08月02日 21:16
 */
public class VerifyConfig {

    // 站点地址
    private String apiUrl;

    public static String LOGIN_TOKEN;

    // 应用编号（应用管理 -> 应用编号）
    private String appId;

    // 应用密钥（应用管理 -> 应用密钥）
    private String appKey;

    // 加密算法（接口管理 -> 接口安全 此处的格式为[加密方式,加密密钥设置]
    // 例如[AES对称加密,AES/ECB/PKCS5Padding(无需IV-偏移)]）
    // 例如[RSA非对称加密,默认]）
    // 例如[不加密]）
    private String[] secretType;

    // 加密密钥（接口管理 -> 加密密钥）
    // 例如当选择的是aes对称加密的ECB无需IV时则为：[AXmQDiGMzws8cJAa4StnYG6DRNn5f3Kz]
    // 例如当选择的是aes对称加密的CTS需要IV时则为：[AXmQDiGMzws8cJAa4StnYG6DRNn5f3Kz:CAxzCscZZWkKaiNk]
    // 例如当选择的是RSA非对称加密时为：[公钥]
    private String[] secretKey;

    // 编码方式（接口管理 -> 加密编码方式）
    // 此处可选(16进制编码) / (Base64编码)
    private String encodeType;

    // 请求加密方式（接口管理 -> 请求加密方式）
    // 此处可选(不加密) / (仅加密参数值) / (全部加密)
    private String reqType;

    // 响应加密方式（接口管理 -> 响应加密方式）
    // 此处可选 (不加密) / (全部加密)
    private String resType;

    // 随机数防劫持（接口管理 -> 随机数防劫持）
    // 此处可选(关) / (开)
    private String randomType;

    // 签名方式（接口管理 -> 签名方式）
    // 此处可选 (不计算) / (MD5) / (SHA1) / (SHA256)
    private String signType;

    // 签名计算规则（接口管理 -> 签名计算规则）
    // 此处可选 (不计算) / (方式一) / (方式二)
    private String signRule;

    // 【**不在网站上的配置，本地配置即可，无需到网站上配置**】
    // 本地时间戳验证（此处是本地的时间戳验证，将验证响应从服务器发出到客户端的时间差，若时间差过高，校验是否被静态注入）
    // 此处一般是 5000 (5 秒) 如果不需要则填 "0" 或者 填 "空"
    private String localTimeVerify;

    // 是否开启心跳验证（单码应用 / 会员应用 -> 心跳间隔需大于 0 ）
    // 站点上建议根据自己的业务情况填写心跳时间，例如 站点上填写 300 则为 5 分钟内必须发一条心跳
    // 则客户端心跳时间建议为 服务端心跳时间 / 心跳重试次数 - 2秒
    // 公式为 本地心跳多久跳一次（注意此处单位是秒，但在客户端中需要 * 1000 才是秒） = 服务端填写的心跳时间 / 心跳重试次数 - 1秒
    // 【服务端填写的心跳时间】在“单码应用 / 会员应用 的用户安全配置中”
    // 此处可选(关) / (开)
    private String heartOpen;


    // 服务器设置的心跳时间（会员应用 / 单码应用 -> 用户安全配置）
    // 此处填写服务器上填写的值即可，若站点配置上填写的为 0 则认为 heartOpen字段无论是 开 还是 关，均认为是 关
    private String serverHeartTime;

    // 【**不在网站上的配置，本地配置即可，无需到网站上配置**】
    // 心跳多久跳一次
    // 此处建议看上述的 heartOpen 的公式说明
    // 此处可选 自己填 int 类型的数字 或 “自动”
    // 填写自动时将根据 【本地心跳多久跳一次（注意此处单位是秒，但在客户端中需要 * 1000 才是秒） = 服务端填写的心跳时间 / 心跳重试次数 - 1秒】作为计算公式计算
    //
    // 【服务端填写的心跳时间】在“单码应用 / 会员应用 的用户安全配置中”
    private String heartTime;

    // 【**不在网站上的配置，本地配置即可，无需到网站上配置**】
    // 心跳重试次数
    // 一般客户端可能由于网络波动，导致某次心跳失败，所以这里是一个容错，允许客户端发生 N 次的失败
    private String heartRetryCount;

    // 自定义响应码（接口管理 -> 自定义响应码 -> 设置业务逻辑成功状态码）
    // 此处请根据您后台的配置填写 ，若不填写则默认为 1
    private String logicCode;
    private transient volatile byte[] currentDHKey;

    public byte[] getCurrentDHKey() {
    return currentDHKey;
    }

    public void setCurrentDHKey(byte[] currentDHKey) {
    this.currentDHKey = currentDHKey;
    }

    // 以下代码均为对上述变量的暴露，无需修改------------------------------------------------------------------------------------------------------------------------------------
    // 以下代码均为对上述变量的暴露，无需修改------------------------------------------------------------------------------------------------------------------------------------
    // 以下代码均为对上述变量的暴露，无需修改------------------------------------------------------------------------------------------------------------------------------------


    public VerifyConfig() {
    }

    public String getServerHeartTime() {
        return serverHeartTime;
    }

    public void setServerHeartTime(String serverHeartTime) {
        this.serverHeartTime = serverHeartTime;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String[] getSecretType() {
        return secretType;
    }

    public void setSecretType(String[] secretType) {
        this.secretType = secretType;
    }

    public String[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String[] secretKey) {
        this.secretKey = secretKey;
    }

    public String getEncodeType() {
        return encodeType;
    }

    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }

    public String getReqType() {
        return reqType;
    }

    public void setReqType(String reqType) {
        this.reqType = reqType;
    }

    public String getResType() {
        return resType;
    }

    public void setResType(String resType) {
        this.resType = resType;
    }

    public String getRandomType() {
        return randomType;
    }

    public void setRandomType(String randomType) {
        this.randomType = randomType;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getSignRule() {
        return signRule;
    }

    public void setSignRule(String signRule) {
        this.signRule = signRule;
    }

    public String getLocalTimeVerify() {
        return localTimeVerify;
    }

    public void setLocalTimeVerify(String localTimeVerify) {
        this.localTimeVerify = localTimeVerify;
    }

    public String getHeartOpen() {
        return heartOpen;
    }

    public void setHeartOpen(String heartOpen) {
        this.heartOpen = heartOpen;
    }

    public String getHeartTime() {
        return heartTime;
    }

    public void setHeartTime(String heartTime) {
        this.heartTime = heartTime;
    }

    public String getHeartRetryCount() {
        return heartRetryCount;
    }

    public void setHeartRetryCount(String heartRetryCount) {
        this.heartRetryCount = heartRetryCount;
    }

    public String getLogicCode() {
        return logicCode;
    }

    public void setLogicCode(String logicCode) {
        this.logicCode = logicCode;
    }

    public VerifyConfig getVerifyConfig() {
        return verifyConfig;
    }

    public void setVerifyConfig(VerifyConfig verifyConfig) {
        this.verifyConfig = verifyConfig;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public static VerifyConfig verifyConfig = Singleton.get(VerifyConfig.class);

}
