package client;

import chess.ChessPiece;
import chess.ChessPieceDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import websocket.commands.UserGameCommand;

import jakarta.websocket.*;
import java.io.IOException;
import java.net.URI;

public class WebSocketCommunicator extends Endpoint {
    private Session session;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChessPiece.class, new ChessPieceDeserializer())
            .create();

    public void connect(String serverUrl, GameHandler handler) throws ResponseException {
        try {
            String wsUrl = serverUrl.replace("http", "ws") + "/ws";
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(this, new URI(wsUrl));

            this.session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    JsonObject json = gson.fromJson(message, JsonObject.class);
                    String type = json.get("serverMessageType").getAsString();
                    switch (type) {
                        case "LOAD_GAME" -> {
                            var game = gson.fromJson(json.get("game"), chess.ChessGame.class);
                            handler.onLoadGame(game);
                        }
                        case "NOTIFICATION" -> handler.onNotification(json.get("message").getAsString());
                        case "ERROR" -> handler.onError(json.get("errorMessage").getAsString());
                    }
                }
            });
        } catch (Exception e) {
            throw new ResponseException(500, "WebSocket connection failed: " + e.getMessage());
        }
    }

    public void send(UserGameCommand command) throws ResponseException {
        try {
            session.getBasicRemote().sendText(gson.toJson(command));
        } catch (IOException e) {
            throw new ResponseException(500, "Failed to send message: " + e.getMessage());
        }
    }

    public void close() throws ResponseException {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            throw new ResponseException(500, "Failed to close WebSocket: " + e.getMessage());
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
    }
}
