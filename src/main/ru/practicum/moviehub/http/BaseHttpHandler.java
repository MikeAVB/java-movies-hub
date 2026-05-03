package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json"; // !!! Укажите содержимое заголовка Content-Type
    protected static final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        try (OutputStream os = ex.getResponseBody()) {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", CT_JSON);
            ex.sendResponseHeaders(status, body.length);
            os.write(body);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
    }

    protected void sendNotAllowed(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(405, -1);
    }
}