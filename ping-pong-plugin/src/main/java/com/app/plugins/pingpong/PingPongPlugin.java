package com.app.plugins.pingpong;

import com.app.plugin.Plugin;
import com.app.plugin.PluginContext;

public class PingPongPlugin implements Plugin {

    private PongGamePane gamePane;

    @Override
    public String getId() {
        return "ping-pong";
    }

    @Override
    public String getName() {
        return "Ping Pong";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Ping Pong 2 joueurs. J1 : Z/S | J2 : P/M";
    }

    @Override
    public String getAuthor() {
        return "Connected-Neighbours";
    }

    @Override
    public void onLoad(PluginContext context) {
        gamePane = new PongGamePane();
        context.addPanel("🏓 Ping Pong", gamePane);
    }

    @Override
    public void onUnload(PluginContext context) {
        if (gamePane != null) {
            gamePane.stopGame();
        }
        context.removePanel("🏓 Ping Pong");
    }
}
