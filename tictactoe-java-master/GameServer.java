import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 12345;
    private static HashMap<String, ClientHandler> players = new HashMap<>();
    private static HashMap<String, String> challenges = new HashMap<>();
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("Server avviato sulla porta " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuova connessione da: " + clientSocket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Errore del server: " + e.getMessage());
        }
    }

    /*Classe per gestire ogni singolo client*/
    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        private String opponent;
        private boolean inGame;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.inGame = false;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Connessione persa: " + nickname);
            } finally {
                cleanup();
            }
        }

        /*Gestisce i messaggi ricevuti dal client*/
        private void handleMessage(String message) {
            System.out.println("Ricevuto: " + message);

            if (message.startsWith("SET-NICKNAME=")) {
                handleNickname(message.substring(13));
            } else if (message.startsWith("GET-PLAYERS")) {
                sendPlayerList();
            } else if (message.startsWith("CHALLENGE=")) {
                handleChallenge(message.substring(10));
            } else if (message.startsWith("ACCEPT-CHALLENGE=")) {
                handleAcceptChallenge(message.substring(17));
            } else if (message.startsWith("DECLINE-CHALLENGE=")) {
                handleDeclineChallenge(message.substring(18));
            } else if (message.startsWith("MOVE=")) {
                handleMove(message);
            } else if (message.startsWith("GAME-OVER")) {
                handleGameOver();
            }
        }

        /*Controlla e registra il nickname*/
        private void handleNickname(String nick) {
            synchronized (players) {
                if (players.containsKey(nick)) {
                    out.println("NOT-VALID");
                } else {
                    this.nickname = nick;
                    players.put(nick, this);
                    out.println("NICKNAME-SUCCESS");
                    System.out.println("Registrato: " + nick);
                }
            }
        }

        /*Invia la lista dei giocatori disponibili*/
        private void sendPlayerList() {
            StringBuilder list = new StringBuilder("PLAYERS=");
            synchronized (players) {
                for (String player : players.keySet()) {
                    if (!player.equals(nickname) && !players.get(player).inGame) {
                        list.append(player).append(",");
                    }
                }
            }
            out.println(list.toString());
        }

        /*Gestisce la richiesta di sfida*/
        private void handleChallenge(String targetPlayer) {
            ClientHandler target = players.get(targetPlayer);
            if (target != null && !target.inGame) {
                challenges.put(targetPlayer, nickname);
                target.out.println("CHALLENGE-REQUEST=" + nickname);
            }
        }

        /*Gestisce l'accettazione della sfida*/
        private void handleAcceptChallenge(String challenger) {
            ClientHandler challengerHandler = players.get(challenger);
            if (challengerHandler != null) {
                this.inGame = true;
                challengerHandler.inGame = true;
                this.opponent = challenger;
                challengerHandler.opponent = this.nickname;

                //Il primo giocatore usa X
                challengerHandler.out.println("GAME-START=" + this.nickname + "=X");
                this.out.println("GAME-START=" + challenger + "=O");
                
                challenges.remove(this.nickname);
            }
        }

        /*Gestisce il rifiuto della sfida*/
        private void handleDeclineChallenge(String challenger) {
            ClientHandler challengerHandler = players.get(challenger);
            if (challengerHandler != null) {
                challengerHandler.out.println("CHALLENGE-DECLINED=" + this.nickname);
            }
            challenges.remove(this.nickname);
        }

        /*Gestisce le mosse del gioco*/
        private void handleMove(String message) {
            if (opponent != null) {
                ClientHandler opponentHandler = players.get(opponent);
                if (opponentHandler != null) {
                    opponentHandler.out.println(message);
                }
            }
        }

        /*Gestisce la fine della partita*/
        private void handleGameOver() {
            this.inGame = false;
            if (opponent != null) {
                ClientHandler opponentHandler = players.get(opponent);
                if (opponentHandler != null) {
                    opponentHandler.inGame = false;
                    opponentHandler.opponent = null;
                }
                this.opponent = null;
            }
        }

        /*Pulizia quando il client si disconnette*/
        private void cleanup() {
            if (nickname != null) {
                synchronized (players) {
                    players.remove(nickname);
                }
                if (opponent != null) {
                    ClientHandler opponentHandler = players.get(opponent);
                    if (opponentHandler != null) {
                        opponentHandler.out.println("OPPONENT-DISCONNECTED");
                        opponentHandler.inGame = false;
                        opponentHandler.opponent = null;
                    }
                }
                System.out.println("Disconnesso: " + nickname);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
