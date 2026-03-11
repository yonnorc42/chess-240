package dataaccess;

import model.UserData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlUserDAOTest {
    private MySqlUserDAO userDAO;

    @BeforeAll
    static void init() throws DataAccessException {
        DatabaseManager.configureDatabase();
    }

    @BeforeEach
    void setUp() throws DataAccessException {
        userDAO = new MySqlUserDAO();
        userDAO.clear();
    }

    @Test
    @DisplayName("Create and get user success")
    void createAndGetUser() throws DataAccessException {
        UserData user = new UserData("alice", "password123", "alice@mail.com");
        userDAO.createUser(user);
        UserData result = userDAO.getUser("alice");
        assertNotNull(result);
        assertEquals("alice", result.username());
        assertEquals("alice@mail.com", result.email());
    }

    @Test
    @DisplayName("Create duplicate user throws")
    void createDuplicateUser() throws DataAccessException {
        UserData user = new UserData("alice", "password123", "alice@mail.com");
        userDAO.createUser(user);
        assertThrows(DataAccessException.class, () ->
                userDAO.createUser(new UserData("alice", "other", "other@mail.com")));
    }

    @Test
    @DisplayName("Get nonexistent user returns null")
    void getNonexistentUser() throws DataAccessException {
        assertNull(userDAO.getUser("nobody"));
    }

    @Test
    @DisplayName("Clear removes all users")
    void clearUsers() throws DataAccessException {
        userDAO.createUser(new UserData("alice", "pass", "a@mail.com"));
        userDAO.createUser(new UserData("bob", "pass", "b@mail.com"));
        userDAO.clear();
        assertNull(userDAO.getUser("alice"));
        assertNull(userDAO.getUser("bob"));
    }
}
