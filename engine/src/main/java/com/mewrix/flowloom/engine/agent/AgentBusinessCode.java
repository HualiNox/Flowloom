package com.mewrix.flowloom.engine.agent;

import com.mewrix.flowloom.engine.common.response.BusinessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Agent 模块的业务码。 */
@Getter
@RequiredArgsConstructor
public enum AgentBusinessCode implements BusinessCode {
    ;

    private final String messageKey;

    @Override
    public String getCode() {
        return name();
    }
}
