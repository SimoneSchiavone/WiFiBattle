package it.di.unipi.mat582418.wifibattle.WifiConnection;

import java.net.InetAddress;
import it.di.unipi.mat582418.wifibattle.ConnectionService;


public class HandshakeThread extends Thread {

    public ConnectionService connection_service;
    private String picture_uri;
    private boolean i_am_the_server;
    private InetAddress address;

    public HandshakeThread(ConnectionService cs, String b, boolean server,InetAddress add){
        i_am_the_server=server;
        picture_uri=b;
        connection_service=cs;
        address=add;
        if(server){
            if(connection_service.createServerSocket()) {
                //CreateServerSocket OK
            }else{
                //CreaseServerSocket ERRORE
            }
        }
    }

    @Override
    public void run() {
        super.run();
        if(!i_am_the_server){ //Sono il client
            if(address!=null)
                ActAsClient(address);
        }else{ //Sono il server
            ActAsServer();
        }
    }

    private void ActAsClient(InetAddress addr){
            if(addr!=null)
                connection_service.setGOaddress(addr);
            else
                return;

            if(!connection_service.createSocket())
                return;

            if(!connection_service.connectToGO() &&!isInterrupted()){
                //Client non connesso
                return;
            }else {
                //Connect andata a buon fine
            }

            //Controllo ogni volta se il thread è stato interrotto a causa di qualche errore nelle operazioni di rete
            if(!isInterrupted())
                connection_service.send_your_info(picture_uri);

            if(!isInterrupted())
                connection_service.read_opponent_info();

            if(!isInterrupted())
                connection_service.notify_handshake_completed();

            if(!isInterrupted())
                connection_service.wait_configuration();
    }

    private void ActAsServer(){
        //Controllo ogni volta se il thread è stato interrotto a causa di qualche errore nelle operazioni di rete

        if(connection_service.acceptDevice()) {
            if(!isInterrupted())
                connection_service.read_opponent_info();
            if(!isInterrupted())
                connection_service.send_your_info(picture_uri);
        }else{
            //Errore in acceptDevice
        }

        if(!isInterrupted())
            connection_service.notify_handshake_completed();

        if(!isInterrupted())
            connection_service.wait_user_input();

        if(!isInterrupted())
            connection_service.send_configuration();

    }
}
