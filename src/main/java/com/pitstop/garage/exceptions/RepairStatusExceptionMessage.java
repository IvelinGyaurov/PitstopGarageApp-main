package com.pitstop.garage.exceptions;

public class RepairStatusExceptionMessage {

    private RepairStatusExceptionMessage() {}

    public static final String REPAIR_STATUS_UNAUTHORIZED = "Only pending repairs can be cancelled";
}
