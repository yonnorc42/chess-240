package client;

import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearDatabase() throws ResponseException {
        facade.clear();
    }

    @Test
    @DisplayName("Clear succeeds")
    void clearSuccess() {
        assertDoesNotThrow(() -> facade.clear());
    }

    @Test
    @DisplayName("Clear works on empty database")
    void clearEmpty() throws ResponseException {
        facade.clear();
        assertDoesNotThrow(() -> facade.clear());
    }

    @Test
    @DisplayName("Register success")
    void registerSuccess() throws ResponseException {
        AuthData auth = facade.register("alice", "pass", "a@mail.com");
        assertEquals("alice", auth.username());
        assertNotNull(auth.authToken());
    }

    @Test
    @DisplayName("Register duplicate user fails")
    void registerDuplicate() throws ResponseException {
        facade.register("alice", "pass", "a@mail.com");
        assertThrows(ResponseException.class, () ->
                facade.register("alice", "other", "b@mail.com"));
    }

    @Test
    @DisplayName("Login success")
    void loginSuccess() throws ResponseException {
        facade.register("alice", "pass", "a@mail.com");
        AuthData auth = facade.login("alice", "pass");
        assertEquals("alice", auth.username());
        assertNotNull(auth.authToken());
    }

    @Test
    @DisplayName("Login wrong password fails")
    void loginWrongPassword() throws ResponseException {
        facade.register("alice", "pass", "a@mail.com");
        assertThrows(ResponseException.class, () ->
                facade.login("alice", "wrong"));
    }

    @Test
    @DisplayName("Logout success")
    void logoutSuccess() throws ResponseException {
        AuthData auth = facade.register("alice", "pass", "a@mail.com");
        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }

    @Test
    @DisplayName("Logout with bad token fails")
    void logoutBadToken() {
        assertThrows(ResponseException.class, () ->
                facade.logout("bad-token"));
    }

    @Test
    @DisplayName("Create game success")
    void createGameSuccess() throws ResponseException {
        AuthData auth = facade.register("alice", "pass", "a@mail.com");
        int id = facade.createGame(auth.authToken(), "my game");
        assertTrue(id > 0);
    }

    @Test
    @DisplayName("Create game unauthorized fails")
    void createGameUnauthorized() {
        assertThrows(ResponseException.class, () ->
                facade.createGame("bad-token", "my game"));
    }

    @Test
    @DisplayName("List games success")
    void listGamesSuccess() throws ResponseException {
        AuthData auth = facade.register("alice", "pass", "a@mail.com");
        facade.createGame(auth.authToken(), "game1");
        facade.createGame(auth.authToken(), "game2");
        GameData[] games = facade.listGames(auth.authToken());
        assertEquals(2, games.length);
    }

    @Test
    @DisplayName("List games unauthorized fails")
    void listGamesUnauthorized() {
        assertThrows(ResponseException.class, () ->
                facade.listGames("bad-token"));
    }

    @Test
    @DisplayName("Join game success")
    void joinGameSuccess() throws ResponseException {
        AuthData auth = facade.register("alice", "pass", "a@mail.com");
        int gameID = facade.createGame(auth.authToken(), "my game");
        assertDoesNotThrow(() ->
                facade.joinGame(auth.authToken(), "WHITE", gameID));
    }

    @Test
    @DisplayName("Join game color already taken fails")
    void joinGameColorTaken() throws ResponseException {
        AuthData auth1 = facade.register("alice", "pass", "a@mail.com");
        AuthData auth2 = facade.register("bob", "pass", "b@mail.com");
        int gameID = facade.createGame(auth1.authToken(), "my game");
        facade.joinGame(auth1.authToken(), "WHITE", gameID);
        assertThrows(ResponseException.class, () ->
                facade.joinGame(auth2.authToken(), "WHITE", gameID));
    }
}
