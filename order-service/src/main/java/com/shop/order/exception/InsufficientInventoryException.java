package com.shop.order.exception;

public class InsufficientInventoryException extends ConflictException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}
