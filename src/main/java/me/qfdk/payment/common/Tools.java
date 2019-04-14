package me.qfdk.payment.common;

public class Tools {
    public static String generateTransactionNumber() {
        return "nasi-pay-" + System.currentTimeMillis()
                + (long) (Math.random() * 10000000L);
    }
}
