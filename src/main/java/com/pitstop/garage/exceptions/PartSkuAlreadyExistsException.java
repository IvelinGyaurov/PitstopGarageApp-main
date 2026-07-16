package com.pitstop.garage.exceptions;

public class PartSkuAlreadyExistsException extends RuntimeException {

    public PartSkuAlreadyExistsException(String message) {
        super(message);
    }
}