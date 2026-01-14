import java.io.*;
import java.net.*;

public class GameClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private String opponent;
    private String mySymbol;
    private TicTacToeOnline gameUI;
    private MainMenu menuUI;
    private Thread listenerThread;

    public GameClient(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        
        //Thread secondario che ascolta continuamente i messaggi dal server
        listenerThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Disconnesso dal server");
            }
        });
        listenerThread.start();
    }

    /*Imposta l'interfaccia del menu*/
    public void setMenuUI(MainMenu menu) {
        this.menuUI = menu;
    }

    /*Imposta l'interfaccia di gioco*/
    public void setUI(TicTacToeOnline ui) {
        this.gameUI = ui;
    }

    /*Invia il nickname al server*/
    public void sendNickname(String nick) {
        this.nickname = nick;
        out.println("SET-NICKNAME=" + nick);
    }

    /*Richiede la lista dei giocatori disponibili*/
    public void requestPlayerList() {
        out.println("GET-PLAYERS");
    }

    /*Invia una richiesta di sfida*/
    public void sendChallenge(String player) {
        out.println("CHALLENGE=" + player);
    }

    /*Accetta una sfida*/
    public void acceptChallenge(String challenger) {
        out.println("ACCEPT-CHALLENGE=" + challenger);
    }

    /*Rifiuta una sfida*/
    public void declineChallenge(String challenger) {
        out.println("DECLINE-CHALLENGE=" + challenger);
    }

    /*Invia una mossa al server*/
    public void sendMove(int row, int col, String symbol) {
        String message = String.format("MOVE=%s=%s=%s=%d=%d", 
            nickname, opponent, symbol, row, col);
        out.println(message);
    }

    /*Notifica la fine della partita*/
    public void sendGameOver() {
        out.println("GAME-OVER");
    }

    /*Gestisce i messaggi ricevuti dal server*/
    private void handleServerMessage(String message) {
        System.out.println("Dal server: " + message);

        if (message.equals("NICKNAME-SUCCESS")) {
            if (menuUI != null) menuUI.handleNicknameSuccess();
        } else if (message.equals("NOT-VALID")) {
            if (menuUI != null) menuUI.handleNicknameError();
        } else if (message.startsWith("PLAYERS=")) {
            String playerList = message.substring(8);
            if (menuUI != null) menuUI.handlePlayerList(playerList);
        } else if (message.startsWith("CHALLENGE-REQUEST=")) {
            String challenger = message.substring(18);
            if (menuUI != null) menuUI.handleChallengeRequest(challenger);
        } else if (message.startsWith("CHALLENGE-DECLINED=")) {
            String player = message.substring(19);
            if (menuUI != null) menuUI.handleChallengeDeclined(player);
        } else if (message.startsWith("GAME-START=")) {
            String[] parts = message.split("=");
            this.opponent = parts[1];
            this.mySymbol = parts[2];
            if (menuUI != null) menuUI.handleGameStart(opponent, mySymbol);
            if (gameUI != null) gameUI.handleGameStart(opponent, mySymbol);
        } else if (message.startsWith("MOVE=")) {
            handleOpponentMove(message);
        } else if (message.equals("OPPONENT-DISCONNECTED")) {
            if (gameUI != null) gameUI.handleOpponentDisconnected();
        }
    }

    /*Gestisce la mossa dell'avversario*/
    private void handleOpponentMove(String message) {
        try {
            String[] parts = message.split("=");
            //MOVE=nickname=opponent=symbol=row=col
            int row = Integer.parseInt(parts[4]);
            int col = Integer.parseInt(parts[5]);
            String symbol = parts[3];
            
            if (gameUI != null) gameUI.handleOpponentMove(row, col, symbol);
        } catch (Exception e) {
            System.err.println("Errore nel parsing della mossa: " + e.getMessage());
        }
    }

    /*Chiude la connessione*/
    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getOpponent() {
        return opponent;
    }

    public String getMySymbol() {
        return mySymbol;
    }
}
