package it.di.unipi.mat582418.wifibattle;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.io.IOException;
import it.di.unipi.mat582418.wifibattle.Database.HistoryActivity;
import it.di.unipi.mat582418.wifibattle.WifiConnection.WaitingConnection;

public class MainActivity extends Activity {
    //username da mostrare ed nome app
    private TextView username_tv, title;
    //foto profilo da mostrare
    private ImageView userpicture;
    //bottoni per le impostazioni e lo storico delle partite
    private ImageButton settings_button,game_history;

    //Menu inizio partita
    private ExtendedFloatingActionButton fab;
    private LinearLayout box1,box2;
    private boolean menu_visible;

    //Gestione WIFI
    private WifiManager wm;

    //Gestione audio di gioco
    private MediaPlayer sword_sound;
    private boolean sword_sound_ready;
    private MediaPlayer intro_sound;
    private Boolean audio_enabled;

    //Location manager per la gestione della posizione
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);

        //Intro di avvio activity
        intro_sound=new MediaPlayer();
        sword_sound=new MediaPlayer();
        try {
            intro_sound.setDataSource(getResources().openRawResourceFd(R.raw.intro));
            sword_sound.setDataSource(getResources().openRawResourceFd(R.raw.sword));
            intro_sound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            intro_sound.prepareAsync();
            sword_sound.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    sword_sound_ready=true;
                }
            });
            sword_sound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.seekTo(0);
                }
            });
            sword_sound.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Recupero elementi della view
        box1 = findViewById(R.id.join_layout);
        box2 = findViewById(R.id.create_layout);
        fab = findViewById(R.id.start);
        menu_visible = false;
        username_tv = findViewById(R.id.username);
        title = findViewById(R.id.appname);
        userpicture = findViewById(R.id.user_picture);
        settings_button = findViewById(R.id.settings_button);
        game_history=findViewById(R.id.gamehystory);

        //Listener per i click sui vari bottoni della schermata
        View.OnClickListener buttons_actions=new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(view==settings_button){
                    Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(i);
                }
                if(view==game_history){
                    Intent i = new Intent(getApplicationContext(), HistoryActivity.class);
                    startActivity(i);
                }
                if(view==fab){
                    PopupGameModes(view);
                }
            }
        };

        //Avvio attività che mostra i risultati memorizzati nel database
        game_history.setOnClickListener(buttons_actions);
        //Avvio attività che mostra le preferenze memorizzate
        settings_button.setOnClickListener(buttons_actions);
        //Avvio attività di gestione della connessione wifi_p2p
        fab.setOnClickListener(buttons_actions);

        wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        //Procedure per la gestione del messaggio di benvenuto
        WelcomeManagement();

        //Gestione delle animazioni sul titolo e sul bottone di avvio partita
        ManageAnimation(R.anim.fade_in, title);
        ManageAnimation(R.anim.bouncing, fab);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Verifica delle preferenze audio
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("audio",false)) {
            audio_enabled = true;
            if(intro_sound.isPlaying())
                intro_sound.seekTo(0);
            else
                intro_sound.start();
        }else {
            audio_enabled = false;
        }

        //Verifica della preferenza username
        username_tv.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("username", "NO"));

        //Verifica della preferenza sull'immagine di profilo
        String pic_uri = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null);
        if (pic_uri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(pic_uri));
                if(bitmap!=null){
                    userpicture.setImageBitmap(bitmap);
                    userpicture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                }
            } catch (Exception e) {
                //Se la procedura di recupero dell'immagine non va a buon fine metto la foto di default
                e.printStackTrace();
                userpicture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
            }
        } else { //caso preferenza cancellata
            userpicture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
        }

        ManageAnimation(R.anim.bouncing, fab);
    }

    //Metodo che applica l'animazione descritta dal campo id a tutte le views in someviews
    private void ManageAnimation(int id, View... someviews) {
        Animation a = AnimationUtils.loadAnimation(this, id);
        if (a != null) {
            for (View v : someviews) {
                v.startAnimation(a);
            }
        }
    }

    //Metodo per la gestione del messaggio di benvenuto e della foto di profilo in apertura di app
    private void WelcomeManagement() {
        //Gestione dell'username dell'utente da inserire la prima volta
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //Se non è mai stato impostato un username allora chiedi all'utente di impostarlo tramite dialog
        if (preferences.getString("username", null) == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.no_username), Toast.LENGTH_SHORT).show();
            UsernameDialogFragment udf = new UsernameDialogFragment();
            udf.setCancelable(false);
            udf.show(getFragmentManager(), UsernameDialogFragment.TAG);
        } else {
            username_tv.setText(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("username", "NO"));
        }

        //Verifica dei permessi
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //Caso permesso da richiedere all'utente
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }else{
            //Caso permesso già concesso:
            //Se è stata impostata una foto profilo in precedenza la mostro nell'apposito riquadro
            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null) != null) {
                Uri u = Uri.parse(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null));
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), u);
                    userpicture.setImageBitmap(bitmap);
                    userpicture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                } catch (Exception e) {
                    e.printStackTrace();
                    userpicture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
                }
            } else {
                userpicture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
            }
        }
    }


    //Metodo che si occupa di mostrare le opzioni di avvio partita quando viene premuto il floating action button
    public void PopupGameModes(View w) {
        //Controllo permessi
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(!menu_visible){
                OpenMenu();
            }else{
                CloseMenu();
            }
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE,Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_NETWORK_STATE,Manifest.permission.INTERNET,Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==0){ //permesso WiFi
            for(int i : grantResults){
                if(i != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, getString(R.string.permission_game), Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if(!menu_visible){
                OpenMenu();
            }else{
                CloseMenu();
            }
        }

        if(requestCode==1){ //permesso ReadExternalStorage
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, getString(R.string.permission_photo), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            //Se è stata impostata una foto profilo in precedenza la mostro nell'apposito riquadro
            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null) != null) {
                Uri u = Uri.parse(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("picture", null));
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), u);
                    userpicture.setImageBitmap(bitmap);
                    userpicture.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                } catch (IOException e) {
                    e.printStackTrace();
                    userpicture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
                }
            } else {
                userpicture.setImageDrawable(getDrawable(R.drawable.user_icon_foreground));
            }
        }
    }

    private void OpenMenu(){
        if(audio_enabled && sword_sound_ready) {
            if(sword_sound.isPlaying())
                sword_sound.seekTo(0);
            else
                sword_sound.start();
        }
        box1.setVisibility(View.VISIBLE);
        box2.setVisibility(View.VISIBLE);
        ManageAnimation(R.anim.fab_open,box1,box2);
        box1.setClickable(true);
        box2.setClickable(true);
        View.OnClickListener clicked=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view==box1 || view==box2){
                    //Accensione WIFI
                    if (!wm.isWifiEnabled()) {
                        //Chiediamo all'utente di accendere il WIFI
                        startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                        if(!wm.isWifiEnabled()) {
                            Toast.makeText(getApplicationContext(), getString(R.string.wifi_necessary), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    //Accensione della posizione
                    if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        Intent enable_location=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(enable_location);
                        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                            Toast.makeText(getApplicationContext(), getString(R.string.position_necessary), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    Intent i = new Intent(getApplicationContext(), WaitingConnection.class);
                    if(view==box1)
                        i.putExtra("Role","Wait");
                    if(view==box2)
                        i.putExtra("Role","Start");
                    CloseMenu();
                    startActivity(i);
                }
            }
        };
        box1.setOnClickListener(clicked);
        box2.setOnClickListener(clicked);
        menu_visible=true;
    }

    private void CloseMenu(){
        Animation a = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        Animation.AnimationListener end_listener=new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //Al termine dell'animazione rendere invisibili le view relative al menù
                box1.setVisibility(View.INVISIBLE);
                box2.setVisibility(View.INVISIBLE);
                box1.setClickable(false);
                box2.setClickable(false);
                menu_visible=false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        a.setAnimationListener(end_listener);
        box1.startAnimation(a);
        box2.startAnimation(a);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sword_sound.release();
        intro_sound.release();
        sword_sound=null;
        intro_sound=null;
    }
}



