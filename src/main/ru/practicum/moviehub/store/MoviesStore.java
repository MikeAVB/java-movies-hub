package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> store;
    private Integer currentId;

    public MoviesStore() {
        this.store = new HashMap<>();
        this.currentId = 0;
    }

    public Movie addMovie(Movie movie) {
        Movie newMovie = new Movie(movie.getTitle(), movie.getYear(), ++currentId);
        store.put(currentId, newMovie);
        return newMovie;
    }

    public Optional<Movie> getById(Integer id) {
        if (store.containsKey(id)) {
            return Optional.of(store.get(id));
        } else {
            return Optional.empty();
        }
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(store.values());
    }

    public List<Movie> filterByYear(int year) {
        return store.values().stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public boolean removeById(Integer id) {
        if (store.containsKey(id)) {
            store.remove(id);
            return true;
        } else {
            return false;
        }
    }

    public void clear() {
        store.clear();
        this.currentId = 0;
    }
}