package loom.sc.impl;

import loom.sc.inject.Logger;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class TestLogger implements Logger {
    private final Queue<String> loggedMessages = new ConcurrentLinkedQueue<>();

    @Override
    public void log(String message) {
        loggedMessages.add(message);
    }

    public String lastMessage() {
        return loggedMessages.peek();
    }

    public Stream<String> loggedMessages() {
        return loggedMessages.stream();
    }
}
