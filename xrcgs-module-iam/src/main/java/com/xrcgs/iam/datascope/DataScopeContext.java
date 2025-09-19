package com.xrcgs.iam.datascope;

import java.util.function.Supplier;

/**
 * Simple holder to expose the effective data scope for the duration of a request.
 */
public final class DataScopeContext {

    private static final ThreadLocal<EffectiveDataScope> CONTEXT = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static EffectiveDataScope get() {
        return CONTEXT.get();
    }

    public static void set(EffectiveDataScope scope) {
        if (scope == null) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(scope);
        }
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static <T> T withScope(EffectiveDataScope scope, Supplier<T> supplier) {
        EffectiveDataScope previous = CONTEXT.get();
        CONTEXT.set(scope);
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
        }
    }
}
