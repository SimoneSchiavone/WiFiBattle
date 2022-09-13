package it.di.unipi.mat582418.wifibattle.Database;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import it.di.unipi.mat582418.wifibattle.R;

/*Dialog fragment per chiedere conferma della cancellazione dal database di un una partita salvata*/
public class CancellationConfirmDialog extends DialogFragment {
    public static String TAG = "CancellationConfirmDialog";

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder b=new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.cancel))
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /* Procedo alla cancellazione chiamando il metodo RemoveMatch a cui passo il timestamp
                         (chiave primaria) dell record selezionato */
                        MatchHistory db = new MatchHistory(getActivity(), "MatchHistory", null, 2);
                        db.removeMatch(getArguments().getString("Timestamp","NULL"));
                        ((HistoryActivity)getActivity()).showAllInRV();
                    }
                })
                .setIcon(R.mipmap.appicon)
                .setMessage(getString(R.string.del_confirm)+" "+getArguments().getString("Timestamp","NULL"));
        return b.create();
    }
}
