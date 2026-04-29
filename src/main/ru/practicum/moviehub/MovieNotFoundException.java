package ru.practicum.moviehub;

public class MovieNotFoundException extends Exception {
    private final Integer id;

    public MovieNotFoundException(String message, Integer id) {
        super(message);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
