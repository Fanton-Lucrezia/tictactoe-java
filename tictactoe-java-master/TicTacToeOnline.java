import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TicTacToeOnline {
    int boardWidth = 600;
    int boardHeight = 650;

    JFrame frame = new JFrame("Tic-Tac-Toe Online");
    JLabel textLabel = new JLabel();
    JPanel textPanel = new JPanel();
    JPanel boardPanel = new JPanel();

    JButton[][] board = new JButton[3][3];
    String currentPlayer;
    boolean gameOver = false;
    int turns = 0;

    GameClient client;
    String mySymbol;
    boolean myTurn = false;

    TicTacToeOnline(GameClient client) {
        this.client = client;
        client.setUI(this);

        frame.setVisible(true);
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textLabel.setBackground(Color.darkGray);
        textLabel.setForeground(Color.white);
        textLabel.setFont(new Font("Arial", Font.BOLD, 40));
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setText("Caricamento...");
        textLabel.setOpaque(true);

        textPanel.setLayout(new BorderLayout());
        textPanel.add(textLabel);
        frame.add(textPanel, BorderLayout.NORTH);

        boardPanel.setLayout(new GridLayout(3, 3));
        boardPanel.setBackground(Color.darkGray);
        frame.add(boardPanel);

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton tile = new JButton();
                board[r][c] = tile;
                boardPanel.add(tile);

                tile.setBackground(Color.darkGray);
                tile.setForeground(Color.white);
                tile.setFont(new Font("Arial", Font.BOLD, 120));
                tile.setFocusable(false);

                int row = r;
                int col = c;

                tile.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (gameOver || !myTurn) return;
                        
                        JButton tile = (JButton) e.getSource();
                        if (tile.getText().equals("")) {
                            tile.setText(mySymbol);
                            turns++;
                            
                            //Invia la mossa al server
                            client.sendMove(row, col, mySymbol);
                            
                            myTurn = false;
                            checkWinner();
                            
                            if (!gameOver) {
                                textLabel.setText("Turno di " + client.getOpponent());
                            }
                        }
                    }
                });
            }
        }
    }

    /*Gestisce l'inizio della partita*/
    public void handleGameStart(String opponent, String symbol) {
        SwingUtilities.invokeLater(() -> {
            this.mySymbol = symbol;
            this.currentPlayer = "X";
            
            System.out.println("Partita iniziata! Io sono " + symbol + ", avversario: " + opponent);
            
            //Il giocatore X inizia sempre per primo
            if (symbol.equals("X")) {
                myTurn = true;
                textLabel.setText("Il tuo turno! Sei " + symbol);
            } else {
                myTurn = false;
                textLabel.setText("Turno di " + opponent + " (X)");
            }
        });
    }

    /*Gestisce la mossa dell'avversario*/
    public void handleOpponentMove(int row, int col, String symbol) {
        SwingUtilities.invokeLater(() -> {
            //Controlla che le coordinate siano valide
            if (row < 0 || row > 2 || col < 0 || col > 2) {
                System.err.println("Coordinate non valide: " + row + ", " + col);
                return;
            }

            board[row][col].setText(symbol);
            turns++;
            myTurn = true;
            
            checkWinner();
            
            if (!gameOver) {
                textLabel.setText("Il tuo turno! Sei " + mySymbol);
            }
        });
    }

    /*Controlla se c'è un vincitore*/
    void checkWinner() {
        //Controllo orizzontale
        for (int r = 0; r < 3; r++) {
            if (board[r][0].getText().equals("")) continue;

            if (board[r][0].getText().equals(board[r][1].getText()) &&
                board[r][1].getText().equals(board[r][2].getText())) {
                for (int i = 0; i < 3; i++) {
                    setWinner(board[r][i]);
                }
                gameOver = true;
                client.sendGameOver();
                return;
            }
        }

        //Controllo verticale
        for (int c = 0; c < 3; c++) {
            if (board[0][c].getText().equals("")) continue;
            
            if (board[0][c].getText().equals(board[1][c].getText()) &&
                board[1][c].getText().equals(board[2][c].getText())) {
                for (int i = 0; i < 3; i++) {
                    setWinner(board[i][c]);
                }
                gameOver = true;
                client.sendGameOver();
                return;
            }
        }

        //Controllo diagonale
        if (!board[0][0].getText().equals("") &&
            board[0][0].getText().equals(board[1][1].getText()) &&
            board[1][1].getText().equals(board[2][2].getText())) {
            for (int i = 0; i < 3; i++) {
                setWinner(board[i][i]);
            }
            gameOver = true;
            client.sendGameOver();
            return;
        }

        //Controllo anti-diagonale
        if (!board[0][2].getText().equals("") &&
            board[0][2].getText().equals(board[1][1].getText()) &&
            board[1][1].getText().equals(board[2][0].getText())) {
            setWinner(board[0][2]);
            setWinner(board[1][1]);
            setWinner(board[2][0]);
            gameOver = true;
            client.sendGameOver();
            return;
        }

        //Controllo pareggio
        if (turns == 9) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    setTie(board[r][c]);
                }
            }
            gameOver = true;
            client.sendGameOver();
        }
    }

    /*Imposta il vincitore*/
    void setWinner(JButton tile) {
        tile.setForeground(Color.green);
        tile.setBackground(Color.gray);
        
        String winner = tile.getText().equals(mySymbol) ? "Tu" : client.getOpponent();
        textLabel.setText(winner + " ha vinto!");
    }

    /*Imposta il pareggio*/
    void setTie(JButton tile) {
        tile.setForeground(Color.orange);
        tile.setBackground(Color.gray);
        textLabel.setText("Pareggio!");
    }

    /*Gestisce la disconnessione dell'avversario*/
    public void handleOpponentDisconnected() {
        JOptionPane.showMessageDialog(frame, 
            "L'avversario si è disconnesso!", 
            "Disconnessione", 
            JOptionPane.WARNING_MESSAGE);
        frame.dispose();
    }

    /*Metodi chiamati dal client per gestire eventi*/
    public void handleNicknameSuccess() {}
    public void handleNicknameError() {}
    public void handlePlayerList(String players) {}
    public void handleChallengeRequest(String challenger) {}
    public void handleChallengeDeclined(String player) {}
}
