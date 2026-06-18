package com.verify.sdk.api;

import com.verify.sdk.config.ApiBaseEntity;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName SingleLoginEntity.java
 * @Description 获取单码信息接口
 * @createTime 2025年08月02日 22:04
 */
public class SingleInfoEntity extends ApiBaseEntity {

    // 设备卡密
    private String card;


    public String getCard() {
        return card;
    }

    public void setCard(String card) {
        this.card = card;
    }

    public SingleInfoEntity(String card) {
        super.setApiUrl("/api/single/info");
        this.card = card;
    }

}
