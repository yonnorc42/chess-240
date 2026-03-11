package dataaccess;

import model.AuthData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlAuthDAOTest {
    private MySqlAuthDAO authDAO;

    @BeforeAll
    static void init() throws DataAccessException {
        DatabaseManager.configureDatabase();
    }

    @BeforeEach
    void setUp() throws DataAccessException {
        authDAO = new MySqlAuthDAO();
        authDAO.clear();
    }

    @Test
    @DisplayName("Create and get auth success")
    void createAndGetAuth() throws DataAccessException {
        AuthData auth = new AuthData("token123", "alice");
        authDAO.createAuth(auth);
        AuthData result = authDAO.getAuth("token123");
        assertNotNull(result);
        assertEquals("token123", result.authToken());
        assertEquals("alice", result.username());
    }

    @Test
    @DisplayName("Get nonexistent auth returns null")
    void getNonexistentAuth() throws DataAccessException {
        assertNull(authDAO.getAuth("bad-token"));
    }

    @Test
    @DisplayName("Delete auth success")
    void deleteAuth() throws DataAccessException {
        authDAO.createAuth(new AuthData("token123", "alice"));
        authDAO.deleteAuth("token123");
        assertNull(authDAO.getAuth("token123"));
    }

    @Test
    @DisplayName("Delete nonexistent auth throws")
    void deleteNonexistentAuth() {
        assertThrows(DataAccessException.class, () ->
                authDAO.deleteAuth("bad-token"));
    }

    @Test
    @DisplayName("Clear removes all auth tokens")
    void clearAuth() throws DataAccessException {
        authDAO.createAuth(new AuthData("token1", "alice"));
        authDAO.createAuth(new AuthData("token2", "bob"));
        authDAO.clear();
        assertNull(authDAO.getAuth("token1"));
        assertNull(authDAO.getAuth("token2"));
    }
}
