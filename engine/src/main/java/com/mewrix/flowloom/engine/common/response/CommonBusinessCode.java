package com.mewrix.flowloom.engine.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 跨模块通用业务码。 */
@Getter
@RequiredArgsConstructor
public enum CommonBusinessCode implements BusinessCode {
    OK("common.ok");

    private final String messageKey;

    @Override
    public String getCode() {
        return this.name();
    }
}
