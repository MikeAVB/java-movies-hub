package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final int PORT = 8080;
    private static final Gson gson = new Gson();

    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, PORT);
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
        client.close();
    }

    /*
        GET
     */

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNonEmpty_returnsCorrectArray() throws Exception {
        Movie movie1 = store.addMovie(new Movie("Фильм 1", 1999));
        Movie movie2 = store.addMovie(new Movie("Фильм 2", 2000));
        Movie movie3 = store.addMovie(new Movie("Фильм 3", 2001));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(3, movies.size(), "Должно быть 3 фильма");
        assertEquals(movie1, movies.get(0));
        assertEquals(movie2, movies.get(1));
        assertEquals(movie3, movies.get(2));
    }

    /*
        POST
     */

    @Test
    void postMovie_whenIncorrectContentType_returnError() throws Exception {
        Movie normalMovie = new Movie("Кин-дза-дза!", 1986);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(normalMovie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "Неверный Content-Type должен вернуть 415");
    }

    @Test
    void postMovie_whenNormalMovie_returnOK() throws Exception {
        Movie normalMovie = new Movie("Кин-дза-дза!", 1986);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(normalMovie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        normalMovie.setId(1);
        Movie respondedMovie = gson.fromJson(resp.body(), Movie.class);
        assertEquals(normalMovie, respondedMovie, "Фильмы должны быть идентичны");
    }

    @Test
    void postMovie_whenIncorrectMovie_returnError() throws Exception {
        Movie movie = new Movie("", 2100);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);

        assertEquals("Ошибка валидации", errorResponse.getDescription(), "Должна быть ошибка валидации");
        assertEquals(2, errorResponse.getDetails().size());
    }

}