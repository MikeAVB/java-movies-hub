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
        UTILS===========================================================================================================
     */

    private HttpRequest getRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();
    }

    private HttpRequest postRequest(String path, Movie movie) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .setHeader("Content-type", BaseHttpHandler.CT_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();
    }

    private HttpRequest deleteRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();
    }

    private HttpResponse<String> sendRequest(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertContentTypeIsJson(HttpResponse<String> response) {
        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals(BaseHttpHandler.CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
    }

    /*
        GET=============================================================================================================
     */

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = getRequest("/movies");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertContentTypeIsJson(resp);

        String body = resp.body();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNonEmpty_returnsCorrectArray() throws Exception {
        Movie movie1 = store.addMovie(new Movie("Фильм 1", 1999));
        Movie movie2 = store.addMovie(new Movie("Фильм 2", 2000));
        Movie movie3 = store.addMovie(new Movie("Фильм 3", 2001));

        HttpRequest req = getRequest("/movies");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(200, resp.statusCode());
        assertContentTypeIsJson(resp);

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(3, movies.size(), "Должно быть 3 фильма");
        assertEquals(movie1, movies.get(0));
        assertEquals(movie2, movies.get(1));
        assertEquals(movie3, movies.get(2));
    }

    @Test
    void getMovie_whenCorrectID_returnOK() throws Exception {
        Movie movie = store.addMovie(new Movie("Фильм 1", 2000));

        HttpRequest req = getRequest("/movies/1");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(200, resp.statusCode());
        assertContentTypeIsJson(resp);

        Movie respondedMovie = gson.fromJson(resp.body(), Movie.class);
        assertEquals(movie, respondedMovie);
    }

    @Test
    void getMovie_whenIncorrectID_returnError() throws Exception {
        HttpRequest req = getRequest("/movies/100");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(404, resp.statusCode());
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("По данному ID фильмов не найдено", errorResponse.error());
        assertEquals(1, errorResponse.details().size());
        assertEquals("ID: 100", errorResponse.details().getFirst());
    }

    @Test
    void getMovie_whenIDIsNotInteger_returnError() throws Exception {
        HttpRequest req = getRequest("/movies/ABC");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(400, resp.statusCode());
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Неверный формат запроса", errorResponse.error());
        assertEquals(0, errorResponse.details().size());
    }

    /*
        POST============================================================================================================
     */

    @Test
    void postMovie_whenNormalMovie_returnOK() throws Exception {
        Movie normalMovie = new Movie("Кин-дза-дза!", 1986);

        HttpRequest req = postRequest("/movies", normalMovie);

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertContentTypeIsJson(resp);

        normalMovie.setId(1);
        Movie respondedMovie = gson.fromJson(resp.body(), Movie.class);
        assertEquals(normalMovie, respondedMovie, "Фильмы должны быть идентичны");
    }

    @Test
    void postMovie_whenTitleIsEmpty_returnError() throws Exception {
        Movie errorMovie = new Movie("", 2000);

        HttpRequest req = postRequest("/movies", errorMovie);

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(422, resp.statusCode(), "Должен вернуть 422, если название фильма пустое");
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.error());
        assertEquals(1, errorResponse.details().size());
        assertEquals("Название фильма не должно быть пустым", errorResponse.details().getFirst());
    }

    @Test
    void postMovie_whenTitleIsTooLong_returnError() throws Exception {
        Movie errorMovie = new Movie("ABC".repeat(50), 2000);

        HttpRequest req = postRequest("/movies", errorMovie);

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(422, resp.statusCode(), "Должен вернуть 422, если название фильма слишком длинное");
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.error());
        assertEquals(1, errorResponse.details().size());
        assertEquals("Длина названия фильма не должна превышать 100", errorResponse.details().getFirst());
    }

    @Test
    void postMovie_whenYearIsNotCorrect_returnError() throws Exception {
        Movie errorMovie = new Movie("Фильм", 1654);

        HttpRequest req = postRequest("/movies", errorMovie);

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(422, resp.statusCode(), "Должен вернуть 422, если год выпуска некорректный");
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.error());
        assertEquals(1, errorResponse.details().size());
        assertTrue(errorResponse.details().getFirst().startsWith("Год выпуска фильма должен быть в промежутке"));
    }

    @Test
    void postMovie_whenYearAndTitleIsNotCorrect_returnError() throws Exception {
        Movie errorMovie = new Movie("", 2122);

        HttpRequest req = postRequest("/movies", errorMovie);

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(422, resp.statusCode(), "Должен вернуть 422, если год и название некорректные");
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", errorResponse.error());
        assertEquals(2, errorResponse.details().size());
        assertEquals("Название фильма не должно быть пустым", errorResponse.details().getFirst());
        assertTrue(errorResponse.details().get(1).startsWith("Год выпуска фильма должен быть в промежутке"));
    }

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
    void postMovie_whenIncorrectJSON_returnError() throws Exception {
        String incorrectJson = "}INCORRECT JSON{";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .setHeader("Content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(incorrectJson))
                .build();

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(400, resp.statusCode(), "Должен вернуть 400, если передан некорректный JSON");
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный JSON-объект", errorResponse.error());
        assertEquals(0, errorResponse.details().size());
    }

    /*
        DELETE==========================================================================================================
     */

    @Test
    void deleteMovie_whenCorrectID_deleteMovie() throws Exception {
        Movie movie = new Movie("Фильм", 2000);
        store.addMovie(movie);

        HttpRequest req = deleteRequest("/movies/1");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(204, resp.statusCode());
        assertContentTypeIsJson(resp);

        assertTrue(store.getById(1).isEmpty());
    }

    @Test
    void deleteMovie_whenIncorrectID_returnError() throws Exception {
        HttpRequest req = deleteRequest("/movies/100");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(404, resp.statusCode());
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Не удалось удалить фильм, ID не найден", errorResponse.error());
        assertEquals(1, errorResponse.details().size());
        assertEquals("ID: 100", errorResponse.details().getFirst());
    }

    @Test
    void deleteMovie_whenIdIsNotInteger_returnError() throws Exception {
        HttpRequest req = deleteRequest("/movies/ABC");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(400, resp.statusCode());
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Неверный формат запроса", errorResponse.error());
        assertEquals(0, errorResponse.details().size());
    }

    /*
        GET /movies?year=YYYY===========================================================================================
     */

    @Test
    void getMovieByYear_whenYearCorrect_returnOK() throws Exception {
        Movie movie1 = store.addMovie(new Movie("Фильм 1", 2000));
        Movie movie2 = store.addMovie(new Movie("Фильм 2", 2004));
        Movie movie3 = store.addMovie(new Movie("Фильм 3", 2000));

        HttpRequest req = getRequest("/movies?year=2000");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertContentTypeIsJson(resp);

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(2, movies.size(), "Должно быть 2 фильма");
        assertEquals(movie1, movies.get(0));
        assertEquals(movie3, movies.get(1));
        assertFalse(movies.contains(movie2));
    }

    @Test
    void getMovieByYear_whenYearIncorrect_returnEmptyList() throws Exception {
        store.addMovie(new Movie("Фильм 1", 2000));
        store.addMovie(new Movie("Фильм 2", 2004));
        store.addMovie(new Movie("Фильм 3", 2000));

        HttpRequest req = getRequest("/movies?year=2006");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertContentTypeIsJson(resp);

        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(0, movies.size(), "Список должен быть пуст");
    }

    @Test
    void getMovieByYear_whenYearIsNotInteger_returnError() throws Exception {
        HttpRequest req = getRequest("/movies?year=ABC");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(400, resp.statusCode(), "Должен вернуть 400");
        assertContentTypeIsJson(resp);

        ErrorResponse errorResponse = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Неверный формат запроса", errorResponse.error());
        assertEquals(0, errorResponse.details().size());
    }

    /*
        NOT ALLOWED METHODS=============================================================================================
     */

    @Test
    void putMethod_returnNotAllowed() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .PUT(HttpRequest.BodyPublishers.ofString("body"))
                .build();

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(405, resp.statusCode(), "Должен вернуть 405 - метод недоступен");
    }

    @Test
    void deleteAllMethod_returnNotAllowed() throws Exception {
        HttpRequest req = deleteRequest("/movies");

        HttpResponse<String> resp = sendRequest(req);

        assertEquals(405, resp.statusCode(), "Должен вернуть 405 - метод недоступен");
    }
}