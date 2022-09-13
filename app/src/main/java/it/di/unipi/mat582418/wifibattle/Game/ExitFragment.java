package it.di.unipi.mat582418.wifibattle.Game;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import it.di.unipi.mat582418.wifibattle.R;
import it.di.unipi.mat582418.wifibattle.WifiConnection.PreGameActivity;
import it.di.unipi.mat582418.wifibattle.WifiConnection.WaitingConnection;

//Dialog mostrato quando viene premuto il tasto back durante la PregameActivity o GameActivity
public class ExitFragment extends DialogFragment {
    public static String TAG = "ExitFragment";

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder b=new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.hey))
                .setNegativeButton(getString(R.string.no_exit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .setPositiveButton(getString(R.string.yes_exit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                        //Sto uscendo da GameActivity
                        if(getActivity() instanceof GameActivity){
                            GameActivity ga=(GameActivity) getActivity();
                            ga.free_resources();
                        }
                        //Sto uscendo da PregameActivity
                        if(getActivity() instanceof PreGameActivity){
                            PreGameActivity pga=(PreGameActivity) getActivity();
                            pga.free_resources();
                        }

                        WaitingConnection.closeconnection();
                        getActivity().finish();
                    }
                })
                .setIcon(R.mipmap.appicon)
                .setMessage(getString(R.string.no_back));
        return b.create();
    }
}
