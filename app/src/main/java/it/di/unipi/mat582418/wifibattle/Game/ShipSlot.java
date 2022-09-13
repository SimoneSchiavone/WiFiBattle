package it.di.unipi.mat582418.wifibattle.Game;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageButton;

/*Sottoclasse di AppCompatImageButton che mantiene al suo interno un booleano che segnala che
 * se cella è già stata assegnata o meno.*/
public class ShipSlot extends AppCompatImageButton {
    boolean assigned;

    public ShipSlot(Context context) {
        super(context);
        assigned=false;
    }

    public ShipSlot(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShipSlot(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void assign(){
        assigned=true;
    }

    public void free(){
        assigned=false;
    }

    public boolean is_assigned(){
        return assigned;
    }
}
