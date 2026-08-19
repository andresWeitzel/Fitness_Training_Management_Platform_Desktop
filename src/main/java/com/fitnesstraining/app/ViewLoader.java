package com.fitnesstraining.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.function.Function;

public class ViewLoader {

    private final Function<Class<?>, Object> controllerFactory;

    public ViewLoader(Function<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    public <T> LoadedView<T> load(String resource) {
        URL url = ViewLoader.class.getResource(resource);
        if (url == null) {
            throw new IllegalStateException("No se encontró la vista " + resource);
        }
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(controllerFactory::apply);
        try {
            Parent root = loader.load();
            return new LoadedView<>(root, loader.getController());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar " + resource, ex);
        }
    }

    public record LoadedView<T>(Parent root, T controller) {
    }
}
