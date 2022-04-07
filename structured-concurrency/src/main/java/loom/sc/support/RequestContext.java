package loom.sc.support;

import jdk.incubator.concurrent.ScopeLocal;

import java.time.Duration;
import java.time.Instant;

public record RequestContext(
        String clientId,
        String traceId,
        Instant createdAt,
        Instant deadline) {

    public RequestContext(String clientId, String traceId) {
        this(clientId, traceId, Instant.now(), Instant.now().plusMillis(5000));
    }

    public Duration duration() {
        return Duration.between(createdAt, Instant.now());
    }

    public String debugInfo() {
        var ctx = RequestContext.get();
        return "[traceId=%s][%-33s][elapsed=%-13s]".formatted(
                ctx.traceId,
                "thread=" + Thread.currentThread().getThreadGroup().getName() + "/" + Thread.currentThread().getName(),
                ctx.duration());
    }

    public static RequestContext get() {
        return scopeLocal.get();
    }

    public ScopeLocal.Carrier bind() {
        return ScopeLocal.where(scopeLocal, this);
    }

    private static final ScopeLocal<RequestContext> scopeLocal = ScopeLocal.newInstance();

}

