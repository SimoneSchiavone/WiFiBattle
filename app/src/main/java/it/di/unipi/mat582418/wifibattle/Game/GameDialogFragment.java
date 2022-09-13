package it.di.unipi.mat582418.wifibattle.Game;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import it.di.unipi.mat582418.wifibattle.R;

/*DialogFragrment che informa l'utente sul numero di navi da posizionare sul campo di battaglia
 * oppure sulla terminazione delle navi disponibili da piazzare*/
public class GameDialogFragment extends DialogFragment {
    public static String TAG = "GameDialogFragment";

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder b=new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.game_instructions))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .setIcon(R.mipmap.appicon);
        if(getArguments().getString("Why").equals("Start")){
            b.setMessage(getString(R.string.ships_placement_1)+" "+getArguments().getInt("ShipsNum",-1)+" "+getString(R.string.ships_placement_2));
        }else{
            b.setMessage(getString(R.string.limit_reached));
        }
        return b.create();
    }
}
