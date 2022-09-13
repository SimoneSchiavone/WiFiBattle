package it.di.unipi.mat582418.wifibattle;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UsernameDialogFragment extends DialogFragment {

    private EditText username; //Casella di testo dove l'utente digiterà il nome
    private Button savebutton; //Bottone per il salvataggio dell'username
    public static String TAG = "UsernameDialogFragment";

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater=getActivity().getLayoutInflater();
        View view=inflater.inflate(R.layout.username_dialog,null);
        //Recupero elementi della view
        username=view.findViewById(R.id.edit_text_username);
        savebutton=view.findViewById(R.id.savebutton);

        //Creazione del dialog
        AlertDialog.Builder b=new AlertDialog.Builder(getActivity())
                .setView(view)
                .setTitle(getString(R.string.username_required))
                .setIcon(R.mipmap.appicon);

        //Preparazione bottone di salvataggio
        savebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(username.getText().toString().length()==0) {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.no_empty_username), Toast.LENGTH_SHORT).show();
                }else{
                    //Salvataggio dello username nelle preferenze
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString("username",username.getText().toString()).commit();
                    TextView tv=(TextView) getActivity().findViewById(R.id.username);
                    tv.setText(username.getText().toString());
                    dismiss();
                }
            }
        });

        return b.create();
    }
}
