package loom.sc.support;

import java.util.function.Consumer;

public class Delays {

    public static <T> Consumer<T> delayMs(long millis) {
        return (T t) -> {
            try {
                Thread.sleep(millis);
            } catch (Exception e) {
                throw new StructuredTaskException(e);
            }
            ;
        };
    }


}
