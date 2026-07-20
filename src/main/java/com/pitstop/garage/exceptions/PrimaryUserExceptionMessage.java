package com.pitstop.garage.exceptions;

public class PrimaryUserExceptionMessage {

    private PrimaryUserExceptionMessage() {}

    public static final String CANNOT_CHANGE_LAST_ADMIN_STATUS = "Cannot deactivate the last remaining admin.";

    public static final String CANNOT_CHANGE_LAST_ADMIN_ROLE = "Cannot change the role of the last remaining admin.";
}
