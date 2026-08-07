package com.pitstop.garage.exceptions;

public class CarHasActiveRepairException extends RuntimeException {

    public CarHasActiveRepairException(String message) {
        super(message);
    }
}
