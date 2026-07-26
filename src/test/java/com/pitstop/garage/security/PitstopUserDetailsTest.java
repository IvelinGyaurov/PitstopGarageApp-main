package com.pitstop.garage.security;

import com.pitstop.garage.user.model.UserRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PitstopUserDetailsTest {

    @Test
    void accountFlags_returnTrueWhenActive() {
        PitstopUserDetails details = new PitstopUserDetails(
                UUID.randomUUID(), "user", "pass", UserRole.USER, true);

        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
        assertTrue(details.isEnabled());
    }
}
