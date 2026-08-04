package com.pitstop.garage.repair.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RepairStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    USER_CANCELLED("Cancelled by user"),
    EXPIRED("Expired");

    private final String displayName;
}
