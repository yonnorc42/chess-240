package service;

import dataaccess.AuthDAO;
import dataaccess.DataAccessException;
import dataaccess.UserDAO;
import model.AuthData;
import model.UserData;

import java.util.UUID;

public class UserService {
    private final UserDAO userDAO;
    private final AuthDAO authDAO;

    public UserService(UserDAO userDAO, AuthDAO authDAO) {
        this.userDAO = userDAO;
        this.authDAO = authDAO;
    }

    public AuthData register(UserData user) throws ServiceException {
        if (user.username() == null || user.password() == null || user.email() == null) {
            throw new ServiceException(400, "Error: bad request");
        }
        try {
            if (userDAO.getUser(user.username()) != null) {
                throw new ServiceException(403, "Error: already taken");
            }
            userDAO.createUser(user);
            return createAuthToken(user.username());
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }

    public AuthData login(String username, String password) throws ServiceException {
        if (username == null || password == null) {
            throw new ServiceException(400, "Error: bad request");
        }
        try {
            UserData user = userDAO.getUser(username);
            if (user == null || !user.password().equals(password)) {
                throw new ServiceException(401, "Error: unauthorized");
            }
            return createAuthToken(username);
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }

    public void logout(String authToken) throws ServiceException {
        try {
            if (authDAO.getAuth(authToken) == null) {
                throw new ServiceException(401, "Error: unauthorized");
            }
            authDAO.deleteAuth(authToken);
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }

    private AuthData createAuthToken(String username) throws DataAccessException {
        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);
        authDAO.createAuth(auth);
        return auth;
    }
}
