package ru.practicum.moviehub.store;

import ru.practicum.moviehub.MovieNotFoundException;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.validation.MovieTitleValidator;
import ru.practicum.moviehub.MovieValidationException;
import ru.practicum.moviehub.store.validation.MovieValidator;
import ru.practicum.moviehub.store.validation.MovieYearValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> store;
    private final List<MovieValidator> validators;
    private Integer currentId;

    public MoviesStore() {
        this.store = new HashMap<>();
        this.currentId = 0;

        this.validators = new ArrayList<>();
        validators.add(new MovieTitleValidator());
        validators.add(new MovieYearValidator());
    }

    public Integer addMovie(Movie movie) throws MovieValidationException {
        List<String> validationResults = validateMovie(movie);
        if (!validationResults.isEmpty()) {
            throw new MovieValidationException("Ошибка валидации", validationResults);
        }

        Integer nextId = currentId + 1;
        store.put(nextId, movie);
        currentId = nextId;
        return nextId;
    }

    public Movie getById(Integer id) throws MovieNotFoundException {
        if (!store.containsKey(id)) {
            throw new MovieNotFoundException("Фильм не найден", id);
        }

        return store.get(id);
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(store.values());
    }

    public void removeById(Integer id) throws MovieNotFoundException {
        if (!store.containsKey(id)) {
            throw new MovieNotFoundException("Фильм не найден", id);
        }

        store.remove(id);
    }

    private List<String> validateMovie(Movie movie) {
        List<String> validationResults = new ArrayList<>();
        validators.forEach(movieValidator -> movieValidator.validate(movie, validationResults));
        return validationResults;
    }
}