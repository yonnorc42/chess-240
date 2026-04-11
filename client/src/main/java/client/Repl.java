package client;

import java.util.Scanner;

public class Repl {
    private final ChessClient client;

    public Repl(String serverUrl) {
        client = new ChessClient(serverUrl);
    }

    public void run() {
        System.out.println("♕ 240 Chess Client. Type help to get started.");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String prompt;
            if (client.isInGame()) {
                prompt = "[IN_GAME] >>> ";
            } else if (client.isLoggedIn()) {
                prompt = "[LOGGED_IN] >>> ";
            } else {
                prompt = "[LOGGED_OUT] >>> ";
            }
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                break;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String result = client.eval(line);
            if (result.equals("quit")) {
                break;
            }
            System.out.println(result);
        }
        System.out.println("Goodbye!");
    }
}
