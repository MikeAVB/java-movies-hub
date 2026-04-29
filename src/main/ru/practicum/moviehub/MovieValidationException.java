package ru.practicum.moviehub;

import java.util.List;

public class MovieValidationException extends Exception {
    private List<String> results;

    public MovieValidationException(String message, List<String> validationResults) {
        super(message);
        this.results = validationResults;
    }

    public List<String> getResults() {
        return results;
    }
}
