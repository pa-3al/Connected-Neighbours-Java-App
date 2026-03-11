package com.app.infrastructure.adapter.i18n;

import com.app.domain.port.out.I18nPort;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18nAdapter implements I18nPort {
    private static final String BASE_NAME = "com.app.i18n.messages";

    @Override
    public String get(String key, Object... args) {
        String pattern = key;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, Locale.getDefault());
            if (bundle.containsKey(key)) {
                pattern = bundle.getString(key);
            }
        } catch (MissingResourceException ignored) {
        }

        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }
}
