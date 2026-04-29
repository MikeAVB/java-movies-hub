package ru.practicum.moviehub.store.validation;

import ru.practicum.moviehub.model.Movie;

import java.util.List;

public interface MovieValidator {
    void validate(Movie movie, List<String> results);
}
