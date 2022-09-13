package it.di.unipi.mat582418.wifibattle.Game;
import it.di.unipi.mat582418.wifibattle.ConnectionService;

public class GameThread extends Thread {
    public ConnectionService connection_service;
    private boolean i_am_the_server;

    public GameThread(ConnectionService cs,boolean server){
        i_am_the_server=server;
        connection_service=cs;
    }

    @Override
    public void run() {
        super.run();
        connection_service.ready(i_am_the_server);

        //Fin quando la partita non è terminata oppure si è verificato un errore
        while(!connection_service.isGameStopped() &&!isInterrupted()) {
            //Svolgo un turno di gioco (per turno si intende un colpo per ciascun giocatore)
            connection_service.game_turn(i_am_the_server);
        }
    }
}
