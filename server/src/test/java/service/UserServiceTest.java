package service;

import dataaccess.MemoryAuthDAO;
import dataaccess.MemoryUserDAO;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    private UserService userService;
    private MemoryUserDAO userDAO;
    private MemoryAuthDAO authDAO;

    @BeforeEach
    void setUp() {
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        userService = new UserService(userDAO, authDAO);
    }

    @Test
    @DisplayName("Register success")
    void registerSuccess() throws ServiceException {
        AuthData result = userService.register(new UserData("alice", "pass", "a@mail.com"));
        assertEquals("alice", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    @DisplayName("Register duplicate username")
    void registerDuplicate() throws ServiceException {
        userService.register(new UserData("alice", "pass", "a@mail.com"));
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.register(new UserData("alice", "other", "b@mail.com")));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    @DisplayName("Login success")
    void loginSuccess() throws ServiceException {
        userService.register(new UserData("alice", "pass", "a@mail.com"));
        AuthData result = userService.login("alice", "pass");
        assertEquals("alice", result.username());
        assertNotNull(result.authToken());
    }

    @Test
    @DisplayName("Login wrong password")
    void loginWrongPassword() throws ServiceException {
        userService.register(new UserData("alice", "pass", "a@mail.com"));
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.login("alice", "wrong"));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    @DisplayName("Logout success")
    void logoutSuccess() throws ServiceException {
        AuthData auth = userService.register(new UserData("alice", "pass", "a@mail.com"));
        assertDoesNotThrow(() -> userService.logout(auth.authToken()));
    }

    @Test
    @DisplayName("Logout invalid token")
    void logoutInvalidToken() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.logout("bad-token"));
        assertEquals(401, ex.getStatusCode());
    }
}
