package it.di.unipi.mat582418.wifibattle.WifiConnection;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.util.ArrayList;
import it.di.unipi.mat582418.wifibattle.R;


public class WaitingConnection extends Activity {
    /*Ruolo del dispositivo:
        Wait--> Visualizza i dispositivi disponibili ed attendi che qualcuno chieda di connettersi
        Start--> Visualizza i dispositivi disponibili e scegli tra essi uno a cui richiedere una connessione
     */
    private String role;

    //Progress bar che rappresenta l'attività di ricerca
    private ProgressBar pb;
    //Lista dei dispositivi
    private ListView listView;
    //adapter per il WifiP2PDevice
    private WifiP2PAdapter adapter;

    /*----------WifiP2P----------*/
    //Lista di dispositivi disponibili
    private ArrayList<WifiP2pDevice> devices_list;
    //onItemClickListener per avviare la connessione con il dispositivo cliccato
    private AdapterView.OnItemClickListener device_clicked;
    //WifiP2PManager
    private static WifiP2pManager manager;
    private static WifiP2pManager.Channel ch;
    //Listener chiamato dal sistema quando è disponibile una lista di peer disponibili
    private WifiP2pManager.PeerListListener peerListListener;
    //Listener chiamato dal sistema quando sono disponibili le informazioni circa una connessione
    private WifiP2pManager.ConnectionInfoListener connInfoListener;
    //Indirizzo del group owner del WifiP2PGroup
    private InetAddress GO_address;

    //Intent filter per il broadcast receiver che intercetta gli eventi relativi il WiFiP2P
    private IntentFilter intentFilter;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        role = getIntent().getExtras().getString("Role");
        if (role == null) //abort
            finish();

        //Recupero elementi della View
        setContentView(R.layout.connection_activity);
        pb = findViewById(R.id.progressBar);

        //Modifica alla schermata in base al ruolo del dispositivo
        if (role.equals("Start")) {
            TextView pippo = (TextView) findViewById(R.id.conn_waiting);
            pippo.setText(getString(R.string.click_device));
        }
        if (role.equals("Wait")) {
            TextView pippo = (TextView) findViewById(R.id.conn_waiting);
            pippo.setText(getString(R.string.waiting_connection));
        }

        //Gestione lista dei dispositivi disponibili
        listView = (ListView) findViewById(R.id.devices_list);
        devices_list = new ArrayList<WifiP2pDevice>();
        adapter = new WifiP2PAdapter(this, devices_list);
        listView.setAdapter(adapter);


        //Intercettiamo gli intent broadcast relativi a variazioni dello stato del WIFI
        intentFilter = new IntentFilter();
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //Inizializzazione del WifiP2PManager
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        ch = manager.initialize(this, getMainLooper(), null);

        //Listener chiamato quando è disponibile una nuova lista di peer
        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                if (!peerList.equals(devices_list)) {
                    devices_list.clear();
                    devices_list.addAll(peerList.getDeviceList());

                    //Notifichiamo l'adapter che i dati sono cambiati, in modo da aggiornare la lista
                    adapter.notifyDataSetChanged();
                }
            }
        };

        //Listener chiamato quando sono disponibili nuove informazioni sulla connessione
        connInfoListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                //Memorizzo l'indirizzo del proprietario del gruppo WifiP2P
                GO_address = wifiP2pInfo.groupOwnerAddress;

                if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) { //Caso in cui sono il Group Owner
                    Intent i = new Intent(getApplicationContext(), PreGameActivity.class);
                    i.putExtra("Role", "Server");
                    startActivity(i);
                } else if (wifiP2pInfo.groupFormed) { //Caso in cui non sono il Group Owner
                    Intent i = new Intent(getApplicationContext(), PreGameActivity.class);
                    i.putExtra("Role", "Client");
                    i.putExtra("Address", GO_address);
                    startActivity(i);
                }
            }
        };

        /*Gestione del tocco su un dispositivo disponibile dalla lista (disponibile solo se il dispositivo
        ha come ruolo "start"*/
        device_clicked = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Recupero il device cliccato
                WifiP2pDevice clicked = (WifiP2pDevice) adapterView.getItemAtPosition(i);

                //Nuova config in cui specifichiamo l'indirizzo del device a cui connetterci
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = clicked.deviceAddress;

                try {
                    //Operazione di connessione. Il listener gestirà il fatto che la connessione andrà
                    //a buon fine o meno.
                    manager.connect(ch, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(),getString(R.string.connection_ok)+" "+clicked.deviceName+"!",Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(int i) {
                            Toast.makeText(getApplicationContext(),getString(R.string.connection_failed),Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (SecurityException se) {
                    se.printStackTrace();
                }
            }
        };
        if (role.equals("Start")) //Solo se posso avviare una connessione permetto il click su un dispositivo della lista
            listView.setOnItemClickListener(device_clicked);

        //BroadcastReceiver per gli eventi relativi al WIFI_P2P
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    //Determino se la modalità Wifi P2P è attiva o meno
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) { //Caso ok
                    } else { //Caso non abilitato
                        Toast.makeText(context, getString(R.string.wifi_off), Toast.LENGTH_SHORT).show();
                        if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("audio",false)){
                            MediaPlayer error=MediaPlayer.create(getApplicationContext(),R.raw.error_sound);
                            error.start();
                        }
                        finish();
                    }
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    //Cambiamento nella lista dei peer disponibili
                    try {
                        //Richiedo la nuova lista dei peer
                        manager.requestPeers(ch, peerListListener);
                    } catch (SecurityException se) {
                        se.printStackTrace();
                    }
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    //Gestione nuove connessioni e disconnessioni

                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (networkInfo.isConnected()) { //Caso connesso
                        //Rendo invisibile la progress bar della ricerca
                        pb.setVisibility(ViewGroup.INVISIBLE);
                        //Richiedo le informazioni relative alla connessione
                        manager.requestConnectionInfo(ch, connInfoListener);
                    }
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    //I dettagli del nostro dispositivo sono cambiati
                }
            }
        };

        Button refresh = findViewById(R.id.refresh_peers);
        try {
            //Richiesta di ricerca di nuovi peer
            manager.discoverPeers(ch, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() { //Procedura avviata con successo
                    pb.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), getString(R.string.discover_success), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(int reasonCode) { //Procedura fallita
                    Toast.makeText(getApplicationContext(), getString(R.string.discover_failed), Toast.LENGTH_LONG).show();
                    if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("audio",false)){
                        MediaPlayer error=MediaPlayer.create(getApplicationContext(),R.raw.error_sound);
                        error.start();
                    }
                    finish();
                }
            });
        } catch (SecurityException se) {
            se.printStackTrace();
        }

        //Bottone refresh lista peers
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    manager.discoverPeers(ch, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() { //Procedura avviata con successo
                            Toast.makeText(getApplicationContext(), getString(R.string.discover_success), Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(int reasonCode) { //Procedura fallita
                            Toast.makeText(getApplicationContext(), getString(R.string.discover_failed), Toast.LENGTH_LONG).show();
                            if(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("audio",false)){
                                MediaPlayer error=MediaPlayer.create(getApplicationContext(),R.raw.error_sound);
                                error.start();
                            }
                            finish();
                        }
                    });
                }catch (SecurityException se){
                    se.printStackTrace();
                    finish();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        //De-registro il receiver quando l'activity non è più visibile
        unregisterReceiver(broadcastReceiver);
    }

    public static void closeconnection(){
        if(ch!=null){
            manager.removeGroup(ch, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int i) {

                }
            });
        }
    }

}
