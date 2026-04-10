package server.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private final ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public record Connection(String username, Session session, int gameID) {
    }

    public void add(String username, Session session, int gameID) {
        connections.put(username, new Connection(username, session, gameID));
    }

    public void remove(String username) {
        connections.remove(username);
    }

    public Connection get(String username) {
        return connections.get(username);
    }

    public void broadcast(int gameID, String excludeUsername, ServerMessage message) throws IOException {
        String json = gson.toJson(message);
        for (Connection conn : connections.values()) {
            if (conn.gameID() == gameID && !conn.username().equals(excludeUsername)) {
                if (conn.session().isOpen()) {
                    conn.session().getRemote().sendString(json);
                }
            }
        }
    }

    public void sendTo(String username, ServerMessage message) throws IOException {
        String json = gson.toJson(message);
        Connection conn = connections.get(username);
        if (conn != null && conn.session().isOpen()) {
            conn.session().getRemote().sendString(json);
        }
    }
}
