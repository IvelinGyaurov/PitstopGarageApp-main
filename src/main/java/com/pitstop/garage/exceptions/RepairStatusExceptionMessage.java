package com.pitstop.garage.exceptions;

public class RepairStatusExceptionMessage {

    private RepairStatusExceptionMessage() {}

    public static final String REPAIR_STATUS_UNAUTHORIZED = "error.repairCancelOnlyPending";

    public static final String REPAIR_NOT_PENDING = "error.repairOnlyPending";

    public static final String REPAIR_ALREADY_TAKEN = "error.repairAlreadyTaken";

    public static final String REPAIR_NOT_ASSIGNED_TO_MECHANIC = "error.repairNotAssigned";

    public static final String REPAIR_NOT_ACCEPTED = "error.repairOnlyAccepted";

    public static final String REPAIR_NOT_IN_PROGRESS = "error.repairOnlyInProgress";
}
