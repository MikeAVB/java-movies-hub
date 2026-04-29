package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.MovieValidationException;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            handleGetAllMovies(ex);
        } else if (method.equalsIgnoreCase("POST")) {
            handlePostNewMovie(ex);
        }
    }

    private void handlePostNewMovie(HttpExchange ex) throws IOException {
        if (!CT_JSON.equals(ex.getRequestHeaders().getFirst("Content-type"))) {
            sendJson(ex, 415, gson.toJson(new ErrorResponse("Неверный тип данных", Collections.emptyList())));
            return;
        }

        try (InputStream is = ex.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            Movie newMovie = gson.fromJson(body, Movie.class);
            newMovie = store.addMovie(newMovie);

            sendJson(ex, 201, gson.toJson(newMovie));
        } catch (JsonSyntaxException exception) {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный JSON-объект", Collections.emptyList());
            sendJson(ex, 400, gson.toJson(errorResponse));
        } catch (MovieValidationException exception) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", exception.getResults());
            sendJson(ex, 422, gson.toJson(errorResponse));
        }
    }

    private void handleGetAllMovies(HttpExchange ex) throws IOException {
        sendJson(ex, 200, gson.toJson(store.getAllMovies()));
    }
}
