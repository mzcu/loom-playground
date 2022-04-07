package loom.sc;

import jdk.incubator.concurrent.ScopeLocal;
import loom.sc.impl.ConsoleLogger;
import loom.sc.impl.TestLogger;
import loom.sc.inject.Environment;
import loom.sc.support.RequestContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static loom.sc.support.Delays.delayMs;
import static loom.sc.support.Scopes.allOf;

public class Main {

    public record F<T, R>(Function<T, R> fn) {

        public Callable<R> bind(T arg) {
            return () -> this.fn().apply(arg);
        }

        public <N> F<T, N> andThen(Function<R, N> fn) {
            return new F<>(this.fn().andThen(fn));
        }

        public F<T, R> effect(Consumer<R> consumer) {
            return this.andThen((R r) -> {
                consumer.accept(r);
                return r;
            });
        }

        public static <T, R> F<T, R> task(Function<T, R> fn) {
            return new F<>(fn);
        }
    }
    public static Runnable runWithContext(Runnable op, ScopeLocal.Carrier ... carriers) {
        var i = 0;
        var j = carriers.length - 1;
        ScopeLocal.Carrier tmp;
        while (i < j) {
            tmp = carriers[j];
            carriers[j] = carriers[i];
            carriers[i] = tmp;
            i++; j--;
        }
        return Arrays.stream(carriers).reduce(op,
                (runnable, carrier) -> () -> carrier.run(runnable),
                (r1, r2) -> r2 // combiner not used in a sequential stream
        );
    }
    private static void testRun() {
        // Inject deps to environment
        var env = new Environment(new TestLogger()).bind();
        // Setup request context
        var ctx = new RequestContext("client-1", UUID.randomUUID().toString()).bind();
        // Run application code
        env.run(() -> {
            ctx.run(Main::app);
            if (Environment.get().logger() instanceof TestLogger tl) {
                System.out.println("Printing collected messages from test logger:");
                tl.loggedMessages().forEach(System.out::println);
            }
        });
    }
    private static void logError(Exception e) {
        var str = new StringWriter();
        e.printStackTrace(new PrintWriter(str));
        Environment.get().logger().log("Error occurred while processing %s%nStack trace: %s%n"
                .formatted(RequestContext.get(), str.toString().replaceAll("^", "\n\t")));
    }


    public static void main(String[] args) {
        productionRun();
    }

    private static void productionRun() {
        // Inject deps to environment
        var env = new Environment(new ConsoleLogger()).bind();
        // Setup request context
        var ctx = new RequestContext("client-1", UUID.randomUUID().toString(),
                Instant.now(), Instant.now().plusSeconds(120)).bind();
        // Run application code
        runWithContext(Main::app, env, ctx).run();
    }


    private static void app() {

        var logger = Environment.get().logger();

        var action1 = F.task(Double::parseDouble)
                .andThen(Math::sqrt)
                .effect(delayMs(2000))
                .andThen(Object::toString)
                 //.effect(s -> { throw new RuntimeException("ex"); })
                .effect(logger::log);

        var action2 = F.task(Base64.getEncoder()::encodeToString)
                        .effect(delayMs(1000))
                        .andThen(Base64.getDecoder()::decode)
                        .andThen(String::new)
                        .effect(logger::log);

        try {
            var c1 = action1.bind("64.0");
            var c2 = action2.bind("test".getBytes(StandardCharsets.UTF_8));
            var result = allOf(c1, c2);
            logger.log("Result '%s' returned after %s %n".formatted(result, RequestContext.get().duration()));
        } catch (Exception e) {
            logError(e);
        }
    }

}
