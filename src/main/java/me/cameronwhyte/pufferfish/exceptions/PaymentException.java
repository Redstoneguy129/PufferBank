package me.cameronwhyte.pufferfish.exceptions;

public class PaymentException extends BankException {
    public PaymentException(String message) {
        super(message);
    }

    @Override
    public String getTitle() {
        return "Payment Error";
    }
}
