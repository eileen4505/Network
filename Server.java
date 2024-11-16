import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static final List<String[]> QUESTIONS = Arrays.asList(
            new String[]{"Which country is the origin of the Olympic Games?", "Greece"},
            new String[]{"What is the hardest natural substance on Earth?", "Diamond"},
            new String[]{"What is the freezing point of water in Celsius?", "0"}
    );

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                int score = 0;

                for (String[] qa : QUESTIONS) {
                    out.println(qa[0]); // Send question
                    String response = in.readLine();

                    if (response != null && response.equalsIgnoreCase(qa[1])) {
                        out.println("Correct!");
                        score++;
                    } else {
                        out.println("Incorrect!");
                    }
                }
                out.println("Quiz Over! ");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {

                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

