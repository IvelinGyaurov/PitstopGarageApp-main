package com.pitstop.garage.config;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageHelper {

    private final MessageSource messageSource;

    public MessageHelper(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String code) {
        return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }

    public String get(String code, Object... args) {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
    }
}
