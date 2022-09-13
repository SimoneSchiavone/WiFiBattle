package it.di.unipi.mat582418.wifibattle.Database;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import it.di.unipi.mat582418.wifibattle.R;

public class HistoryActivity extends Activity {
    RecyclerView rv;
    MatchHistoryAdapter mha;

    LinearLayout l1,l2,l3,l4,l5,l6;
    Switch switch_surrender;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_activity);

        //Listener per i click sui bottoni che comporteranno delle query al database delle partite
        View.OnClickListener clickListener=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(view==l1) {
                    new DBQueryTask(mha,1,rv).execute(switch_surrender.isChecked());
                }
                if(view==l2) {
                    new DBQueryTask(mha, 2, rv).execute(switch_surrender.isChecked());
                }
                if(view==l3) {
                    new DBQueryTask(mha, 3, rv).execute(switch_surrender.isChecked());
                }
                if(view==l4) {
                    new DBQueryTask(mha, 4, rv).execute(switch_surrender.isChecked());
                }
                if(view==l5) {
                    new DBQueryTask(mha, 5, rv).execute(switch_surrender.isChecked());
                }
                if(view==l6) {
                    new DBQueryTask(mha, 6, rv).execute(switch_surrender.isChecked());
                }
            }
        };

        switch_surrender=findViewById(R.id.no_surrendered);
        l1=findViewById(R.id.butt_all);
        l1.setOnClickListener(clickListener);

        l2=findViewById(R.id.butt_point_asc);
        l2.setOnClickListener(clickListener);

        l3=findViewById(R.id.butt_point_desc);
        l3.setOnClickListener(clickListener);

        l4=findViewById(R.id.butt_victories);
        l4.setOnClickListener(clickListener);

        l5=findViewById(R.id.butt_loss);
        l5.setOnClickListener(clickListener);

        l6=findViewById(R.id.butt_tie);
        l6.setOnClickListener(clickListener);

        ManageAnimation(R.anim.bouncing,l1,l2,l3,l4,l5,l6,switch_surrender);

        LinearLayoutManager llm=new LinearLayoutManager(this);
        rv=findViewById(R.id.rv);
        rv.setLayoutManager(llm);

        //Inizialmente mostro tutte le partite
        mha=new MatchHistoryAdapter(this);
        rv.setAdapter(mha);
        new DBQueryTask(mha,1,rv).execute(false);

    }

    private void ManageAnimation(int id, View... someviews) {
        Animation a = AnimationUtils.loadAnimation(this, id);
        if (a != null) {
            for (View v : someviews) {
                v.startAnimation(a);
            }
        }
    }

    protected void showAllInRV() {
        /*Metodo chiamato dal fragment di conferma cancellazione per aggiornare
        la view dopo la cancellazione di un record*/
        new DBQueryTask(mha,1,rv).execute(false);
    }
}
