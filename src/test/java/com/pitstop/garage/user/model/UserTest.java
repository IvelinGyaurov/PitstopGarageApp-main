package com.pitstop.garage.user.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserTest {

    @Test
    void onCreate_setsCreatedOnWhenMissing() throws Exception {
        User user = User.builder()
                .username("u")
                .email("u@mail.com")
                .password("p")
                .role(UserRole.USER)
                .isActive(true)
                .build();

        Method onCreate = User.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(user);

        assertNotNull(user.getCreatedOn());
    }

    @Test
    void onCreate_keepsExistingCreatedOn() throws Exception {
        LocalDateTime existing = LocalDateTime.of(2024, 1, 1, 10, 0);
        User user = User.builder()
                .username("u")
                .email("u@mail.com")
                .password("p")
                .role(UserRole.USER)
                .isActive(true)
                .createdOn(existing)
                .build();

        Method onCreate = User.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(user);

        assertEquals(existing, user.getCreatedOn());
    }
}
