package loom.sc.support;

public class Log {
    public static void log(String message) {
        System.out.printf("%s - %s%n", RequestContext.get().debugInfo(), message);
    }
}
