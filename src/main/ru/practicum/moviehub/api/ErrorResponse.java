package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private String description;
    private List<String> details;

    public ErrorResponse(String description, List<String> details) {
        this.description = description;
        this.details = details;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getDetails() {
        return details;
    }
}