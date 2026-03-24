package client;

import model.GameData;

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
            default -> "Unknown command. Type 'help' for available commands.";
        };
    }

    private String evalPostlogin(String cmd, String[] params) throws ResponseException {
        return switch (cmd) {
            case "help" -> helpPostlogin();
            case "quit" -> "quit";
            default -> "Unknown command. Type 'help' for available commands.";
        };
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
