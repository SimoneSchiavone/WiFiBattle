package it.di.unipi.mat582418.wifibattle.Database;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.service.autofill.FieldClassification;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import it.di.unipi.mat582418.wifibattle.R;

public class MatchHistoryAdapter extends RecyclerView.Adapter<MatchHistoryAdapter.MatchHolder>  {
    private Cursor cursor; //salvo il cursore che mi serve per recuperare gli elementi
    private Context ctx;

    public MatchHistoryAdapter(Context ctx){
        this.ctx=ctx;
    }

    public void updateCursor(Cursor c){
        cursor=c;
    }

    @Override
    public MatchHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        //chiamato quando la recycler view ha bisogno di creare un nuovo VH da inserire nel parent dato
        View v= LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.match_detail,viewGroup,false);
        MatchHolder ivh=new MatchHolder(v);
        return ivh;
    }

    @Override
    public void onBindViewHolder(MatchHolder ivh, int i) {
        //Chiamato quando la recycler view vuole inserire i dati di indice dato nel VH dato
        cursor.moveToPosition(i);

        ivh.date.setText(cursor.getString(MatchHistory.TIMESTAMP));
        ivh.played_turns.setText(ivh.played_turns.getContext().getString(R.string.played_turns)+cursor.getString(MatchHistory.PLAYEDTURNS));
        Location location=new Location("");

        /*Se il record corrente contiene un riferimento alla posizione di gioco allora viene avviato un task
        asincrono che si occupa di effettuare il geocoding (da coordinate ad indirizzo)*/
        if(!cursor.getString(MatchHistory.LATITUDE).equals("null")) {
            location.setLatitude(Double.parseDouble(cursor.getString(MatchHistory.LATITUDE)));
            location.setLongitude(Double.parseDouble(cursor.getString(MatchHistory.LONGITUDE)));

            ivh.match_location.setText(ctx.getString(R.string.address) + " [" + cursor.getDouble(MatchHistory.LATITUDE) + " | " + cursor.getDouble(MatchHistory.LONGITUDE) + "]");
            new ReverseGeocodingTask(ivh.match_location.getContext(), ivh.match_location).execute(location);
        }else{
            ivh.match_location.setText(ctx.getString(R.string.location_na));
        }

        ivh.scores.setText(ivh.scores.getContext().getString(R.string.you)+":"+cursor.getInt(MatchHistory.MYSCORE)+" "+cursor.getString(MatchHistory.OPPONENTNAME)+":"+cursor.getInt(MatchHistory.OPPONENTSCORE));
        //ivh.my_situation.setText(ivh.my_situation.getContext().getString(R.string.Available)+cursor.getString(MatchHistory.MYSHIPAVAILABLE)+"\n"+ivh.my_situation.getContext().getString(R.string.Destroyed)+cursor.getString(MatchHistory.MYSHIPSDESTROYED));
        //ivh.opponent_situation.setText(ivh.my_situation.getContext().getString(R.string.Hitted)+cursor.getString(MatchHistory.ENEMYSHIPSHITTED)+"\n"+ivh.my_situation.getContext().getString(R.string.Missed)+cursor.getString(MatchHistory.ENEMYSHIPSMISSED));

        if(cursor.getInt(MatchHistory.IGAVEUP)==1){
            ivh.result.setText(ivh.result.getContext().getString(R.string.MySurrender));
        }else {
            if(cursor.getInt(MatchHistory.ENEMYGAVEUP)==1){
                ivh.result.setText(ivh.result.getContext().getString(R.string.EnemySurrender));
            }else {
                if(cursor.getInt(MatchHistory.MYSCORE)<cursor.getInt(MatchHistory.OPPONENTSCORE)){
                    ivh.result.setText(ivh.result.getContext().getString(R.string.defeat));
                }else{
                    ivh.result.setText(ivh.result.getContext().getString(R.string.victory));
                }
            }
        }

    }

    @Override
    public int getItemCount() {
        if(cursor==null)
            return 0;
        else
            return cursor.getCount();
    }




    public class MatchHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener{
        TextView date,scores,played_turns,result,match_location,my_situation,opponent_situation;

        public MatchHolder(View itemView){
            super(itemView);
            date=itemView.findViewById(R.id.date);
            scores=itemView.findViewById(R.id.scores);
            played_turns=itemView.findViewById(R.id.played_turns);
            result=itemView.findViewById(R.id.result);
            match_location=itemView.findViewById(R.id.match_location);
            //my_situation=itemView.findViewById(R.id.my_situation);
            //opponent_situation=itemView.findViewById(R.id.opponent_situation);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {
            TextView primarykey=view.findViewById(R.id.date);
            CancellationConfirmDialog cancellationConfirmDialog = new CancellationConfirmDialog();
            cancellationConfirmDialog.setCancelable(false);
            Bundle bundle = new Bundle();
            bundle.putString("Timestamp",primarykey.getText().toString());
            cancellationConfirmDialog.setArguments(bundle);
            cancellationConfirmDialog.show(((Activity)ctx).getFragmentManager(), CancellationConfirmDialog.TAG);
            return false;
        }
    }

}
