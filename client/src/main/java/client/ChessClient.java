package client;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import ui.BoardRenderer;

public class ChessClient {
    private final ServerFacade server;
    private String authToken = null;
    private GameData[] games = null;

    public ChessClient(String serverUrl) {
        server = new ServerFacade(serverUrl);
    }

    public boolean isLoggedIn() {
        return authToken != null;
    }

    public String eval(String input) {
        String[] tokens = input.strip().split("\\s+");
        String cmd = tokens[0].toLowerCase();
        try {
            if (isLoggedIn()) {
                return evalPostlogin(cmd, tokens);
            } else {
                return evalPrelogin(cmd, tokens);
            }
        } catch (ResponseException e) {
            return e.getMessage();
        }
    }

    private String evalPrelogin(String cmd, String[] params) throws ResponseException {
        return switch (cmd) {
            case "help" -> helpPrelogin();
            case "quit" -> "quit";
            case "register" -> register(params);
            case "login" -> login(params);
            default -> "Unknown command. Type 'help' for available commands.";
        };
    }

    private String register(String[] params) throws ResponseException {
        if (params.length != 4) {
            return "Usage: register <USERNAME> <PASSWORD> <EMAIL>";
        }
        AuthData auth = server.register(params[1], params[2], params[3]);
        authToken = auth.authToken();
        return "Registered and logged in as " + auth.username() + ".";
    }

    private String login(String[] params) throws ResponseException {
        if (params.length != 3) {
            return "Usage: login <USERNAME> <PASSWORD>";
        }
        AuthData auth = server.login(params[1], params[2]);
        authToken = auth.authToken();
        return "Logged in as " + auth.username() + ".";
    }

    private String evalPostlogin(String cmd, String[] params) throws ResponseException {
        return switch (cmd) {
            case "help" -> helpPostlogin();
            case "quit" -> "quit";
            case "logout" -> logout();
            case "create" -> createGame(params);
            case "list" -> listGames();
            case "join" -> joinGame(params);
            case "observe" -> observeGame(params);
            default -> "Unknown command. Type 'help' for available commands.";
        };
    }

    private String observeGame(String[] params) throws ResponseException {
        if (params.length != 2) {
            return "Usage: observe <ID>";
        }
        if (games == null) {
            return "Please run 'list' first to see available games.";
        }
        int index;
        try {
            index = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            return "Invalid game number. Use the number from 'list'.";
        }
        if (index < 1 || index > games.length) {
            return "Invalid game number. Use a number between 1 and " + games.length + ".";
        }
        GameData game = games[index - 1];
        return "Observing game \"" + game.gameName() + "\".\n"
                + BoardRenderer.render(game.game().getBoard(), ChessGame.TeamColor.WHITE);
    }

    private String joinGame(String[] params) throws ResponseException {
        if (params.length != 3) {
            return "Usage: join <ID> [WHITE|BLACK]";
        }
        if (games == null) {
            return "Please run 'list' first to see available games.";
        }
        int index;
        try {
            index = Integer.parseInt(params[1]);
        } catch (NumberFormatException e) {
            return "Invalid game number. Use the number from 'list'.";
        }
        if (index < 1 || index > games.length) {
            return "Invalid game number. Use a number between 1 and " + games.length + ".";
        }
        String color = params[2].toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            return "Color must be WHITE or BLACK.";
        }
        GameData game = games[index - 1];
        server.joinGame(authToken, color, game.gameID());
        ChessGame.TeamColor perspective = ChessGame.TeamColor.valueOf(color);
        return "Joined game \"" + game.gameName() + "\" as " + color + ".\n"
                + BoardRenderer.render(game.game().getBoard(), perspective);
    }

    private String listGames() throws ResponseException {
        games = server.listGames(authToken);
        if (games.length == 0) {
            return "No games found.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < games.length; i++) {
            GameData g = games[i];
            sb.append(String.format("  %d. %s -- White: %s | Black: %s%n",
                    i + 1,
                    g.gameName(),
                    g.whiteUsername() != null ? g.whiteUsername() : "(empty)",
                    g.blackUsername() != null ? g.blackUsername() : "(empty)"));
        }
        return sb.toString().stripTrailing();
    }

    private String createGame(String[] params) throws ResponseException {
        if (params.length != 2) {
            return "Usage: create <NAME>";
        }
        server.createGame(authToken, params[1]);
        return "Created game \"" + params[1] + "\".";
    }

    private String logout() throws ResponseException {
        server.logout(authToken);
        authToken = null;
        games = null;
        return "Logged out.";
    }

    private String helpPrelogin() {
        return """
                register <USERNAME> <PASSWORD> <EMAIL> - to create an account
                login <USERNAME> <PASSWORD> - to play chess
                quit - playing chess
                help - with possible commands""";
    }

    private String helpPostlogin() {
        return """
                create <NAME> - a game
                list - games
                join <ID> [WHITE|BLACK] - a game
                observe <ID> - a game
                logout - when you are done
                quit - playing chess
                help - with possible commands""";
    }
}
