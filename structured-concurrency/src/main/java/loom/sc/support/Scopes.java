package loom.sc.support;

import jdk.incubator.concurrent.StructuredTaskScope;
import loom.sc.inject.Environment;
import loom.sc.support.Tuple.Tuple2;

import java.util.concurrent.Callable;

public class Scopes {


    public static <T1, T2> Tuple2<T1, T2> allOf(Callable<T1> task1, Callable<T2> task2) {
        var logger = Environment.get().logger();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var ctx = RequestContext.get();
            logger.log("Starting tasks");
            var t1 = scope.fork(task1);
            var t2 = scope.fork(task2);
            logger.log("Waiting for tasks to complete");
            scope.joinUntil(ctx.deadline());
            scope.throwIfFailed();
            logger.log("Tasks completed within deadline");
            return new Tuple2<>(t1.resultNow(), t2.resultNow());
        } catch (Exception e) {
            logger.log("Tasks completed with error");
            throw new StructuredTaskException(e);
        }
    }

}
