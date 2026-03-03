package service;

import dataaccess.MemoryAuthDAO;
import dataaccess.MemoryGameDAO;
import dataaccess.MemoryUserDAO;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {
    private ClearService clearService;
    private UserService userService;
    private MemoryUserDAO userDAO;
    private MemoryAuthDAO authDAO;
    private MemoryGameDAO gameDAO;

    @BeforeEach
    void setUp() {
        userDAO = new MemoryUserDAO();
        authDAO = new MemoryAuthDAO();
        gameDAO = new MemoryGameDAO();
        clearService = new ClearService(userDAO, authDAO, gameDAO);
        userService = new UserService(userDAO, authDAO);
    }

    @Test
    @DisplayName("Clear removes all data")
    void clearSuccess() throws ServiceException {
        userService.register(new UserData("alice", "pass", "a@mail.com"));
        assertDoesNotThrow(() -> clearService.clear());
        // After clear, login should fail since user no longer exists
        ServiceException ex = assertThrows(ServiceException.class, () ->
                userService.login("alice", "pass"));
        assertEquals(401, ex.getStatusCode());
    }
}
