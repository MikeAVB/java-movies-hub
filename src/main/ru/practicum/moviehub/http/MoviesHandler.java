package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.http.validation.MovieTitleValidator;
import ru.practicum.moviehub.http.validation.MovieValidator;
import ru.practicum.moviehub.http.validation.MovieYearValidator;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final List<MovieValidator> validators;

    public MoviesHandler(MoviesStore store) {
        this.store = store;

        this.validators = new ArrayList<>();
        validators.add(new MovieTitleValidator());
        validators.add(new MovieYearValidator());
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        if (path.matches("/movies$")) {
            if (method.equalsIgnoreCase("GET")) {
                if (query == null) {
                    handleGetAllMovies(ex);
                } else if (query.matches("year=\\d{4}$")){
                    handleGetMoviesByYear(ex, Integer.parseInt(query.split("=")[1]));
                } else {
                    handleGetIncorrectRequest(ex);
                }
            } else if (method.equalsIgnoreCase("POST")) {
                handlePostNewMovie(ex);
            } else {
                sendNotAllowed(ex);
            }
        } else if (path.matches("/movies/\\d+$")) {
            int id = Integer.parseInt(path.split("/")[2]);
            if (method.equalsIgnoreCase("GET")) {
                handleGetMovieById(ex, id);
            } else if (method.equalsIgnoreCase("DELETE")) {
                handleDeleteMovieById(ex, id);
            } else {
                sendNotAllowed(ex);
            }
        } else {
            handleGetIncorrectRequest(ex);
        }
    }

    private void handleGetMoviesByYear(HttpExchange ex, int year) throws IOException {
        sendJson(ex, 200, gson.toJson(store.filterByYear(year)));
    }

    private void handleGetAllMovies(HttpExchange ex) throws IOException {
        sendJson(ex, 200, gson.toJson(store.getAllMovies()));
    }

    private void handleGetMovieById(HttpExchange ex, int id) throws IOException {
        Optional<Movie> movie = store.getById(id);
        if (movie.isPresent()) {
            sendJson(ex, 200, gson.toJson(movie.get()));
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse(
                    "По данному ID фильмов не найдено", List.of(String.format("ID: %d", id))
            )));
        }
    }

    private void handleDeleteMovieById(HttpExchange ex, int id) throws IOException {
        boolean isRemoved = store.removeById(id);
        if (isRemoved) {
            sendNoContent(ex);
        } else {
            sendJson(ex, 404, gson.toJson(new ErrorResponse(
                    "Не удалось удалить фильм, ID не найден", List.of(String.format("ID: %d", id))
            )));
        }
    }

    private void handleGetIncorrectRequest(HttpExchange ex) throws IOException {
        sendJson(ex, 400, gson.toJson(new ErrorResponse(
                "Неверный формат запроса", Collections.emptyList())));
    }

    private void handlePostNewMovie(HttpExchange ex) throws IOException {
        if (!CT_JSON.equals(ex.getRequestHeaders().getFirst("Content-type"))) {
            sendJson(ex, 415, gson.toJson(new ErrorResponse(
                    "Неверный тип данных", Collections.emptyList())));
            return;
        }

        try (InputStream is = ex.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Movie newMovie = gson.fromJson(body, Movie.class);
            List<String> validateResult = validateMovie(newMovie);
            if (validateResult.isEmpty()) {
                newMovie = store.addMovie(newMovie);
                sendJson(ex, 201, gson.toJson(newMovie));
            } else {
               sendJson(ex, 422, gson.toJson(new ErrorResponse(
                       "Ошибка валидации", validateResult
               )));
            }
        } catch (JsonSyntaxException exception) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse(
                    "Некорректный JSON-объект", Collections.emptyList()
            )));
        }
    }

    private List<String> validateMovie(Movie movie) {
        List<String> validationResults = new ArrayList<>();
        validators.forEach(movieValidator -> movieValidator.validate(movie, validationResults));
        return validationResults;
    }
}
