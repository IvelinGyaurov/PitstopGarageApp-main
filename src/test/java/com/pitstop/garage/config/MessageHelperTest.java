package com.pitstop.garage.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageHelperTest {

    @Mock
    private MessageSource messageSource;

    private MessageHelper messageHelper;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        messageHelper = new MessageHelper(messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void get_withoutArgs_resolvesMessage() {
        when(messageSource.getMessage("flash.car.added", null, "flash.car.added", Locale.ENGLISH))
                .thenReturn("Car added successfully.");

        assertEquals("Car added successfully.", messageHelper.get("flash.car.added"));
        verify(messageSource).getMessage("flash.car.added", null, "flash.car.added", Locale.ENGLISH);
    }

    @Test
    void get_withArgs_resolvesParameterizedMessage() {
        Object[] args = {"BMW 320d"};
        when(messageSource.getMessage("confirm.deleteCar", args, "confirm.deleteCar", Locale.ENGLISH))
                .thenReturn("Delete BMW 320d?");

        assertEquals("Delete BMW 320d?", messageHelper.get("confirm.deleteCar", args));
        verify(messageSource).getMessage("confirm.deleteCar", args, "confirm.deleteCar", Locale.ENGLISH);
    }
}
