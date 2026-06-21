package com.pitstop.garage.exceptions;

public class PrimaryUserExceptionMessage {

    private PrimaryUserExceptionMessage() {}

    public static final String CANNOT_CHANGE_PRIMARY_USER_STATUS = "Cannot change primary user status";

    public static final String CANNOT_CHANGE_PRIMARY_USER_ROLE = "The first registered user role cannot be changed.";
}
