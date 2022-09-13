package it.di.unipi.mat582418.wifibattle.WifiConnection;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.net.InetAddress;

import it.di.unipi.mat582418.wifibattle.ConnectionService;
import it.di.unipi.mat582418.wifibattle.Game.ErrorFragment;
import it.di.unipi.mat582418.wifibattle.Game.ExitFragment;
import it.di.unipi.mat582418.wifibattle.Game.GameActivity;
import it.di.unipi.mat582418.wifibattle.R;

public class PreGameActivity extends Activity {
    //Elementi della view
    private TextView opponent_name;
    private ImageView opponent_pic;
    private ProgressBar pb;
    private Bitmap opponent_bitmap;

    //Constanti per l'handler
    private static final int SET_NAME=1;
    private static final int SET_IMAGE_CUSTOM=2;
    private static final int SET_IMAGE_DEFAULT=3;
    private static final int HANDSHAKE_COMPLETED=4;
    private static final int SHIPS_PLACEMENT=5;
    private static final int SET_MY_INFORMATIONS=6;
    private static final int ERROR=12;

    //Flag per segnalare il completamento delle fasi del protocollo di comunicazione
    private boolean handshake_completed=false; //Nome e foto scambiate
    private boolean error_received=false;

    //Handler per la comunicazione HandshakeThread - ThreadUI
    private final Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case SET_NAME: //Imposta il nome dell'avversario
                    opponent_name.setText((String) message.obj);
                    pb.setProgress(10,true);
                    break;
                case SET_IMAGE_CUSTOM: //Imposta l'immagine dell'avversario (personalizzata)
                    opponent_bitmap=connection_service.getOpponent_pic();
                    if (opponent_bitmap != null) {
                        opponent_pic.setImageBitmap(opponent_bitmap);
                        opponent_pic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    }
                    pb.setProgress(50,true);
                    break;
                case SET_IMAGE_DEFAULT: //Imposta l'immagine dell'avversario (default)
                    opponent_pic.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
                    opponent_pic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    pb.setProgress(50,true);
                    break;
                case SET_MY_INFORMATIONS: //Imposta le mie informazioni
                    TextView my_name=findViewById(R.id.your_name);
                    //Recupero il mio username dalle preferenze
                    my_name.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("username", getString(R.string.anon)));
                    pb.setProgress(60,true);

                    //Recupero la mia immagine di profilo se presente
                    ImageView my_picture=findViewById(R.id.your_picture);
                    String pic_uri = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null);
                    if (pic_uri != null) {
                        Uri u = Uri.parse(pic_uri);
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), u);
                            my_picture.setImageBitmap(bitmap);
                            my_picture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        } catch (Exception e) {
                            e.printStackTrace();
                            my_picture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
                        }
                    }else{
                        my_picture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
                    }
                    break;
                case HANDSHAKE_COMPLETED:
                    //Handshake completato -> mostra al dispositivo server il pannello di configurazione partita
                    handshake_completed=true;
                    pb.setProgress(100,true);
                    ShowConfigPanel();
                    break;
                case SHIPS_PLACEMENT:
                    //Completata la configurazione della partita, avvia la partita quindi il posizionamento delle navi
                    if(error_received)
                        break;
                    Intent i=new Intent(getApplicationContext(),GameActivity.class);
                    i.putExtras(getIntent().getExtras());
                    startActivity(i);
                    break;
                case ERROR:
                    //Una operazione del protocollo di comunicazione è fallita, mostro il dialog di errore
                    error_received=true;
                    ErrorFragment ef=new ErrorFragment();
                    ef.setCancelable(false);
                    Bundle bundle=new Bundle();
                    //La ragione dell'errore è mostrata nel dialog
                    bundle.putString("Reason",(String)message.obj);
                    ef.setArguments(bundle);
                    ef.show(getFragmentManager(),ErrorFragment.TAG);
                    break;
            }
            return false;
        }
    });

    //----------Service per la connessione----------

    //Riferimento al service
    private ConnectionService connection_service;
    private ServiceConnection connection;

    /*Booleano che indica se è già stata fatta una connessione al servizio.
    In caso positivo non sarà necessario avviare nuovamente i thread per gestire la connessione
    con l'interlocutore.*/
    public boolean first_connection=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pregame_activity);

        //Dettagli dello sfidante
        opponent_name=findViewById(R.id.opponent_name);
        opponent_pic=findViewById(R.id.opponent_picture);

        pb=findViewById(R.id.progressBar2);

        //Ripristino dello stato precedente se disponibile
        if(savedInstanceState!=null){ //Ripristino
            handshake_completed=savedInstanceState.getBoolean("HandshakeCompleted");
            first_connection=savedInstanceState.getBoolean("FirstConnection");

            //Se non si tratta di una prima connessione, ripristina il mio nome nome e la mia foto profilo
            if(!first_connection){
                TextView my_name=findViewById(R.id.your_name);
                my_name.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("username", getString(R.string.anon)));

                ImageView my_picture=findViewById(R.id.your_picture);
                String pic_uri = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null);
                if (pic_uri != null) {
                    Uri u = Uri.parse(pic_uri);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), u);
                        my_picture.setImageBitmap(bitmap);
                        my_picture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        my_picture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
                    }
                }else{
                    my_picture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
                }
            }
        }

        //Connetto l'activity al service
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                ConnectionService.LocalBinder binder = (ConnectionService.LocalBinder) service;
                connection_service = binder.getService();
                connection_service.setHandler_pregame(handler);

                if(first_connection) { //Avvio i thread necessari a gestire la connessione
                    first_connection=false;
                    if(getIntent().getExtras().getString("Role").equals("Server")) {
                        //Faccio partire un thread che si comporterà da server
                        HandshakeThread ht=new HandshakeThread(connection_service,PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null),true,null);
                        ht.start();
                    }
                    if(getIntent().getExtras().getString("Role").equals("Client")){
                        /*Faccio partire un thread che si comporterà da client. Passerò al thread un riferimento all'
                        indirizzo del GroupOwner necessario per la creazione del socket di rete*/
                        InetAddress a=(InetAddress) getIntent().getExtras().get("Address");
                        HandshakeThread ht=new HandshakeThread(connection_service,PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null),false,a);
                        ht.start();
                    }
                }else{
                    //Aggiorno l'UI con le informazioni già ottenute dalla connessione
                    if(connection_service.getOpponent_pic()!=null) {
                        //Recupero foto dello sfidante
                        opponent_pic.setImageBitmap(connection_service.getOpponent_pic());
                        opponent_pic.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    }
                    if(connection_service.getOpponent_name()!=null){
                        //Recupero nome dello sfidante
                        opponent_name.setText(connection_service.getOpponent_name());
                    }
                    if(handshake_completed){
                        //Se l'handshake era già stato completato mostro il pannello di configurazione
                        ShowConfigPanel();
                    }
                }
            }

                @Override
                public void onServiceDisconnected(ComponentName arg0) {
                }
            };
        Intent intent = new Intent(this, ConnectionService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    @Override
    public void onBackPressed() {
        /*Alla pressione del tasto back mostriamo un dialog che chiede conferma all'utente.
         * Tornando indietro la partita verrà arrestata.*/
        ExitFragment ef=new ExitFragment();
        ef.setCancelable(false);
        ef.show(getFragmentManager(),ExitFragment.TAG);
    }

    private void ShowConfigPanel(){
        /*Configurazione di partita (Tale porzione di view sarà disponibile solo
        al dispositivo server dopo l'avvenuto handshake)*/
        if(getIntent().getExtras().get("Role").equals("Server")){
            RelativeLayout settings_layout=findViewById(R.id.settings);
            settings_layout.setVisibility(View.VISIBLE);
            Button start=findViewById(R.id.send_configuration);
            start.setVisibility(View.VISIBLE);

            //Numero di navi della partita
            NumberPicker np=findViewById(R.id.number_of_ships);
            np.setMinValue(1);
            np.setMaxValue(50);
            np.setValue(10);

            //Bottone per l'avvio della partita
            Button s=findViewById(R.id.send_configuration);
            s.setVisibility(View.VISIBLE);
            s.setOnClickListener(new View.OnClickListener() { //Premuto il tasto PRONTO
                @Override
                public void onClick(View view) {
                    //Notifico il service della scelta dell'utente
                    connection_service.setShips(np.getValue());
                }
            });
        }
    }

    public void free_resources(){
        if(connection_service!=null) //Liberazione delle risorse del service
            connection_service.free_resources();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        //Salvataggio dello stato
        outState.putBoolean("FirstConnection",first_connection);
        outState.putBoolean("HandshakeCompleted",handshake_completed);
        super.onSaveInstanceState(outState);
    }
}
