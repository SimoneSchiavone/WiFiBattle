package it.di.unipi.mat582418.wifibattle.Game;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import java.io.IOException;
import it.di.unipi.mat582418.wifibattle.MainActivity;
import it.di.unipi.mat582418.wifibattle.R;
import it.di.unipi.mat582418.wifibattle.WifiConnection.PreGameActivity;
import it.di.unipi.mat582418.wifibattle.WifiConnection.WaitingConnection;

//Dialog di errore: mostra la causa dell'errore e permette una uscita sicura dalla partita
public class ErrorFragment extends DialogFragment {
    public static String TAG = "ErrorFragment";

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("audio",false)){
            MediaPlayer error=new MediaPlayer();//MediaPlayer.create(getContext(),R.raw.error_sound);
            try {
                error.setDataSource(getResources().openRawResourceFd(R.raw.error_sound));
            } catch (IOException e) {
                e.printStackTrace();
            }
            error.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            mediaPlayer.release();
                        }
                    });
                }
            });
            error.prepareAsync();
        }
        AlertDialog.Builder b=new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.game_ended))
                .setNeutralButton(getString(R.string.exit_1), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                        //Sto uscendo da GameActivity
                        if(getActivity() instanceof GameActivity) {
                            GameActivity ga = (GameActivity) getActivity();
                            ga.free_resources();
                        }
                        //Sto uscendo da PregameActivity
                        if(getActivity() instanceof PreGameActivity){
                            PreGameActivity pga=(PreGameActivity) getActivity();
                            pga.free_resources();
                        }

                        WaitingConnection.closeconnection();
                        getActivity().finish();
                        startActivity(new Intent(getContext(),MainActivity.class));
                    }
                })
                .setIcon(R.mipmap.appicon)
                .setMessage(getString(R.string.ConnectionError)+"\n"+getArguments().getString("Reason",""));
        return b.create();
    }
}
