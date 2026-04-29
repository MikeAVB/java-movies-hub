package ru.practicum.moviehub;

public class MovieNotFoundException extends Exception {
    private Integer id;
    public MovieNotFoundException(String message, Integer id) {
        super(message);
        this.id = id;
    }
}
