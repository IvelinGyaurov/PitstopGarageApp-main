package com.pitstop.garage.exceptions;

public class PrimaryUserExceptionMessage {

    private PrimaryUserExceptionMessage() {}

    public static final String CANNOT_CHANGE_LAST_ADMIN_STATUS = "error.cannotDeactivateLastAdmin";

    public static final String CANNOT_CHANGE_LAST_ADMIN_ROLE = "error.cannotChangeLastAdminRole";

    public static final String CANNOT_DEACTIVATE_MECHANIC_WITH_OPEN_REPAIRS =
            "error.cannotDeactivateMechanicWithOpenRepairs";

    public static final String CANNOT_CHANGE_ROLE_WITH_OPEN_REPAIRS =
            "error.cannotChangeRoleWithOpenRepairs";
}
