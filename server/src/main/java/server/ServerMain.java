package server;

public class ServerMain {
    public static void main(String[] args) {
        Server server = new Server();
        int port = server.run(8080);
        System.out.println("♕ 240 Chess Server started on port " + port);
    }
}
