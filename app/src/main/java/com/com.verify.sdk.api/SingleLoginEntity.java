package com.verify.sdk.api;

import com.verify.sdk.config.ApiBaseEntity;
import com.verify.sdk.config.VerifyConfig;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName SingleLoginEntity.java
 * @Description 单码登录接口
 * @createTime 2025年08月02日 22:04
 */
public class SingleLoginEntity extends ApiBaseEntity {

    // 设备机器码
    private String mac;
    // 设备卡密
    private String card;

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public SingleLoginEntity(String mac, String card) {
        super.setApiUrl("/api/single/login");
        this.mac = mac;
        this.card = card;
    }

}
