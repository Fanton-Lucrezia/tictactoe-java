import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.IOException;

public class MainMenu {
    private JFrame frame;
    private GameClient client;
    private String myNickname;
    private String pendingOpponent;
    private String pendingSymbol;

    public MainMenu() {
        frame = new JFrame("Tic-Tac-Toe Online - Menu");
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        showNicknameScreen();
        
        frame.setVisible(true);
    }

    /*Mostra la schermata per inserire il nickname*/
    private void showNicknameScreen() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.darkGray);
        panel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        JLabel titleLabel = new JLabel("Benvenuto!");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setForeground(Color.white);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel instructionLabel = new JLabel("Inserisci il tuo nickname:");
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        instructionLabel.setForeground(Color.white);
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField nicknameField = new JTextField(20);
        nicknameField.setMaximumSize(new Dimension(300, 40));
        nicknameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nicknameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton connectButton = new JButton("Connetti");
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel errorLabel = new JLabel("");
        errorLabel.setForeground(Color.red);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        connectButton.addActionListener(e -> {
            String nickname = nicknameField.getText().trim();
            if (nickname.isEmpty()) {
                errorLabel.setText("Il nickname non può essere vuoto!");
                return;
            }

            try {
                //Connessione al server
                client = new GameClient("localhost", 12345);
                client.setMenuUI(this);
                myNickname = nickname;
                client.sendNickname(nickname);
                
            } catch (IOException ex) {
                errorLabel.setText("Impossibile connettersi al server!");
                ex.printStackTrace();
            }
        });

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(instructionLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(nicknameField);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(connectButton);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(errorLabel);

        frame.getContentPane().removeAll();
        frame.add(panel);
        frame.revalidate();
        frame.repaint();
    }

    /*Mostra la schermata per selezionare l'avversario*/
    private void showPlayerSelection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(Color.darkGray);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Giocatori disponibili");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.white);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> playerList = new JList<>(listModel);
        playerList.setFont(new Font("Arial", Font.PLAIN, 16));
        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(playerList);

        JButton challengeButton = new JButton("Sfida");
        challengeButton.setFont(new Font("Arial", Font.BOLD, 16));
        
        JButton refreshButton = new JButton("Aggiorna");
        refreshButton.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.darkGray);
        buttonPanel.add(refreshButton);
        buttonPanel.add(challengeButton);

        refreshButton.addActionListener(e -> {
            client.requestPlayerList();
        });

        challengeButton.addActionListener(e -> {
            String selected = playerList.getSelectedValue();
            if (selected != null && !selected.isEmpty()) {
                client.sendChallenge(selected);
                JOptionPane.showMessageDialog(frame, 
                    "Richiesta di sfida inviata a " + selected,
                    "Sfida", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        frame.getContentPane().removeAll();
        frame.add(panel);
        frame.revalidate();
        frame.repaint();

        //Richiedi la lista dei giocatori
        client.requestPlayerList();
    }

    /*Gestisce il successo del nickname*/
    public void handleNicknameSuccess() {
        SwingUtilities.invokeLater(() -> {
            showPlayerSelection();
        });
    }

    /*Gestisce l'errore del nickname*/
    public void handleNicknameError() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame, 
                "Nickname già in uso! Scegline un altro.",
                "Errore", 
                JOptionPane.ERROR_MESSAGE);
        });
    }

    /*Gestisce la lista dei giocatori*/
    public void handlePlayerList(String players) {
        SwingUtilities.invokeLater(() -> {
            try {
                Component[] components = ((JPanel) frame.getContentPane().getComponent(0)).getComponents();
                for (Component comp : components) {
                    if (comp instanceof JScrollPane) {
                        JScrollPane scroll = (JScrollPane) comp;
                        JList<String> list = (JList<String>) scroll.getViewport().getView();
                        DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
                        model.clear();
                        
                        if (!players.isEmpty()) {
                            String[] playerArray = players.split(",");
                            for (String player : playerArray) {
                                if (!player.trim().isEmpty()) {
                                    model.addElement(player.trim());
                                }
                            }
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Errore aggiornamento lista: " + e.getMessage());
            }
        });
    }

    /*Gestisce la richiesta di sfida ricevuta*/
    public void handleChallengeRequest(String challenger) {
        SwingUtilities.invokeLater(() -> {
            int result = JOptionPane.showConfirmDialog(frame,
                "Richiesta di challenge da " + challenger + ". Accetti?",
                "Richiesta di Sfida",
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                client.acceptChallenge(challenger);
            } else {
                client.declineChallenge(challenger);
            }
        });
    }

    /*Gestisce il rifiuto della sfida*/
    public void handleChallengeDeclined(String player) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame,
                player + " ha rifiutato la tua sfida.",
                "Sfida rifiutata",
                JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /*Gestisce l'inizio della partita*/
    public void handleGameStart(String opponent, String symbol) {
        //Salva i dati temporaneamente
        this.pendingOpponent = opponent;
        this.pendingSymbol = symbol;
        
        SwingUtilities.invokeLater(() -> {
            //Crea prima la finestra di gioco
            TicTacToeOnline game = new TicTacToeOnline(client);
            
            //Poi chiama handleGameStart con i dati salvati
            game.handleGameStart(pendingOpponent, pendingSymbol);
            
            //Infine chiudi il menu
            frame.dispose();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainMenu();
        });
    }
}
