package it.di.unipi.mat582418.wifibattle;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import androidx.annotation.Nullable;

public class SettingsActivity extends PreferenceActivity {
    //Activity che mostrerà le preferenze scelte dall'utente

    static final int PHOTO_PICK=0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Carico le preferenze dal file xml di nome "preferences"
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        //Metodo invocato quando una preferenza viene cliccata

        //Selezione foto da galleria
        if(preference.getKey().equals("picture")){
            Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, PHOTO_PICK);
        }

        //Cancellazione foto profilo
        if(preference.getKey().equals("delete_picture")){
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().remove("picture").commit();
        }

        //TODO Da realizzare in futuro
        //Scatta la foto profilo
        if(preference.getKey().equals("take_picture")){
            Toast.makeText(this, getString(R.string.TODO), Toast.LENGTH_SHORT).show();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==PHOTO_PICK){
            if(resultCode==RESULT_CANCELED) //Nessuna foto selezionata, avviso l'utente
                Toast.makeText(getApplicationContext(), getString(R.string.no_photo_selected), Toast.LENGTH_SHORT).show();

            if(resultCode==RESULT_OK && data!=null){ //Se ho selezionato la foto salvo la sua uri nella preferenza dedicata
                Uri selected_image=data.getData();
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("picture",selected_image.toString()).commit();
            }
        }

    }
}
