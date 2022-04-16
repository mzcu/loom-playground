package loom.cont;

import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;

/**
 * A toy example of building async/await on top of JVM continuations
 * <br>
 * Requires following java options to run:
 *
 * <pre>
 *      --add-opens java.base/jdk.internal.vm=ALL-UNNAMED
 * </pre>
 *
 */
public class AsyncAwait {


    public static final ContinuationScope SCHEDULER = new ContinuationScope("scheduler");
    private static final Queue<VContinuation<?>> readyQueue = new LinkedList<>();
    private static boolean running = true;
    private static boolean trace = false;
    private static final Runnable scheduler = () -> {
        while (running && !readyQueue.isEmpty()) {
            var result = readyQueue.poll().call();
            if (trace && result != null) {
                System.out.printf("readyQueue size: %d; produced %s%n", readyQueue.size(), result);
            }
        }
    };

    public static void async(Runnable r) {
        readyQueue.add(new VContinuation<>(SCHEDULER, () -> {
            r.run();
            return null;
        }));
    }

    public static <T> Awaitable<T> async(Callable<T> c) {
        return new Awaitable<>(c);
    }

    public static <T> T await(Callable<T> target) {
        //noinspection rawtypes
        var parent = ((VContinuation.Cont) Continuation.getCurrentContinuation(SCHEDULER)).context();
        var cont = new VContinuation<>(SCHEDULER, () -> {
            var result = target.call();
            readyQueue.add(new VContinuation<>(SCHEDULER, () -> {
                AsyncAwait.yield(parent);
                return null;
            }));
            return result;
        });
        readyQueue.add(cont);
        Continuation.yield(SCHEDULER);
        return cont.result();
    }

    public static void yield(VContinuation<?> c) {
        readyQueue.add(c);
        Continuation.yield(SCHEDULER);
    }

    public static void yield() {
        //noinspection rawtypes
        readyQueue.add(((VContinuation.Cont) Continuation.getCurrentContinuation(SCHEDULER)).context());
        Continuation.yield(SCHEDULER);
    }

    public static void start() {
        running = true;
        scheduler.run();
    }

    public static void startAndTrace() {
        trace = true;
        start();
    }

    public static void stop() {
        running = false;
    }

    // Pending result
    public static class Awaitable<T> {

        private final VContinuation<T> cont;

        public Awaitable(Callable<T> target) {
            cont = new VContinuation<>(SCHEDULER, target);
            readyQueue.add(cont);
            AsyncAwait.yield();
        }

        public T await() {
            try {
                while (!cont.isDone()) {
                    AsyncAwait.yield();
                }
                return cont.result();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Valued continuation
    public static class VContinuation<T> implements Callable<T> {

        public final ContinuationScope scope;
        public final VContinuation<T>.Cont cont;
        private T value;

        public VContinuation(ContinuationScope scope, Callable<T> target) {
            this.scope = scope;
            this.cont = new VContinuation<T>.Cont(scope, () -> {
                try {
                    this.value = target.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        public T call() {
            cont.run();
            return value;
        }

        public boolean isDone() {
            return cont.isDone();
        }

        public T result() {
            return value;
        }

        public class Cont extends Continuation {
            public Cont(ContinuationScope scope, Runnable target) {
                super(scope, target);
            }
            public VContinuation<T> context() {
                return VContinuation.this;
            }
        }

    }

    public static void main(String[] args) {

        async(() -> {
            var a = await(() -> "a");
            var inner = async(() -> {
                var a1 = async(() -> "a1");
                var b1 = await(() -> "b1");
                var c1 = await(() -> "c1");
                return a1.await() + ", " + b1 + ", " + c1;
            });
            var d = await(() -> "d");
            System.out.println("Result: " + a + ", " + inner.await() + ", " + d);
        });

        startAndTrace();
        /*
        Output:

        readyQueue size: 1; produced a
        readyQueue size: 2; produced a1
        readyQueue size: 2; produced d
        readyQueue size: 2; produced b1
        readyQueue size: 2; produced c1
        readyQueue size: 1; produced a1, b1, c1
        Result: a, a1, b1, c1, d
        */

    }
}
