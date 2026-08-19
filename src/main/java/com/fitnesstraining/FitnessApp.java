package com.fitnesstraining;

import com.fitnesstraining.app.AppContext;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitnessApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(FitnessApp.class);

    private AppContext appContext;

    @Override
    public void start(Stage stage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> log.error("Error no controlado", ex));
        appContext = new AppContext(stage);
        appContext.start();
    }

    @Override
    public void stop() {
        if (appContext != null) {
            appContext.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
