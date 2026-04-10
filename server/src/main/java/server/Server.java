package server;

import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.*;
import io.javalin.http.Context;
import model.AuthData;
import model.GameData;
import model.UserData;
import server.request.*;
import server.result.*;
import server.websocket.WebSocketHandler;
import service.*;

import java.util.Collection;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();

    private final UserDAO userDAO = new MySqlUserDAO();
    private final AuthDAO authDAO = new MySqlAuthDAO();
    private final GameDAO gameDAO = new MySqlGameDAO();

    private final UserService userService = new UserService(userDAO, authDAO);
    private final GameService gameService = new GameService(gameDAO, authDAO);
    private final ClearService clearService = new ClearService(userDAO, authDAO, gameDAO);
    private final WebSocketHandler webSocketHandler = new WebSocketHandler(authDAO, gameDAO);

    public Server() {
        try {
            DatabaseManager.configureDatabase();
        } catch (DataAccessException ex) {
            throw new RuntimeException("failed to initialize database", ex);
        }
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.ws("/ws", ws -> {
            ws.onMessage(webSocketHandler::onMessage);
        });

        javalin.delete("/db", this::handleClear);
        javalin.post("/user", this::handleRegister);
        javalin.post("/session", this::handleLogin);
        javalin.delete("/session", this::handleLogout);
        javalin.get("/game", this::handleListGames);
        javalin.post("/game", this::handleCreateGame);
        javalin.put("/game", this::handleJoinGame);
    }

    private void sendJson(Context ctx, int status, Object obj) {
        ctx.status(status).contentType("application/json").result(gson.toJson(obj));
    }

    private void handleClear(Context ctx) {
        try {
            clearService.clear();
            sendJson(ctx, 200, new Object());
        } catch (ServiceException e) {
            sendJson(ctx, e.getStatusCode(), new ErrorResult(e.getMessage()));
        }
    }

    private void handleRegister(Context ctx) {
        try {
            RegisterRequest req = gson.fromJson(ctx.body(), RegisterRequest.class);
            UserData user = new UserData(req.username(), req.password(), req.email());
            AuthData auth = userService.register(user);
            sendJson(ctx, 200, new AuthResult(auth.username(), auth.authToken()));
        } catch (ServiceException e) {
            sendJson(ctx, e.getStatusCode(), new ErrorResult(e.getMessage()));
        }
    }

    private void handleLogin(Context ctx) {
        try {
            LoginRequest req = gson.fromJson(ctx.body(), LoginRequest.class);
            AuthData auth = userService.login(req.username(), req.password());
            sendJson(ctx, 200, new AuthResult(auth.username(), auth.authToken()));
        } catch (ServiceException e) {
            sendJson(ctx, e.getStatusCode(), new ErrorResult(e.getMessage()));
        }
    }

    private void handleLogout(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            userService.logout(authToken);
            sendJson(ctx, 200, new Object());
        } catch (ServiceException e) {
            sendJson(ctx, e.getStatusCode(), new ErrorResult(e.getMessage()));
        }
    }

    private void handleListGames(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            Collection<GameData> games = gameService.listGames(authToken);
            sendJson(ctx, 200, new ListGamesResult(games));
        } catch (ServiceException e) {
            sendJson(ctx, e.getStatusCode(), new ErrorResult(e.getMessage()));
        }
    }

    private void handleCreateGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            CreateGameRequest req = gson.fromJson(ctx.body(), CreateGameRequest.class);
            int gameID = gameService.createGame(authToken, req.gameName());
            sendJson(ctx, 200, new CreateGameResult(gameID));
        } catch (ServiceException e) {
            sendJson(ctx, e.getStatusCode(), new ErrorResult(e.getMessage()));
        }
    }

    private void handleJoinGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            JoinGameRequest req = gson.fromJson(ctx.body(), JoinGameRequest.class);
            gameService.joinGame(authToken, req.playerColor(), req.gameID());
            sendJson(ctx, 200, new Object());
        } catch (ServiceException e) {
            sendJson(ctx, e.getStatusCode(), new ErrorResult(e.getMessage()));
        }
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
