package edu.self.w2k.kindling;

public class KindlingException extends Exception {

    public KindlingException(String message) {
        super(message);
    }

    public KindlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
