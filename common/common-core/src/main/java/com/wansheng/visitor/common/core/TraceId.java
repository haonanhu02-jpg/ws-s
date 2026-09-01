package com.wansheng.visitor.common.core;

import java.util.UUID;

public final class TraceId {
    private TraceId() {}

    public static String create() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

