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
import service.*;

import java.util.Collection;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();

    private final UserDAO userDAO = new MemoryUserDAO();
    private final AuthDAO authDAO = new MemoryAuthDAO();
    private final GameDAO gameDAO = new MemoryGameDAO();

    private final UserService userService = new UserService(userDAO, authDAO);
    private final GameService gameService = new GameService(gameDAO, authDAO);
    private final ClearService clearService = new ClearService(userDAO, authDAO, gameDAO);

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        javalin.delete("/db", this::handleClear);
        javalin.post("/user", this::handleRegister);
        javalin.post("/session", this::handleLogin);
        javalin.delete("/session", this::handleLogout);
        javalin.get("/game", this::handleListGames);
        javalin.post("/game", this::handleCreateGame);
        javalin.put("/game", this::handleJoinGame);
    }

    private void handleClear(Context ctx) {
        try {
            clearService.clear();
            ctx.status(200).json(new Object());
        } catch (ServiceException e) {
            ctx.status(e.getStatusCode()).json(new ErrorResult(e.getMessage()));
        }
    }

    private void handleRegister(Context ctx) {
        try {
            RegisterRequest req = gson.fromJson(ctx.body(), RegisterRequest.class);
            UserData user = new UserData(req.username(), req.password(), req.email());
            AuthData auth = userService.register(user);
            ctx.status(200).json(new AuthResult(auth.username(), auth.authToken()));
        } catch (ServiceException e) {
            ctx.status(e.getStatusCode()).json(new ErrorResult(e.getMessage()));
        }
    }

    private void handleLogin(Context ctx) {
        try {
            LoginRequest req = gson.fromJson(ctx.body(), LoginRequest.class);
            AuthData auth = userService.login(req.username(), req.password());
            ctx.status(200).json(new AuthResult(auth.username(), auth.authToken()));
        } catch (ServiceException e) {
            ctx.status(e.getStatusCode()).json(new ErrorResult(e.getMessage()));
        }
    }

    private void handleLogout(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            userService.logout(authToken);
            ctx.status(200).json(new Object());
        } catch (ServiceException e) {
            ctx.status(e.getStatusCode()).json(new ErrorResult(e.getMessage()));
        }
    }

    private void handleListGames(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            Collection<GameData> games = gameService.listGames(authToken);
            ctx.status(200).json(new ListGamesResult(games));
        } catch (ServiceException e) {
            ctx.status(e.getStatusCode()).json(new ErrorResult(e.getMessage()));
        }
    }

    private void handleCreateGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            CreateGameRequest req = gson.fromJson(ctx.body(), CreateGameRequest.class);
            int gameID = gameService.createGame(authToken, req.gameName());
            ctx.status(200).json(new CreateGameResult(gameID));
        } catch (ServiceException e) {
            ctx.status(e.getStatusCode()).json(new ErrorResult(e.getMessage()));
        }
    }

    private void handleJoinGame(Context ctx) {
        try {
            String authToken = ctx.header("authorization");
            JoinGameRequest req = gson.fromJson(ctx.body(), JoinGameRequest.class);
            gameService.joinGame(authToken, req.playerColor(), req.gameID());
            ctx.status(200).json(new Object());
        } catch (ServiceException e) {
            ctx.status(e.getStatusCode()).json(new ErrorResult(e.getMessage()));
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
