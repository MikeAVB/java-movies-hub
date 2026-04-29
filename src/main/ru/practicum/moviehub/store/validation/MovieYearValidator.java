package ru.practicum.moviehub.store.validation;

import ru.practicum.moviehub.model.Movie;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class MovieYearValidator implements MovieValidator {
    public static final int MIN_RELEASE_YEAR = 1888;
    public static final int MAX_RELEASE_YEAR = LocalDate.now().plusYears(1).getYear();

    @Override
    public void validate(Movie movie, List<String> results) {
        if (movie.getYear() < MIN_RELEASE_YEAR || movie.getYear() > MAX_RELEASE_YEAR) {
            Objects.requireNonNull(results).add(
                    String.format("Год выпуская фильма должен быть в промежутке [%d - %d]",
                            MIN_RELEASE_YEAR, MAX_RELEASE_YEAR)
            );
        }
    }
}
