package loom.sc.inject;

import jdk.incubator.concurrent.ScopeLocal;

public record Environment(Logger logger) {

    public static Environment get() {
        return scopeLocal.get();
    }

    public ScopeLocal.Carrier bind() {
        return ScopeLocal.where(scopeLocal, this);
    }

    private static final ScopeLocal<Environment> scopeLocal = ScopeLocal.newInstance();

}

