package it.di.unipi.mat582418.wifibattle.Database;

import android.database.Cursor;
import android.os.AsyncTask;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.recyclerview.widget.RecyclerView;
import it.di.unipi.mat582418.wifibattle.R;

public class DBQueryTask extends AsyncTask<Boolean,Void, Cursor> {
    MatchHistoryAdapter mha;
    RecyclerView rv;
    int query_num;
    MatchHistory db;

    public DBQueryTask(MatchHistoryAdapter adapter,int query,RecyclerView recyclerView){
        //Riferimenti necessari per aggiornare la vista dopo la query
        mha=adapter;
        rv=recyclerView;

        //intero che identifica la query che dovrà essere svolta dal task
        query_num=query;

        db = new MatchHistory(rv.getContext(), "MatchHistory", null, 2);
    }

    @Override
    protected Cursor doInBackground(Boolean... booleans) {
        //Se booleans[0] è true allora devono essere escluse le rese
        switch (query_num){
            case 1:
                return db.getAllMatches(booleans[0]);
            case 2:
                return db.OrderMatchesByPoints(false,booleans[0]);
            case 3:
                return db.OrderMatchesByPoints(true,booleans[0]);
            case 4:
                return db.getAllWinMatches(booleans[0]);
            case 5:
                return db.getAllLostMatches(booleans[0]);
            case 6:
                return db.getAllTieMatches(booleans[0]);
            default:
                break;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Cursor cursor) {
        if(cursor!=null){
            super.onPostExecute(cursor);
            mha.updateCursor(cursor);
            mha.notifyDataSetChanged();
            Animation a = AnimationUtils.loadAnimation(rv.getContext(), R.anim.fade_in);
            rv.startAnimation(a);
        }
    }
}
