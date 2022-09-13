package it.di.unipi.mat582418.wifibattle.WifiConnection;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;
import it.di.unipi.mat582418.wifibattle.R;


public class WifiP2PAdapter extends ArrayAdapter<WifiP2pDevice> {
    Context ctx;

    public WifiP2PAdapter(Context context, List<WifiP2pDevice> s){
        super(context,0,s);
        ctx=context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_info, parent, false);
        }
        //Recupero gli elementi della view
        TextView name = (TextView) convertView.findViewById(R.id.devicename);
        TextView addr = (TextView) convertView.findViewById(R.id.deviceaddr);
        TextView type=(TextView) convertView.findViewById(R.id.devicetype);

        //Recupero il device dalla lista e metto nei campi preposti le informazioni sul dispositivo
        WifiP2pDevice dev=(WifiP2pDevice) getItem(position);
        name.setText(dev.deviceName);
        addr.setText(dev.deviceAddress);
        switch (dev.status){
            case 3:
            case 0:
                type.setText(parent.getContext().getString(R.string.dev_available));
                break;
            case 1:
                type.setText(parent.getContext().getString(R.string.dev_invited));
                break;
            case 2:
                type.setText(parent.getContext().getString(R.string.dev_failed));
                break;
            case 4:
                type.setText(parent.getContext().getString(R.string.dev_unavailable));
                break;
        }

        //Restituisco la view riempita con le informazioni sul dispositivo di indice "position"
        return convertView;
    }
}
