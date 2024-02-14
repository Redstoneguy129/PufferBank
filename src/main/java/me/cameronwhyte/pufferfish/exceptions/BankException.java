package me.cameronwhyte.pufferfish.exceptions;

public abstract class BankException extends RuntimeException {

    public BankException(String message) {
        super(message, null, false, false);
    }

    public abstract String getTitle();
}
