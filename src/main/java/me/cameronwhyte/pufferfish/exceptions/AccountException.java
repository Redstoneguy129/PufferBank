package me.cameronwhyte.pufferfish.exceptions;

public class AccountException extends BankException {

    public AccountException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "Account Error";
    }
}
