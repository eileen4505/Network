// Server.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 1234;
    private static final int MAX_THREADS = 10;
    private static final List<String[]> QUESTIONS = Arrays.asList(
            new String[]{"Which country is the origin of the Olympic Games?", "Greece"},
            new String[]{"What is the hardest natural substance on Earth?", "Diamond"},
            new String[]{"What is the freezing point of water in Celsius?", "0"}
    );

    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    private static final ConcurrentHashMap<String, Integer> clientScores = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            System.out.println("Maximum concurrent clients: " + MAX_THREADS);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    String clientId = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                    System.out.println("New client connected: " + clientId);
                    clientScores.put(clientId, 0);
                    threadPool.execute(new ClientHandler(clientSocket, clientId));
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT);
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final String clientId;

        public ClientHandler(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                System.out.println("Starting quiz for client: " + clientId);

                // Send welcome message
                out.println("Welcome to the Quiz Game! You are player: " + clientId);
                out.println("There are " + QUESTIONS.size() + " questions in total.");

                int questionNumber = 1;
                for (String[] qa : QUESTIONS) {
                    out.println("Question " + questionNumber + "/" + QUESTIONS.size() + ": " + qa[0]);

                    String response = in.readLine();
                    if (response == null) {
                        break; // Client disconnected
                    }

                    if (response.equalsIgnoreCase(qa[1])) {
                        out.println("Correct!");
                        clientScores.computeIfPresent(clientId, (k, v) -> v + 1);
                    } else {
                        out.println("Incorrect! The correct answer is: " + qa[1]);
                    }

                    questionNumber++;
                }

                // Send final score
                Integer finalScore = clientScores.get(clientId);
                out.println("Quiz Over! Your final score is: " + finalScore + "/" + QUESTIONS.size());

                // Show current leaderboard
                out.println("\nCurrent Active Players Scores:");
                clientScores.forEach((id, score) -> out.println(id + ": " + score + "/" + QUESTIONS.size()));

            } catch (IOException e) {
                System.err.println("Error handling client " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    clientScores.remove(clientId);
                    socket.close();
                    System.out.println("Client disconnected: " + clientId);
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
}
