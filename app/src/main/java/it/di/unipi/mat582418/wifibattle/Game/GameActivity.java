package it.di.unipi.mat582418.wifibattle.Game;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Random;

import it.di.unipi.mat582418.wifibattle.Database.MatchHistory;
import it.di.unipi.mat582418.wifibattle.R;
import it.di.unipi.mat582418.wifibattle.ConnectionService;


public class GameActivity extends Activity {
    private static ConnectionService connection_service;
    private ServiceConnection connection;

    //# navi piazzate
    int ships_placed;
    //#navi disponibili
    int ships_available;
    //riferimento al servizio è stato ottenuto, abilito l'utente
    boolean can_touch;

    //costanti per l'handler
    private static final int READY_TO_PLAY = 7;
    private static final int HIT = 8;
    private static final int MISS = 9;
    private static final int DEFEAT = 10;
    private static final int VICTORY = 11;
    private static final int ERROR = 12;

    private TableLayout my_table;
    private TableLayout opponent_table;
    private TextView text_info;
    private TextView turn_counter;
    private TextView score_table;
    private Button surrender;
    private Button ready;

    //Reperimento della posizione durante la partita
    private LocationManager lm;
    private LocationListener location_listener;

    //Riproduzione multimedia
    private MediaPlayer hit_sound;
    private MediaPlayer miss_sound;
    private MediaPlayer victory_sound;
    private MediaPlayer defeat_sound;
    private boolean hit_sound_ready,miss_sound_ready,victory_sound_ready,defeat_sound_ready;
    private boolean audio_enabled;

    private static GameInfo match_info;
    //Vibrazione in caso di nave colpita
    private Vibrator vibrator;

    /*Metodo che si occupa di cambiare la view durante il susseguirsi dei turni di gioco.*/
    public void changeView() {
        if (my_turn) { //Mio turno di attacco: mostro la tabella avversaria ed il tasto di resa
            my_table.setVisibility(View.INVISIBLE);
            opponent_table.setVisibility(View.VISIBLE);
            text_info.setText(getString(R.string.my_turn));
            surrender.setVisibility(View.VISIBLE);
        } else { //Attendo l'attacco nemico: mostro la mia tabella
            my_table.setVisibility(View.VISIBLE);
            opponent_table.setVisibility(View.INVISIBLE);
            text_info.setText(getString(R.string.his_turn));
        }

        //Aggiornamento dei punteggi
        score_table.setText(connection_service.getMy_points() + " vs " + connection_service.getOpponent_points());
        turn_counter.setText(getString(R.string.turn_num) + "" + connection_service.getTurn_counter());
        turn_counter.setVisibility(View.VISIBLE);
    }

    /*Booleano che indica se è già stata fatta una connessione al servizio.
    In caso positivo non sarà necessario avviare nuovamente i thread per gestire la connessione
    con l'interlocutore.*/
    private boolean first_instance = true;
    //Booleano che indica se tutte le navi sono state piazzate
    private boolean selection_completed = false;
    //Booleano che indice se è il mio turno di gioco
    private boolean my_turn = false;


    //Handler per la comunicazione activity-game thread
    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case READY_TO_PLAY: { //Le navi sono state piazzate
                    if (getIntent().getExtras().getString("Role").equals("Server")) {
                        my_turn = true;
                    } else {
                        my_turn = false;
                    }
                    changeView();
                    break;
                }
                case HIT: {
                    int slot = message.arg1;
                    int owner = message.arg2; //1-> casella nemica 0-> casella mia
                    if (owner == 1) {
                        my_turn = false;
                        ShipSlot sl = opponent_table.findViewWithTag(slot);
                        sl.setImageDrawable(getDrawable(R.drawable.killed));
                        sl.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    } else {
                        my_turn = true;
                        ShipSlot sl = my_table.findViewWithTag(slot);
                        sl.setImageDrawable(getDrawable(R.drawable.killed));
                        sl.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        if (vibrator.hasVibrator()) {
                            vibrator.vibrate(500);
                        }
                    }
                    if (audio_enabled && hit_sound_ready) {
                        if(hit_sound!=null){
                            if(hit_sound.isPlaying())
                                hit_sound.seekTo(0);
                            else
                                hit_sound.start();
                        }
                    }
                    Toast.makeText(getApplicationContext(), getString(R.string.hit), Toast.LENGTH_SHORT).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            changeView();
                        }
                    }, 2000);
                    break;
                }
                case MISS: {
                    int slot = message.arg1;
                    int owner = message.arg2; //1-> casella nemica 0-> casella mia
                    if (owner == 1) {
                        my_turn = false;
                        ShipSlot sl = opponent_table.findViewWithTag(slot);
                        sl.setImageDrawable(getDrawable(R.drawable.circle));
                        sl.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    } else {
                        my_turn = true;
                        ShipSlot sl = my_table.findViewWithTag(slot);
                        sl.setImageDrawable(getDrawable(R.drawable.circle));
                        sl.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    }
                    Toast.makeText(getApplicationContext(), getString(R.string.miss), Toast.LENGTH_SHORT).show();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            changeView();
                        }
                    }, 2000);

                    if (audio_enabled && miss_sound_ready) {
                        if(miss_sound!=null){
                            if(miss_sound.isPlaying())
                                miss_sound.seekTo(0);
                            else
                                miss_sound.start();
                        }
                    }

                    break;
                }
                case DEFEAT: {
                    EndGameFragment egf = new EndGameFragment();
                    egf.setCancelable(false);
                    Bundle bundle = new Bundle();
                    if (message.arg1 == 0) { //sconfitta per distruzione di tutte le navi
                        bundle.putString("Result", "Defeat");
                    }
                    if (message.arg1 == -1) { //sconfitta per resa
                        bundle.putString("Result", "MySurrender");
                    }
                    egf.setArguments(bundle);
                    egf.show(getFragmentManager(), EndGameFragment.TAG);
                    if (audio_enabled && defeat_sound_ready)
                        if(defeat_sound!=null)
                            defeat_sound.start();
                    break;
                }
                case VICTORY: {
                    EndGameFragment egf = new EndGameFragment();
                    egf.setCancelable(false);
                    Bundle bundle = new Bundle();
                    if (message.arg1 == 0) { //vittoria per distruzione di tutte le navi
                        bundle.putString("Result", "Victory");
                    }
                    if (message.arg1 == -1) { //vittoria per resa
                        bundle.putString("Result", "EnemySurrender");
                    }
                    egf.setArguments(bundle);
                    egf.show(getFragmentManager(), EndGameFragment.TAG);
                    if (audio_enabled && victory_sound_ready)
                        if(victory_sound!=null)
                            victory_sound.start();
                    break;
                }
                case ERROR: {
                    ErrorFragment ef = new ErrorFragment();
                    ef.setCancelable(false);
                    Bundle bundle = new Bundle();
                    //La ragione dell'errore viene passata al fragment per mostrarla a schermo
                    bundle.putString("Reason", (String) message.obj);
                    ef.setArguments(bundle);
                    ef.show(getFragmentManager(), ErrorFragment.TAG);
                    break;
                }
            }
            return false;
        }
    });

    public void ShowInstructionDialog() {
        //Dialog con istruzioni pre-partita
        GameDialogFragment gdf = new GameDialogFragment();
        gdf.setCancelable(false);
        Bundle bundle = new Bundle();
        bundle.putString("Why", "Start");
        /*motivo per cui è stato creato questo fragment: start-> dialog pre-partita,
         * limit-> limite di navi raggiunto*/
        bundle.putInt("ShipsNum", ships_available); //numero di navi disponibili che sarà mostrato nel messaggio
        gdf.setArguments(bundle);
        gdf.show(getFragmentManager(), GameDialogFragment.TAG);
        can_touch = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_table);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //Recupero le informazioni se presenti
        if(savedInstanceState!=null){
            my_turn=savedInstanceState.getBoolean("MyTurn");
            selection_completed=savedInstanceState.getBoolean("SelectionCompleted");
            first_instance=savedInstanceState.getBoolean("FirstInstance");
        }else{
            first_instance=true;
            my_turn=false;
            selection_completed=false;
        }

        ships_placed = 0;
        ships_available = 0;
        can_touch = false; //Inizialmente non è possibile toccare le caselle
        text_info = findViewById(R.id.text_info);
        turn_counter = findViewById(R.id.counter);
        score_table = findViewById(R.id.score_table);
        surrender = findViewById(R.id.surrender);
        surrender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //In caso di resa verrà inviato al nemico un intero di scelta -1
                connection_service.my_choice(-1);
            }
        });

        /*----------Gestione audio----------*/
        //Recupero la preferenza sull'audio
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("audio", false)) {
            audio_enabled = true;
        } else {
            audio_enabled = false;
        }
        hit_sound_ready=false;
        miss_sound_ready=false;
        victory_sound_ready=false;
        defeat_sound_ready=false;
        hit_sound=new MediaPlayer();
        miss_sound=new MediaPlayer();
        victory_sound=new MediaPlayer();
        defeat_sound=new MediaPlayer();
        MediaPlayer.OnCompletionListener restart=new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.seekTo(0);
            }
        };
        try{
            hit_sound.setDataSource(getResources().openRawResourceFd(R.raw.car_explosion));
            hit_sound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    hit_sound_ready=true;
                }
            });
            hit_sound.setOnCompletionListener(restart);
            miss_sound.setDataSource(getResources().openRawResourceFd(R.raw.sea_explosion));
            miss_sound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    miss_sound_ready=true;
                }
            });
            miss_sound.setOnCompletionListener(restart);
            victory_sound.setDataSource(getResources().openRawResourceFd(R.raw.victory));
            victory_sound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    victory_sound_ready=true;
                }
            });
            defeat_sound.setDataSource(getResources().openRawResourceFd(R.raw.defeat_audio));
            defeat_sound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    defeat_sound_ready=true;
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }
        hit_sound.prepareAsync();
        miss_sound.prepareAsync();
        victory_sound.prepareAsync();
        defeat_sound.prepareAsync();

        //Recupero riferimento al servizio
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                ConnectionService.LocalBinder binder = (ConnectionService.LocalBinder) service;
                connection_service = binder.getService();
                connection_service.setHandler_game(handler);

                GameInfo gi = null;
                if (first_instance) {
                    //Se si tratta di un primo avvio di partita recupero il numero di navi da piazzare
                    ships_available = connection_service.getNumber_of_ships();
                } else {
                    //Se non si tratta di un primo avvio di partita recupero le informazioni sulla partita sospesa
                    gi = connection_service.getFinalInfos();
                    ships_available = connection_service.getNumber_of_ships() - gi.my_ships_available.size();
                }

                text_info.setText(text_info.getText() + " " + ships_available);

                //Se il piazzamento delle navi non è stato completato allora mostra il dialog di istruzioni
                if (!selection_completed)
                    ShowInstructionDialog();

                lm = (LocationManager) connection_service.getApplicationContext().getSystemService(LOCATION_SERVICE);

                location_listener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        if (connection_service != null)
                            connection_service.updateBattleLocation(location);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        //Se il provider viene disabilitato
                        lm.removeUpdates(location_listener);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }
                };
                Criteria c = new Criteria();
                c.setAccuracy(Criteria.ACCURACY_FINE);
                String provider = lm.getBestProvider(c, true);
                if (lm.isProviderEnabled(provider)){
                    try {
                        lm.requestLocationUpdates(provider, 5000, 100, location_listener);
                    } catch (SecurityException se) {
                        se.printStackTrace();
                    }
                }

                if(!first_instance){
                    //Non si tratta di una prima connessione, recupero le informazioni di gioco
                    RestoreMatch(gi);
                }else{
                    //Segnalo che vi è stata già una connessione al servizio
                    first_instance=false;
                }

            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
            }
        };

        Intent intent = new Intent(this, ConnectionService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);
        BuildGameTable();
    }

    public void SaveTheGame() {
        if (connection_service == null) {
            //Fail
        } else {
            //Recupero le informazioni di fine partita
            match_info=connection_service.getFinalInfos();
            /*Procedo al salvataggio delle informazioni; due procedure disponibili in base al fatto che vi sia
            o meno un riferimento alla posizione*/
            MatchHistory db = new MatchHistory(connection_service.getApplicationContext(), "MatchHistory", null, 2);
            if(match_info.battle_location!=null) {
                db.addMatch(match_info, match_info.battle_location.getLatitude(), match_info.battle_location.getLongitude());
            }else {
                db.addMatch(match_info);
            }
            free_resources();
        }
    }

    public void free_resources(){
        if(connection_service!=null)
            connection_service.free_resources();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(lm!=null)
            lm.removeUpdates(location_listener);
    }

    @Override
    public void onBackPressed() {
        ExitFragment ef=new ExitFragment();
        ef.setCancelable(false);
        ef.show(getFragmentManager(),ExitFragment.TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hit_sound.release();
        miss_sound.release();
        defeat_sound.release();
        victory_sound.release();
        defeat_sound=null;
        victory_sound=null;
        hit_sound=null;
        miss_sound=null;

        /* Alla distruzione dell'attività procedo all'unbind del servizio */
        unbindService(connection);
    }

    private void RestoreMatch(GameInfo gi){
        //Ripristino le barche utilizzando le informazioni contenute nel service

        //Recupero le mie navi distrutte
        for(Integer i : gi.my_ships_available){
            ShipSlot ss=my_table.findViewWithTag(i.intValue());
            ss.setImageDrawable(null);
            ss.setImageDrawable(getDrawable(R.drawable.ship));
            ss.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ss.assign();
            if(selection_completed)
                ss.setClickable(false);
        }

        //Recupero le mie navi distrutte
        for(Integer i : gi.my_ships_destroyed){
            ShipSlot ss=my_table.findViewWithTag(i.intValue());
            ss.setImageDrawable(null);
            ss.setImageDrawable(getDrawable(R.drawable.killed));
            ss.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ss.assign();
            ss.setClickable(false);
        }

        //Recupero i colpi mancati dal nemico sul mio campo di battaglia
        for(Integer i : gi.enemy_missed){
            ShipSlot ss=my_table.findViewWithTag(i.intValue());
            ss.setImageDrawable(null);
            ss.setImageDrawable(getDrawable(R.drawable.circle));
            ss.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ss.assign();
            ss.setClickable(false);
        }

        //Recupero i miei colpi mancati sul campo di battaglia nemico
        for(Integer i : gi.missed){
            ShipSlot ss=opponent_table.findViewWithTag(i.intValue());
            ss.setImageDrawable(null);
            ss.setImageDrawable(getDrawable(R.drawable.circle));
            ss.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ss.assign();
            ss.setClickable(false);
        }

        //Recupero le navi distrutte sul campo di battaglia nemico
        for(Integer i : gi.hitted){
            ShipSlot ss=opponent_table.findViewWithTag(i.intValue());
            ss.setImageDrawable(null);
            ss.setImageDrawable(getDrawable(R.drawable.killed));
            ss.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ss.assign();
            ss.setClickable(false);
        }

        if(selection_completed){ //Le navi sono state già piazzate

            //Recupero le informazioni di gioco
            if (my_turn) {
                //Mio turno di attacco
                my_table.setVisibility(View.INVISIBLE);
                opponent_table.setVisibility(View.VISIBLE);
                text_info.setText(getString(R.string.my_turn));
                surrender.setVisibility(View.VISIBLE);
            } else { //attendo l'attacco nemico
                my_table.setVisibility(View.VISIBLE);
                opponent_table.setVisibility(View.INVISIBLE);
                text_info.setText(getString(R.string.his_turn));
            }

            score_table.setText(connection_service.getMy_points() + " vs " + connection_service.getOpponent_points());
            turn_counter.setText(getString(R.string.turn_num) + "" + connection_service.getTurn_counter());
            turn_counter.setVisibility(View.VISIBLE);
        }else{
            //Le navi non sono state ancora piazzate
            ships_available=connection_service.getNumber_of_ships();
            ships_placed=gi.my_ships_available.size();
            if(ships_available==ships_placed) {
                ready.setVisibility(View.VISIBLE);
            }
        }

    }

    private void BuildGameTable(){
        ready = findViewById(R.id.ships_placed);
        if(!selection_completed) { //Se la selezione non è già stata completata predispongo il bottone ready
            //Creazione del MIO campo di battaglia
            ready.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selection_completed = true;
                    if (getIntent().getExtras().getString("Role").equals("Server")) {
                        GameThread gameThread = new GameThread(connection_service, true);
                        gameThread.start();
                        ready.setVisibility(View.INVISIBLE);
                        my_turn = true;
                    } else {
                        GameThread gameThread = new GameThread(connection_service, false);
                        gameThread.start();
                        ready.setVisibility(View.INVISIBLE);
                        my_turn = false;
                    }

                }
            });
        }


        my_table = findViewById(R.id.my_table);
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            ShipSlot ib = new ShipSlot(this);
            ib.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Se non è stato ottenuto un riferimento al service oppure la fase di
                    // preparazione è conclusa non permettere nessuna operazione
                    if (!can_touch || selection_completed)
                        return;

                    ShipSlot clicked = (ShipSlot) view;
                    if (clicked.is_assigned()) {
                        //caso casella già assegnata, rimuovo la barca piazzata
                        if (random.nextInt() % 3 == 0) {
                            //dettaglio onde sul piano di gioco
                            ib.setImageDrawable(getDrawable(R.drawable.waves));
                            ib.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        }
                        else
                            ib.setImageDrawable(null);
                        clicked.free();
                        if (ships_placed == ships_available) {
                            ready.setVisibility(View.INVISIBLE);
                        }
                        ships_placed--;
                        //Aggiorno textview
                        text_info.setText(getString(R.string.ships_available) + " " + (ships_available - ships_placed));
                        //Aggiorno service
                        connection_service.removeMyShip((int) view.getTag());
                    } else {
                        //casella non assegnata
                        if (ships_placed == ships_available) {
                            //Se il limite è stato raggiunto non posso assegnare altre caselle
                            GameDialogFragment gdf = new GameDialogFragment();
                            gdf.setCancelable(false);
                            Bundle bundle = new Bundle();
                            bundle.putString("Why", "Limit");
                            gdf.setArguments(bundle);
                            gdf.show(getFragmentManager(), GameDialogFragment.TAG);
                        } else {
                            //Se il limite non è stato raggiunto assegno la casella cliccata
                            clicked.setImageDrawable(getDrawable(R.drawable.ship));
                            clicked.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            clicked.assign();
                            ships_placed++;
                            //Aggiorno textview
                            text_info.setText(getString(R.string.ships_available) + " " + (ships_available - ships_placed));
                            //Se sono arrivato al limite mostro il bottone "pronto"
                            if (ships_placed == ships_available) {
                                ready.setVisibility(View.VISIBLE);
                            }
                            //Aggiorno il service
                            connection_service.addMyShip((int) view.getTag());
                        }
                    }
                }
            });
            ib.setTag(i);
            ib.setBackground(getDrawable(R.drawable.my_sea_background));
            //dettaglio onde sul piano di gioco
            if (random.nextInt() % 3 == 0) {
                ib.setImageDrawable(getDrawable(R.drawable.waves));
                ib.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
            my_table.addView(ib);
        }

        opponent_table = findViewById(R.id.opponent_table);
        opponent_table.setVisibility(View.INVISIBLE);
        for (int i = 0; i < 100; i++) {
            ShipSlot ss = new ShipSlot(this);
            ss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!my_turn)
                        return;
                    int tag = (int) view.getTag();
                    surrender.setVisibility(View.INVISIBLE);
                    //Aggiorno il service sulla casella premuta, ciò risveglierà il GameThread
                    connection_service.my_choice(tag);
                    view.setClickable(false); //non faccio premere una casella già premuta
                }
            });
            ss.setTag(i);
            ss.setBackground(getDrawable(R.drawable.enemy_sea_background));
            //dettaglio onde sul piano di gioco
            if (random.nextInt() % 3 == 0) {
                ss.setImageDrawable(getDrawable(R.drawable.waves));
                ss.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
            opponent_table.addView(ss);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("SelectionCompleted",selection_completed);
        outState.putBoolean("MyTurn",my_turn);
        outState.putBoolean("FirstInstance",first_instance);
        super.onSaveInstanceState(outState);
    }
}
