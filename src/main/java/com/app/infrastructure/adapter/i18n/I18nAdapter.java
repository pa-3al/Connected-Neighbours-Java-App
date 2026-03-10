package com.app.infrastructure.adapter.i18n;
import com.app.domain.port.out.I18nPort;
import com.app.infrastructure.i18n.I18nService;
public class I18nAdapter implements I18nPort {
    private final I18nService i18nService = I18nService.getInstance();
    @Override
    public String get(String key, Object... args) {
        return i18nService.get(key, args);
    }
}
