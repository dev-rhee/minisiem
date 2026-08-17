package com.minisiem.collector;

public class LogParseException extends RuntimeException {

    public LogParseException(String message) {
        super(message);
    }

    public LogParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
