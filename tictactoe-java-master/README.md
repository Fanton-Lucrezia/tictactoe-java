# Tic-Tac-Toe Online - Multiplayer Java

Gioco del Tris online multiplayer sviluppato in Java con interfaccia grafica Swing.

## Funzionalità

- **Multiplayer online**: Gioca contro altri giocatori connessi al server
- **Sistema di nickname**: Ogni giocatore sceglie un nickname unico
- **Sistema di sfide**: Puoi sfidare giocatori disponibili
- **Multithreading**: Il server gestisce più client contemporaneamente usando un thread pool
- **Comunicazione TCP**: Client e server comunicano tramite socket TCP

## Come funziona

### Architettura

1. **GameServer.java**: Server che gestisce le connessioni dei client
   - Usa `ServerSocket` per accettare connessioni sulla porta 12345
   - Utilizza un `ExecutorService` (thread pool) per gestire più client
   - Mantiene una `HashMap` dei giocatori connessi (nickname -> ClientHandler)
   - Gestisce le richieste di sfida e le partite

2. **GameClient.java**: Client che si connette al server
   - Si connette al server tramite `Socket`
   - Usa `BufferedReader` e `InputStreamReader` per leggere messaggi
   - Ha un thread secondario che esegue continuamente `readLine()` per ascoltare i messaggi
   - Gestisce l'invio delle mosse e la ricezione degli aggiornamenti

3. **MainMenu.java**: Interfaccia per inserire nickname e scegliere avversario
   - Schermata iniziale per inserire il nickname
   - Lista dei giocatori disponibili
   - Sistema per inviare/ricevere richieste di sfida

4. **TicTacToeOnline.java**: Interfaccia di gioco multiplayer
   - Griglia 3x3 per il gioco del tris
   - Gestisce i turni tra i due giocatori
   - Controlla vittorie, pareggi e coordinate valide

## Protocollo di comunicazione

### Messaggi Client -> Server
- `SET-NICKNAME=<nickname>`: Registra il nickname
- `GET-PLAYERS`: Richiede la lista dei giocatori disponibili
- `CHALLENGE=<player>`: Invia richiesta di sfida a un giocatore
- `ACCEPT-CHALLENGE=<challenger>`: Accetta una sfida
- `DECLINE-CHALLENGE=<challenger>`: Rifiuta una sfida
- `MOVE=<nick1>=<nick2>=<symbol>=<row>=<col>`: Invia una mossa
- `GAME-OVER`: Notifica la fine della partita

### Messaggi Server -> Client
- `NICKNAME-SUCCESS`: Nickname registrato con successo
- `NOT-VALID`: Nickname già in uso
- `PLAYERS=<lista>`: Lista dei giocatori disponibili (separati da virgola)
- `CHALLENGE-REQUEST=<challenger>`: Ricevuta richiesta di sfida
- `CHALLENGE-DECLINED=<player>`: Sfida rifiutata
- `GAME-START=<opponent>=<symbol>`: Inizio partita (X o O)
- `MOVE=<nick1>=<nick2>=<symbol>=<row>=<col>`: Mossa dell'avversario
- `OPPONENT-DISCONNECTED`: L'avversario si è disconnesso

## Come eseguire

### 1. Avviare il server
```bash
javac GameServer.java
java GameServer
```
Il server si avvierà sulla porta 12345.

### 2. Avviare i client
In finestre separate (minimo 2 giocatori):
```bash
javac MainMenu.java GameClient.java TicTacToeOnline.java
java MainMenu
```

### 3. Giocare
1. Inserisci il tuo nickname
2. Aspetta che altri giocatori si connettano
3. Clicca "Aggiorna" per vedere i giocatori disponibili
4. Seleziona un giocatore e clicca "Sfida"
5. L'altro giocatore riceverà la richiesta e potrà accettare o rifiutare
6. Una volta accettata, inizia la partita!
7. Il giocatore con la X inizia per primo

## Gestione errori

- **Coordinate non valide**: Se le coordinate sono fuori dal range 0-2, la mossa viene ignorata
- **Nickname duplicato**: Il server controlla che il nickname non sia già in uso
- **Disconnessione**: Se un giocatore si disconnette, l'avversario viene notificato
- **Casella occupata**: Non è possibile cliccare su una casella già occupata

## Struttura del codice

```
tictactoe-java-master/
├── GameServer.java          # Server TCP con thread pool
├── GameClient.java          # Client TCP con thread listener
├── MainMenu.java            # Menu principale e gestione nickname/challenge
├── TicTacToeOnline.java     # Interfaccia di gioco multiplayer
├── TicTacToe.java           # Versione originale single player (non usata)
└── App.java                 # Vecchio launcher (non usato)
```

## Note tecniche

- Il server usa `Executors.newCachedThreadPool()` per creare thread dinamicamente
- Ogni client ha un thread dedicato (`ClientHandler`) sul server
- Il client ha un thread secondario che esegue continuamente `readLine()` per ricevere messaggi in tempo reale
- La `HashMap` sul server associa nickname a socket per instradare i messaggi
- Le coordinate sono passate come riga e colonna (0-2 per entrambe)
