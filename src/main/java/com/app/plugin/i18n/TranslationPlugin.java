package com.app.plugin.i18n;

import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;
import com.app.infrastructure.i18n.I18nService;

public class TranslationPlugin implements Plugin {

    @Override
    public String getId() {
        return "com.app.plugin.i18n";
    }

    @Override
    public String getName() {
        return I18nService.getInstance().get("plugin.translation.name");
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return I18nService.getInstance().get("plugin.translation.description");
    }

    @Override
    public void onLoad(PluginContext context) {
        context.logInfo("Loading Translation Plugin...");
        if (!context.isHeadless()) {
            context.addPanel(I18nService.getInstance().get("plugin.translation.tab"), new TranslationEditor());
        }
    }

    @Override
    public void onUnload(PluginContext context) {
        if (!context.isHeadless()) {
            context.removePanel(I18nService.getInstance().get("plugin.translation.tab"));
        }
    }
}
