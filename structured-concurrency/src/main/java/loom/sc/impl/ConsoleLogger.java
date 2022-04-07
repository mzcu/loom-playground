package loom.sc.impl;

import loom.sc.inject.Logger;
import loom.sc.support.RequestContext;

public class ConsoleLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.printf("%s - %s%n", RequestContext.get().debugInfo(), message);
    }
}
