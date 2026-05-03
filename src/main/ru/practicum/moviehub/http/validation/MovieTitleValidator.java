package ru.practicum.moviehub.http.validation;

import ru.practicum.moviehub.model.Movie;

import java.util.List;
import java.util.Objects;

public class MovieTitleValidator implements MovieValidator {
    private static final int MAX_TITLE_LENGTH = 100;

    @Override
    public void validate(Movie movie, List<String> results) {
        Objects.requireNonNull(results);
        if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            results.add(String.format("Длина названия фильма не должна превышать %d", MAX_TITLE_LENGTH)
            );
        }

        if (movie.getTitle().isBlank()) {
            results.add("Название фильма не должно быть пустым");
        }
    }
}
