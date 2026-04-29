package ru.practicum.moviehub.model;

import java.time.LocalDate;

public record Movie(String title, Integer year) {
    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MIN_RELEASE_YEAR = 1888;
    public static final int MAX_RELEASE_YEAR = LocalDate.now().plusYears(1).getYear();

    public Movie {
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Длина названия фильма не должна превышать 100 символов");
        }
        if (year < MIN_RELEASE_YEAR || year > MAX_RELEASE_YEAR) {
            throw new IllegalArgumentException(
                    String.format("Год выпуска фильма должен быть в промежутке [%d : %d]",
                            MIN_RELEASE_YEAR, MAX_RELEASE_YEAR)
            );
        }
    }
}