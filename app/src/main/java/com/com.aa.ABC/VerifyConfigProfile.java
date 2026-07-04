package com.aa.ABC;

import com.verify.sdk.config.VerifyConfig;

public class VerifyConfigProfile {

    // ===== 改为 public，允许外部直接赋值 =====
    public String[] secretType;
    public String[] secretKey;
    public String encodeType;
    public String reqType;
    public String resType;
    public String randomType;
    public String signType;
    public String signRule;
    public String localTimeVerify;
    public String logicCode;
    public String heartOpen;

    // ===== 从当前全局 VerifyConfig 读取 =====
    public static VerifyConfigProfile snapshot() {
        VerifyConfigProfile p = new VerifyConfigProfile();
        p.secretType   = VerifyConfig.verifyConfig.getSecretType();
        p.secretKey    = VerifyConfig.verifyConfig.getSecretKey();
        p.encodeType   = VerifyConfig.verifyConfig.getEncodeType();
        p.reqType      = VerifyConfig.verifyConfig.getReqType();
        p.resType      = VerifyConfig.verifyConfig.getResType();
        p.randomType   = VerifyConfig.verifyConfig.getRandomType();
        p.signType     = VerifyConfig.verifyConfig.getSignType();
        p.signRule     = VerifyConfig.verifyConfig.getSignRule();
        p.localTimeVerify = VerifyConfig.verifyConfig.getLocalTimeVerify();
        p.logicCode    = VerifyConfig.verifyConfig.getLogicCode();
        p.heartOpen    = VerifyConfig.verifyConfig.getHeartOpen();
        return p;
    }

    // ===== 将快照应用到全局 VerifyConfig =====
    public void apply() {
        VerifyConfig.verifyConfig.setSecretType(secretType);
        VerifyConfig.verifyConfig.setSecretKey(secretKey);
        VerifyConfig.verifyConfig.setEncodeType(encodeType);
        VerifyConfig.verifyConfig.setReqType(reqType);
        VerifyConfig.verifyConfig.setResType(resType);
        VerifyConfig.verifyConfig.setRandomType(randomType);
        VerifyConfig.verifyConfig.setSignType(signType);
        VerifyConfig.verifyConfig.setSignRule(signRule);
        VerifyConfig.verifyConfig.setLocalTimeVerify(localTimeVerify);
        VerifyConfig.verifyConfig.setLogicCode(logicCode);
        VerifyConfig.verifyConfig.setHeartOpen(heartOpen);
    }
}
