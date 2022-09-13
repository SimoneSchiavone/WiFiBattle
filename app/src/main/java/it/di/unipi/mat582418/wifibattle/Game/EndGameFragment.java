package it.di.unipi.mat582418.wifibattle.Game;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import it.di.unipi.mat582418.wifibattle.MainActivity;
import it.di.unipi.mat582418.wifibattle.R;
import it.di.unipi.mat582418.wifibattle.WifiConnection.WaitingConnection;

//Dialog di fine partita: permette di scegliere se salvare o meno lo stato della partita nel database
public class EndGameFragment extends DialogFragment {
    public static String TAG = "EndGameFragment";

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b=new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.game_ended))
                .setPositiveButton(getString(R.string.exit_1), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Libero le risorse senza salvare la partita nel database; Ritorno al menù principale.
                        dismiss();
                        WaitingConnection.closeconnection();
                        GameActivity ga=(GameActivity) getActivity();
                        ga.free_resources();
                        startActivity(new Intent(getContext(),MainActivity.class));
                    }
                })
                .setNeutralButton(getString(R.string.exit_2), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Libero le risorse e salvo la partita nel database; Ritorno al menù principale.
                        dismiss();
                        GameActivity ga=(GameActivity) getActivity();
                        ga.SaveTheGame();;
                        WaitingConnection.closeconnection();
                        startActivity(new Intent(getContext(),MainActivity.class));
                    }
                })
                .setIcon(R.mipmap.appicon);
        if(getArguments().getString("Result").equals("Victory")){
            b.setMessage(getString(R.string.victory));
        }
        if(getArguments().get("Result").equals("MySurrender")){
            b.setMessage(getString(R.string.MySurrender));
        }
        if(getArguments().get("Result").equals("EnemySurrender")){
            b.setMessage(getString(R.string.EnemySurrender));
        }
        if(getArguments().get("Result").equals("Defeat")) {
            b.setMessage(getString(R.string.defeat));
        }
        return b.create();
    }

}
