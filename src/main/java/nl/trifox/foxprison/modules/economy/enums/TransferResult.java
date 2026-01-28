package nl.trifox.foxprison.modules.economy.enums;

public enum TransferResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    SELF_TRANSFER,
    INVALID_AMOUNT,
    RECIPIENT_MAX_BALANCE
}