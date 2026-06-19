package com.verify.sdk.api;

import cn.hutool.core.util.StrUtil;
import com.verify.sdk.config.ApiBaseEntity;
import com.verify.sdk.config.VerifyConfig;

/**
 * @author Anc
 * @version 1.0.0
 * @ClassName CommonVariableEntity.java
 * @Description 获取变量接口
 * @createTime 2025年08月02日 22:04
 */
public class CommonVariableEntity extends ApiBaseEntity {

    // 登录成功后分发的 TOKEN
    private String token;
    // 变量ID（应用管理 -> 变量管理 -> 变量编号）
    private String variableId;

    public CommonVariableEntity(String token, String variableId) {
        super.setApiUrl("/api/expand/variable");
        this.token = StrUtil.isEmpty(token) ? VerifyConfig.LOGIN_TOKEN : token;
        this.variableId = variableId;
    }

}
