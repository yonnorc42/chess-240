package client;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import model.AuthData;
import model.GameData;
import ui.BoardRenderer;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;

public class ChessClient implements GameHandler {
    private final String serverUrl;
    private final ServerFacade server;
    private String authToken = null;
    private GameData[] games = null;

    private WebSocketCommunicator ws = null;
    private Integer currentGameID = null;
    private ChessGame.TeamColor currentColor = null;
    private ChessGame currentGame = null;
    private boolean pendingResign = false;

    public ChessClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.server = new ServerFacade(serverUrl);
    }

    public boolean isLoggedIn() {
        return authToken != null;
    }

    public boolean isInGame() {
        return currentGameID != null;
    }

    public String eval(String input) {
        String[] tokens = input.strip().split("\\s+");
        String cmd = tokens[0].toLowerCase();
        try {
            if (pendingResign) {
                return handleResignConfirmation(cmd);
            }
            if (isInGame()) {
                return evalGameplay(cmd, tokens);
            } else if (isLoggedIn()) {
                return evalPostlogin(cmd, tokens);
            } else {
                return evalPrelogin(cmd, tokens);
            }
        } catch (ResponseException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("Error: ")) {
                msg = msg.substring(7);
            }
            return "Error: " + msg;
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

    private String evalGameplay(String cmd, String[] params) throws ResponseException {
        return switch (cmd) {
            case "help" -> helpGameplay();
            case "redraw" -> redraw();
            case "leave" -> leave();
            case "move" -> makeMove(params);
            case "resign" -> resign();
            case "highlight" -> highlight(params);
            default -> "Unknown command. Type 'help' for available commands.";
        };
    }

    private String redraw() {
        if (currentGame == null) {
            return "No game loaded yet.";
        }
        ChessGame.TeamColor perspective = (currentColor != null) ? currentColor : ChessGame.TeamColor.WHITE;
        return BoardRenderer.render(currentGame.getBoard(), perspective);
    }

    private String leave() throws ResponseException {
        ws.send(new UserGameCommand(UserGameCommand.CommandType.LEAVE, authToken, currentGameID));
        ws.close();
        ws = null;
        currentGameID = null;
        currentColor = null;
        currentGame = null;
        return "Left the game.";
    }

    private String makeMove(String[] params) throws ResponseException {
        if (params.length != 3 && params.length != 4) {
            return "Usage: move <FROM> <TO> [PROMOTION]";
        }
        if (currentColor == null) {
            return "Observers cannot make moves.";
        }
        ChessPosition start = parsePosition(params[1]);
        ChessPosition end = parsePosition(params[2]);
        if (start == null || end == null) {
            return "Invalid position. Use format like 'e2'.";
        }
        ChessPiece.PieceType promotion = null;
        if (params.length == 4) {
            promotion = parsePromotion(params[3]);
            if (promotion == null) {
                return "Invalid promotion piece. Use q, r, b, or n.";
            }
        }
        ChessMove move = new ChessMove(start, end, promotion);
        ws.send(new MakeMoveCommand(authToken, currentGameID, move));
        return "";
    }

    private ChessPosition parsePosition(String s) {
        if (s == null || s.length() != 2) {
            return null;
        }
        char colChar = Character.toLowerCase(s.charAt(0));
        char rowChar = s.charAt(1);
        if (colChar < 'a' || colChar > 'h' || rowChar < '1' || rowChar > '8') {
            return null;
        }
        int col = colChar - 'a' + 1;
        int row = rowChar - '0';
        return new ChessPosition(row, col);
    }

    private ChessPiece.PieceType parsePromotion(String s) {
        return switch (s.toLowerCase()) {
            case "q" -> ChessPiece.PieceType.QUEEN;
            case "r" -> ChessPiece.PieceType.ROOK;
            case "b" -> ChessPiece.PieceType.BISHOP;
            case "n" -> ChessPiece.PieceType.KNIGHT;
            default -> null;
        };
    }

    private String resign() {
        if (currentColor == null) {
            return "Observers cannot resign.";
        }
        pendingResign = true;
        return "Are you sure you want to resign? Type 'yes' to confirm or 'no' to cancel.";
    }

    private String handleResignConfirmation(String cmd) throws ResponseException {
        pendingResign = false;
        if (cmd.equals("yes")) {
            ws.send(new UserGameCommand(UserGameCommand.CommandType.RESIGN, authToken, currentGameID));
            return "";
        }
        return "Resignation cancelled.";
    }

    private String highlight(String[] params) {
        if (params.length != 2) {
            return "Usage: highlight <POSITION>";
        }
        if (currentGame == null) {
            return "No game loaded yet.";
        }
        ChessPosition pos = parsePosition(params[1]);
        if (pos == null) {
            return "Invalid position. Use format like 'e2'.";
        }
        var moves = currentGame.validMoves(pos);
        ChessGame.TeamColor perspective = (currentColor != null) ? currentColor : ChessGame.TeamColor.WHITE;
        return BoardRenderer.renderHighlighted(currentGame.getBoard(), perspective, pos, moves);
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
        connectWebSocket(game.gameID(), null);
        return "Observing game \"" + game.gameName() + "\".";
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
        connectWebSocket(game.gameID(), perspective);
        return "Joined game \"" + game.gameName() + "\" as " + color + ".";
    }

    private void connectWebSocket(int gameID, ChessGame.TeamColor color) throws ResponseException {
        ws = new WebSocketCommunicator();
        ws.connect(serverUrl, this);
        ws.send(new UserGameCommand(UserGameCommand.CommandType.CONNECT, authToken, gameID));
        currentGameID = gameID;
        currentColor = color;
    }

    @Override
    public void onLoadGame(ChessGame game) {
        currentGame = game;
        ChessGame.TeamColor perspective = (currentColor != null) ? currentColor : ChessGame.TeamColor.WHITE;
        System.out.println();
        System.out.println(BoardRenderer.render(game.getBoard(), perspective));
        System.out.print(promptString());
    }

    @Override
    public void onNotification(String message) {
        System.out.println();
        System.out.println("[NOTIFICATION] " + message);
        System.out.print(promptString());
    }

    @Override
    public void onError(String message) {
        System.out.println();
        System.out.println("[ERROR] " + message);
        System.out.print(promptString());
    }

    private String promptString() {
        if (isInGame()) {
            return "[IN_GAME] >>> ";
        } else if (isLoggedIn()) {
            return "[LOGGED_IN] >>> ";
        } else {
            return "[LOGGED_OUT] >>> ";
        }
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

    private String helpGameplay() {
        return """
                redraw - the chess board
                move <FROM> <TO> [PROMOTION] - make a move (e.g. e2 e4)
                highlight <POSITION> - legal moves from a square (e.g. e2)
                resign - forfeit the game
                leave - the game
                help - with possible commands""";
    }
}
