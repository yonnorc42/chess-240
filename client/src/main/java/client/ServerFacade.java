package client;

import chess.ChessPiece;
import chess.ChessPieceDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChessPiece.class, new ChessPieceDeserializer())
            .create();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public ServerFacade(int port) {
        this("http://localhost:" + port);
    }

    private <T> T makeRequest(String method, String path, Object request, String authToken, Class<T> responseClass)
            throws ResponseException {
        try {
            URL url = new URI(serverUrl + path).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setRequestProperty("Content-Type", "application/json");

            if (authToken != null) {
                http.setRequestProperty("authorization", authToken);
            }

            if (request != null) {
                http.setDoOutput(true);
                try (OutputStream os = http.getOutputStream()) {
                    os.write(gson.toJson(request).getBytes());
                }
            }

            int status = http.getResponseCode();
            if (status >= 400) {
                try (InputStream errStream = http.getErrorStream()) {
                    if (errStream != null) {
                        String body = new String(errStream.readAllBytes());
                        ErrorMessage err = gson.fromJson(body, ErrorMessage.class);
                        throw new ResponseException(status, err != null && err.message() != null
                                ? err.message() : "Request failed with status " + status);
                    }
                }
                throw new ResponseException(status, "Request failed with status " + status);
            }

            if (responseClass == null) {
                return null;
            }
            try (InputStream in = http.getInputStream()) {
                String body = new String(in.readAllBytes());
                return gson.fromJson(body, responseClass);
            }

        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseException(500, "Connection error: " + e.getMessage());
        }
    }

    private record ErrorMessage(String message) {
    }
}
