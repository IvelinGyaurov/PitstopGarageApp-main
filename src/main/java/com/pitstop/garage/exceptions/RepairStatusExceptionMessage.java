package com.pitstop.garage.exceptions;

public class RepairStatusExceptionMessage {

    private RepairStatusExceptionMessage() {}

    public static final String REPAIR_STATUS_UNAUTHORIZED = "Only pending repairs can be cancelled";

    public static final String REPAIR_NOT_PENDING = "Only pending repairs can be updated.";

    public static final String REPAIR_ALREADY_TAKEN = "This repair has already been taken by another mechanic.";

    public static final String REPAIR_NOT_ASSIGNED_TO_MECHANIC = "You are not assigned to this repair.";

    public static final String REPAIR_NOT_ACCEPTED = "Only accepted repairs can be started.";

    public static final String REPAIR_NOT_IN_PROGRESS = "Only in-progress repairs can be completed.";
}
