package it.di.unipi.mat582418.wifibattle.Database;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.widget.TextView;


import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class ReverseGeocodingTask extends AsyncTask<Location,Void,String> {
    private Context mcontext;
    private TextView to_update;

    public ReverseGeocodingTask(Context c, TextView tv){
        super();
        mcontext=c;
        to_update=tv;
    }

    @Override
    protected String doInBackground(Location... locations) {
        Geocoder geocoder=new Geocoder(mcontext, Locale.getDefault());
        if(geocoder.isPresent()){
            Location loc=locations[0];
            List<Address> addresses=null; //possibili match multipli
            try{
                addresses=geocoder.getFromLocation(loc.getLatitude(),loc.getLongitude(),1);
            }catch (IOException e){
                e.printStackTrace();
                return null;
            }
            String textual_address=null;
            if(addresses!=null && addresses.size()>0){
                Address a=addresses.get(0);
                StringBuilder sb=new StringBuilder();
                for(int i=0;i<a.getMaxAddressLineIndex();i++){
                    sb.append(a.getAddressLine(i)+", ");
                }
                sb.append(a.getAddressLine(a.getMaxAddressLineIndex()));
                textual_address=sb.toString();
            }
            return textual_address;
        }else{
            return null;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        //Al termine dell'operazione di geocoding aggiorno la textview contenente l'indirizzo
        if(to_update!=null && s!=null){
            to_update.setText(s);
        }
    }
}
