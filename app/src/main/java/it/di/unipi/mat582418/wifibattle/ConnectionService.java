package it.di.unipi.mat582418.wifibattle;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import it.di.unipi.mat582418.wifibattle.Game.GameInfo;


public class ConnectionService extends Service {
    //Variabili per la comunicazione
    private static InetAddress GO_address=null;
    private static final int port=12345;
    private static final int connection_timeout=120000; //timeout 2min
    private static ServerSocket server_socket;
    private static Socket socket;

    private static DataInputStream dis;
    private static DataOutputStream dos;

    /* Handler per la comunicazione tra i thread per le operazioni di rete ed il
    thread UI + costanti usate per argomento dei messaggi*/
    private Handler handler_pregame;
    private Handler handler_game;
    private static final int SET_NAME=1;
    private static final int SET_IMAGE_CUSTOM=2;
    private static final int SET_IMAGE_DEFAULT=3;
    private static final int HANDSHAKE_COMPLETED=4;
    private static final int SHIPS_PLACEMENT=5;
    private static final int SET_MY_INFORMATIONS=6;
    private static final int READY_TO_PLAY=7;
    private static final int HIT=8;
    private static final int MISS=9;
    private static final int DEFEAT=10;
    private static final int VICTORY=11;
    private static final int ERROR=12;

    /*-----IMPOSTAZIONI INIZIALI-----*/
    private static int number_of_ships=0; //numero di navi della partita
    private static Object lock_settings=new Object(); //oggetto creato per la mutua esclusione sulle impostazioni

    /*-----STATISTICHE DI GIOCO-----*/
    private static ArrayList<Integer> my_ships_available=new ArrayList<Integer>(); //id delle caselle contenenti le mie navi disponibili
    private static ArrayList<Integer> my_ships_destroyed=new ArrayList<Integer>(); //id delle caselle contenenti le mie navi distrutte
    private static ArrayList<Integer> enemy_missed=new ArrayList<Integer>(); //id delle caselle mancate dal nemico
    private static ArrayList<Integer> missed=new ArrayList<Integer>(); //id delle caselle avversarie mancate
    private static ArrayList<Integer> hitted=new ArrayList<Integer>(); //id delle caselle avversarie colpite


    private static Object lock_results=new Object(); //mutua esclusione sui risultati di gioco
    private static int turn_counter=0;
    private static int my_points=0;
    private static int opponent_points=0;
    private static boolean i_surrender=false;
    private static boolean opponent_surrender=false;

    private static Location battle_location;

    //Riferimenti al nome e foto profilo dello sfidante
    public static String opponent_name=null;
    public static Bitmap opponent_pic=null;

    private boolean game_stopped=false;
    private static Object user_choice=new Object(); //oggetto per l'attesa dell'input utente
    private static int my_choice; //casella scelta dall'utente

    //Riferimento al servizio
    private final IBinder binder = new LocalBinder();

    //Classe usata dai client per prendere un riferimento al servizio
    public class LocalBinder extends Binder {
        public ConnectionService getService() {
            return ConnectionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //restituisco il binder
        return binder;
    }

    //-----METODI PUBBLICI CHIAMABILI DAI THREAD-----

    //Metodo chiamato per settare l'handler per l'activity Pregame
    public void setHandler_pregame(Handler h){
        handler_pregame =h;
    }

    //Metodo usato per impostare l'indirizzo del dispositivo che funge da group owner del WifiP2PGroup
    public void setGOaddress(InetAddress add){
        GO_address=add;
    }

    //Metodo per la creazione del ServerSocket quando il dispositivo è il server
    public boolean createServerSocket() {
        try{
            server_socket=new ServerSocket(port);
            server_socket.setSoTimeout(connection_timeout);
        }catch (IOException e){
            e.printStackTrace();
            handler_pregame.obtainMessage(ERROR,"createServerSocket").sendToTarget();
            return false;
        }
        return true;
    }

    //Metodo per la creazione del Socket quando il dispositivo è il client
    public boolean createSocket(){
        if(GO_address==null)
            return false;
        socket=new Socket();
        return true;
    }

    //Metodo per l'accettazione di un client da parte del server
    public boolean acceptDevice(){
        if(server_socket==null)
            return false;
        try{
            socket=server_socket.accept();
            dis=new DataInputStream(socket.getInputStream());
            dos=new DataOutputStream(socket.getOutputStream());
            game_stopped=false;
        }catch (SocketTimeoutException e){
            handler_pregame.obtainMessage(ERROR,"SocketTimeout in acceptDevice").sendToTarget();
            e.printStackTrace();
            return false;
        }catch (IOException e){
            e.printStackTrace();
            handler_pregame.obtainMessage(ERROR,"IOException in acceptDevice").sendToTarget();
            return false;
        }
        return true;
    }

    //Metodo per la connessione con il dispositivo server
    public boolean connectToGO(){
        if(socket==null)
            return false;
        try {
            socket.setSoTimeout(connection_timeout);
            socket.connect(new InetSocketAddress(GO_address, port),connection_timeout);
            dis=new DataInputStream(socket.getInputStream());
            dos=new DataOutputStream(socket.getOutputStream());
            game_stopped=false;
        }catch (IOException e){
            e.printStackTrace();
            handler_pregame.obtainMessage(ERROR,"IOException in ConnectToGO").sendToTarget();
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    //Metodo per la liberazione delle risorse utilizzate durante la partita
    public void free_resources(){
        if(server_socket!=null) {
            try {
                server_socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket!=null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(dis!=null) {
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(dos!=null) {
            try {
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        my_ships_available.clear();
        my_ships_destroyed.clear();
        missed.clear();
        hitted.clear();
        enemy_missed.clear();

        opponent_pic=null;
        opponent_name=null;
        GO_address=null;
        opponent_points=0;
        my_points=0;
        opponent_surrender=false;
        i_surrender=false;
        number_of_ships=0;
        turn_counter=0;
    }

    //Metodo utilizzato per l'invio all'interlocutore delle mie informazioni
    public void send_your_info(String uri){
        //Recupero nome e immagine di profilo dalle preferenze
        String name= PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("username",getString(R.string.anon));

        //Invio username
        write_string_to_stream(name,true);

        //Invio profile picture se presente
        if(uri  !=null){
            boolean error=false;

            Bitmap bit=null;
            try{
                bit= MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(uri));
            }catch (Exception e){
                e.printStackTrace();
                error=true;
            }

            if(error){ //Non inviare foto
                write_int_to_stream(0,true);
                return;
            }

            //Invio bit di presenza foto
            write_int_to_stream(1,true);

            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            byte[] byteArray;
            bit.compress(Bitmap.CompressFormat.JPEG,100,baos);
            byteArray=baos.toByteArray();

            //Invio della foto
            write_bytes_to_stream(byteArray,true);
        }else{
            write_int_to_stream(0,true);
        }
    }

    //Metodo utilizzato per la lettura delle informazioni dell'interlocutore
    public void read_opponent_info(){
        //Leggo l'username
        synchronized (lock_results) {
            opponent_name = read_string_from_stream(true);
        }
        if(Thread.currentThread().isInterrupted())
            return;

        //Invio il nome letto al thread UI (PregameActivity)
        handler_pregame.obtainMessage(SET_NAME, opponent_name).sendToTarget();

        //Controllo se c'è una foto
        int photo_available=read_int_from_stream(true);

        if(Thread.currentThread().isInterrupted())
            return;

        if(photo_available==1){ //Se c'è una foto disponibile mi predispongo per riceverla
            byte data[]=read_bytes_from_stream(true);
            if(Thread.currentThread().isInterrupted())
                return;

            Bitmap bm= BitmapFactory.decodeByteArray(data,0, data.length);

            opponent_pic=bm;
            //Comunico al thread UI di mostrare la foto profilo custom (PregameActivity)
            handler_pregame.obtainMessage(SET_IMAGE_CUSTOM).sendToTarget();
        }else{
            //Comunico al thread UI di mostrare la foto profilo di default (PregameActivity)
            handler_pregame.obtainMessage(SET_IMAGE_DEFAULT).sendToTarget();
        }
        //Comunico al thread UI di mostrare le mie informazioni
        handler_pregame.obtainMessage(SET_MY_INFORMATIONS).sendToTarget();
    }

    //Metodo per il recupero dell'immagine dello sfidante
    public Bitmap getOpponent_pic(){
        return opponent_pic;
    }

    //Metodo per il recupero del nome dello sfidante
    public String getOpponent_name(){
        return opponent_name;
    }

    /*Metodo per la notifica dell'avvenuto handshake tra i dispositivi; ora il dispositivo server
     può configurare il numero di navi della partita*/
    public void notify_handshake_completed(){
        handler_pregame.obtainMessage(HANDSHAKE_COMPLETED).sendToTarget();
    }

    //Metodo per settare il numero di navi della partita
    public void setShips(int no){
        synchronized (lock_settings){
            number_of_ships=no;
            //Risveglio il thread che sta aspettando il numero di navi dal dispositivo server
            lock_settings.notify();
        }
    }

    //Metodo per l'attesa della configurazione partita da parte del server
    public void wait_configuration(){
        if(Thread.currentThread().isInterrupted())
            return;

        this.setShips(read_int_from_stream(true)); //leggo il # di barche

        write_boolean_to_stream(true,true); //invio conferma

        if(Thread.currentThread().isInterrupted())
            return;

        //Notifico il completamento dell'handshake al Thread UI (PregameActivity)
        handler_pregame.obtainMessage(SHIPS_PLACEMENT).sendToTarget();

    }

    //Metodo per l'invio della configurazione partita al server
    public void send_configuration(){

        if(Thread.currentThread().isInterrupted())
            return;

        //Invio il numero di navi disponibili
        write_int_to_stream(number_of_ships,true);
        if(Thread.currentThread().isInterrupted())
            return;

        //Attendo una risposta
        boolean client_ready=read_boolean_from_stream(true);

        //Notifico il completamento dell'handshake al Thread UI (PregameActivity)
        handler_pregame.obtainMessage(SHIPS_PLACEMENT).sendToTarget();
    }

    //Metodo per attendere che l'utente selezioni il numero di navi dal NumberPicker
    public void wait_user_input(){
        //Il thread si ferma fin quando l'utente non imposta i dati iniziali
        synchronized (lock_settings){
            while(number_of_ships==0) {
                try {
                    lock_settings.wait();
                } catch (InterruptedException e) {
                    handler_pregame.obtainMessage(ERROR,"createServerSocket").sendToTarget();
                    e.printStackTrace();
                }
            }
        };
    }

    //Metodo per settare l'handler di comunicazione con l'activity GameActivity
    public void setHandler_game(Handler h){
        handler_game=h;
    }

    //Metodo per ottenere il numero di navi della partita
    public int getNumber_of_ships(){
        int x;
        synchronized (lock_settings){
            x=number_of_ships;
        }
        return x;
    }

    //Metodo per comunicare all'interlocutore di essere pronto a giocare
    public boolean ready(boolean server){
        if(server){
            write_boolean_to_stream(true,false);

            if(Thread.currentThread().isInterrupted())
                return false;

            if(read_boolean_from_stream(false)==true){
                if(Thread.currentThread().isInterrupted())
                    return false;

                handler_game.obtainMessage(READY_TO_PLAY).sendToTarget();
                return true;
            }
        }else{
            if(read_boolean_from_stream(false)==true) {
                if(Thread.currentThread().isInterrupted())
                    return false;

                write_boolean_to_stream(true,false);
                if(Thread.currentThread().isInterrupted())
                    return false;

                handler_game.obtainMessage(READY_TO_PLAY).sendToTarget();
                return true;
            }
        }
        return false;
    }

    public void updateBattleLocation(Location l){
        battle_location=l;
    }

    /*Metodo per inserire l'id della casella scelta dall'utente e risvegliare
    il thread di gioco che sta aspettando questa inforamzione*/
    public void my_choice(int i){
        synchronized (user_choice){
            my_choice=i;
            user_choice.notify();
        }

    }

    public void game_turn(boolean server){
        /*Turno di gioco:
        * -Il gamethread del server attende che l'utente server scelta una casella
        * -Il gamethread del server invia l'intero corrispondente al client
        * -Il gamethread del client attente l'intero dal server, risponde con un booleano
        * -Il server legge il booleano dal client ed aggiorna la propria tabella*/
        synchronized (lock_results) {
            turn_counter++;
        }

        if (server) {
            synchronized (user_choice) {
                //Aspetto che l'utente scelga un intero
                try {
                    user_choice.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    handler_game.obtainMessage(ERROR,"InterruptedException in game_turn").sendToTarget();
                }

                //Scrivo l'id della casella scelta
                write_int_to_stream(my_choice,false);
                if(Thread.currentThread().isInterrupted())
                    return;

                //attendo esito
                boolean hit = read_boolean_from_stream(false);
                if(Thread.currentThread().isInterrupted())
                    return;

                //Procedura di uscita in caso di resa (la resa corrisponde ad una scelta di casella con tag "-1")
                if(my_choice==-1){
                    synchronized (lock_results){
                        i_surrender=true;
                    }
                    game_stopped=true;
                    Message msg=handler_game.obtainMessage(DEFEAT);
                    msg.arg1=-1; //l'argomento -1 indica una sconfitta per mia resa
                    msg.sendToTarget();
                    return;
                }

                if (hit) { //Caso casella avversaria colpita (1 in arg2 indica che si tratta di una casella avversaria)
                    handler_game.obtainMessage(HIT, my_choice, 1).sendToTarget();
                    synchronized (lock_results){
                        hitted.add(my_choice);
                        my_points++;
                    }
                } else { //Caso casella avversaria mancata (1 in arg2 indica che si tratta di una casella avversaria)
                    handler_game.obtainMessage(MISS, my_choice, 1).sendToTarget();
                    synchronized (lock_results){
                        missed.add(my_choice);
                    }
                }

                //Attendo di sapere se lo sfidante ha perso tutte le sue navi
                boolean opponent_defeat = read_boolean_from_stream(false);
                if(Thread.currentThread().isInterrupted())
                    return;

                //Procedura di uscita in caso di vittoria
                if (opponent_defeat) {
                    game_stopped = true;
                    Message msg= handler_game.obtainMessage(VICTORY);
                    msg.arg1=0; //l'argomento 0 indica una vittoria per distruzione di tutte le navi nemiche
                    msg.sendToTarget();
                    return;
                }

                //aspetto l'intero da verificare dal client
                int received = read_int_from_stream(false);
                if(Thread.currentThread().isInterrupted())
                    return;

                    if(received==-1){
                        //resa avversaria
                        write_boolean_to_stream(false,false);
                        if(Thread.currentThread().isInterrupted())
                            return;
                        synchronized (lock_results){
                            opponent_surrender=true;
                        }
                        game_stopped=true;
                        Message msg=handler_game.obtainMessage(VICTORY);
                        msg.arg1=-1; //l'argomento -1 indica una vittoria per resa nemica
                        msg.sendToTarget();
                        return;
                    }

                    //Controllo se la casella ricevuta dall'avversario corrisponde ad una mia nave
                    if (my_ships_available.contains(Integer.valueOf(received))) {
                        write_boolean_to_stream(true,false);
                        if(Thread.currentThread().isInterrupted())
                            return;

                        //0 in arg2 indica che si tratta di una mia casella
                        handler_game.obtainMessage(HIT, received, 0).sendToTarget();
                        synchronized (lock_results){
                            my_ships_available.remove(Integer.valueOf(received));
                            my_ships_destroyed.add(received);
                            opponent_points++;
                        }
                    } else {
                        write_boolean_to_stream(false,false);
                        if(Thread.currentThread().isInterrupted())
                            return;

                        synchronized (lock_results){
                            enemy_missed.add(received);
                        }

                        //0 in arg2 indica che si tratta di una mia casella
                        handler_game.obtainMessage(MISS, received, 0).sendToTarget();
                    }

                    //Se non ho più navi disponibili invio sconfitta
                    if (my_ships_available.size() == 0) {
                        write_boolean_to_stream(true,false);
                    } else {
                        write_boolean_to_stream(false,false);
                    }
                    if(Thread.currentThread().isInterrupted())
                        return;

                    //procedura di uscita in caso di sconfitta
                    if (my_ships_available.size() == 0) {
                        game_stopped = true;
                        Message msg=handler_game.obtainMessage(DEFEAT);
                        msg.arg1=0; //l'argomento 0 indica una sconfitta per distruzione di tutte le mie navi
                        msg.sendToTarget();
                        return;
                    }
            }
        } else {
            /*Turno di gioco:
                * -Il gamethread del client attente l'intero dal server, risponde con un booleano
                * -Il server legge il booleano dal client ed aggiorna la propria tabella
                * -Il gamethread del client attende che l'utente client scelga una casella
                * -Il gamethread del client invia l'intero corrispondente al client
                * */
            synchronized (user_choice) {
                //aspetto l'intero da verificare dal server
                int received = read_int_from_stream(false);

                if(received<-1){ //Errore nella read
                    Thread.currentThread().interrupt();
                    return;
                }

                    if(received==-1){ //resa avversaria
                        write_boolean_to_stream(false,false);
                        if(Thread.currentThread().isInterrupted())
                            return;
                        synchronized (lock_results){
                            opponent_surrender=true;
                        }
                        game_stopped=true;
                        Message msg=handler_game.obtainMessage(VICTORY);
                        msg.arg1=-1;
                        msg.sendToTarget();
                        return;
                    }

                    //Controllo se la casella ricevuta dall'avversario contiene una mia nave (0)
                    if (my_ships_available.contains(Integer.valueOf(received))) {
                        write_boolean_to_stream(true,false);
                        handler_game.obtainMessage(HIT, received, 0).sendToTarget();
                        synchronized (lock_results){
                            my_ships_available.remove(Integer.valueOf(received));
                            my_ships_destroyed.add(received);
                            opponent_points++;
                        }
                    } else {
                        write_boolean_to_stream(false,false);
                        handler_game.obtainMessage(MISS, received, 0).sendToTarget();
                        synchronized (lock_results){
                            enemy_missed.add(received);
                        }
                    }
                    if(Thread.currentThread().isInterrupted())
                        return;

                    //Se non ho più navi disponibili invio sconfitta
                    if (my_ships_available.size() == 0) {
                        write_boolean_to_stream(true,false);
                    } else {
                        write_boolean_to_stream(false,false);
                    }
                    if(Thread.currentThread().isInterrupted())
                        return;

                    //procedura di uscita in caso di sconfitta
                    if (my_ships_available.size() == 0) {
                        game_stopped = true;
                        Message msg=handler_game.obtainMessage(DEFEAT);
                        msg.arg1=0;
                        msg.sendToTarget();
                        return;
                    }


                //Aspetto che l'utente scelga un intero
                try {
                    user_choice.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    handler_game.obtainMessage(ERROR,"InterruptedException in GameTurn").sendToTarget();
                }

                write_int_to_stream(my_choice,false); //invio casella scelta
                if(Thread.currentThread().isInterrupted())
                    return;

                boolean hit = read_boolean_from_stream(false); //attendo esito
                if(Thread.currentThread().isInterrupted())
                    return;

                //Procedura di uscita in caso di resa
                if(my_choice==-1){
                    synchronized (lock_results){
                        i_surrender=true;
                    }
                    game_stopped=true;
                    Message msg=handler_game.obtainMessage(DEFEAT);
                    msg.arg1=-1;
                    msg.sendToTarget();
                    return;
                }

                if (hit) { //Caso casella avversaria colpita (1)
                    handler_game.obtainMessage(HIT, my_choice, 1).sendToTarget();
                    synchronized (lock_results){
                        hitted.add(my_choice);
                        my_points++;
                    }
                } else { //Caso casella aversaria mancata (1)
                    handler_game.obtainMessage(MISS, my_choice, 1).sendToTarget();
                    synchronized (lock_results) {
                        missed.add(my_choice);
                    }
                }

                boolean opponent_defeat = read_boolean_from_stream(false);
                if(Thread.currentThread().isInterrupted())
                    return;

                //Procedura di uscita in caso di vittoria
                if (opponent_defeat) {
                    game_stopped = true;
                    Message msg= handler_game.obtainMessage(VICTORY);
                    msg.arg1=0;
                    msg.sendToTarget();
                    return;
                }
            }
        }
    }

    public void addMyShip(int a){
        synchronized (lock_results) {
            my_ships_available.add(a);
        }
    }

    public void removeMyShip(int a){
        synchronized (lock_results) {
            my_ships_available.remove(Integer.valueOf(a));
        }
    }

    public int getTurn_counter(){
        int x;
        synchronized (lock_results){
            x=turn_counter;
        }
        return x;
    }

    public int getMy_points(){
        int x;
        synchronized (lock_results){
            x=my_points;
        }
        return x;
    }

    public int getOpponent_points(){
        int x;
        synchronized (lock_results){
            x=opponent_points;
        }
        return x;
    }

    public boolean isGameStopped(){
        return game_stopped;
    }

    public GameInfo getFinalInfos(){
        GameInfo infos=new GameInfo();
        synchronized (lock_results){
            infos.my_points = my_points;
            infos.opponent_points = opponent_points;
            infos.opponent_name = opponent_name;
            infos.played_turns = turn_counter;
            infos.my_ships_available = my_ships_available;
            infos.my_ships_destroyed = my_ships_destroyed;
            infos.hitted = hitted;
            infos.missed = missed;
            infos.enemy_missed=enemy_missed;
            infos.i_surrender=i_surrender;
            infos.opponent_surrender=opponent_surrender;
            infos.battle_location=battle_location;
        }
        return infos;
    }

    /*----------METODI DI SUPPORTO----------*/
    private void write_string_to_stream(String msg,boolean pregame){
        try {
            dos.writeUTF(msg);
        }catch (IOException e){
            if(!pregame)
                handler_game.obtainMessage(ERROR,"IOException in write_string_to_stream").sendToTarget();
            else
                handler_pregame.obtainMessage(ERROR,"IOException in write_string_to_stream").sendToTarget();
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private void write_bytes_to_stream(byte[] msg,boolean pregame){
        try {
            dos.writeInt(msg.length);
            dos.write(msg,0,msg.length);
        }catch (IOException e){
            if(!pregame)
                handler_game.obtainMessage(ERROR,"IOException in write_bytes_to_stream").sendToTarget();
            else
                handler_pregame.obtainMessage(ERROR,"IOException in write_bytes_to_stream").sendToTarget();
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private void write_int_to_stream(int msg,boolean pregame){
        try {
            dos.writeInt(msg);
        }catch (IOException e){
            if(!pregame)
                handler_game.obtainMessage(ERROR,"IOException in write_int_to_stream").sendToTarget();
            else
                handler_pregame.obtainMessage(ERROR,"IOException in write_int_to_stream").sendToTarget();
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private int read_int_from_stream(boolean pregame){
        int read=Integer.MIN_VALUE;
        try {
            read=dis.readInt();
        }catch (IOException e){
            if(!pregame)
                handler_game.obtainMessage(ERROR,"IOException in read_int_from_stream").sendToTarget();
            else
                handler_pregame.obtainMessage(ERROR,"IOException in read_int_from_stream").sendToTarget();
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return read;
    }

    private boolean read_boolean_from_stream(boolean pregame){
        boolean read=false;
        try {
            read=dis.readBoolean();
        }catch (IOException e){
            if(!pregame)
                handler_game.obtainMessage(ERROR,"IOException in read_boolean_from_stream").sendToTarget();
            else
                handler_pregame.obtainMessage(ERROR,"IOException in read_boolean_from_stream").sendToTarget();
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return read;
    }

    private void write_boolean_to_stream(boolean msg,boolean pregame){
        try {
            dos.writeBoolean(msg);
        }catch (IOException e){
            if(!pregame)
                handler_game.obtainMessage(ERROR,"IOException in write_boolean_to_stream").sendToTarget();
            else
                handler_pregame.obtainMessage(ERROR,"IOException in write_boolean_to_stream").sendToTarget();
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private String read_string_from_stream(boolean pregame){
        String read_string=null;
        try {
            read_string=dis.readUTF();
        }catch (IOException e){
            if(!pregame)
                handler_game.obtainMessage(ERROR,"IOException in read_string_from_stream").sendToTarget();
            else
                handler_pregame.obtainMessage(ERROR,"IOException in read_string_from_stream").sendToTarget();
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return read_string;
    }

    private byte[] read_bytes_from_stream(boolean pregame){
        int length=read_int_from_stream(pregame);
        if(length>0){
            byte data[]=new byte[length];
            try {
                dis.readFully(data,0,length);
                return data;
            } catch (IOException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                if(!pregame)
                    handler_game.obtainMessage(ERROR,"IOException in read_bytes_from_stream").sendToTarget();
                else
                    handler_pregame.obtainMessage(ERROR,"IOException in read_bytes_from_stream").sendToTarget();
            }
        }
        return null;
    }
}
