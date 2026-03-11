package com.app;
import com.app.infrastructure.ui.App;
public class Launcher {
    public static void main(String[] args) {
        if (args.length > 0) {
            com.app.infrastructure.di.ServiceContext context = new com.app.infrastructure.di.ServiceContext(true); 
            com.app.infrastructure.cli.CliHandler handler = new com.app.infrastructure.cli.CliHandler(context);
            handler.handle(args);
        } else {
            App.main(args);
        }
    }
}
