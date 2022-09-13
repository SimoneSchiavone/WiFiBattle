package it.di.unipi.mat582418.wifibattle.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import it.di.unipi.mat582418.wifibattle.Game.GameInfo;

public class MatchHistory extends SQLiteOpenHelper {
    private static final String MATCHES_TABLE = "Matches";
    public static final int TIMESTAMP = 0;
    public static final int MYSCORE =1;
    public static final int OPPONENTSCORE =2;
    public static final int OPPONENTNAME =3;
    public static final int PLAYEDTURNS =4;
    public static final int MYSHIPAVAILABLE =5;
    public static final int MYSHIPSDESTROYED =6;
    public static final int ENEMYSHIPSHITTED =7;
    public static final int ENEMYSHIPSMISSED =8;
    public static final int IGAVEUP =9;
    public static final int ENEMYGAVEUP =10;
    public static final int LATITUDE =11;
    public static final int LONGITUDE =12;

    public MatchHistory(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /*Creazione di una tabella Matches con campi:
         _id (TEXT-PK), MyScore (INTEGER), OpponentScore (INTEGER), OpponentName (TEXT), PlayedTurns (INTEGER),
         MyShipsAvailable (TEXT), MyShipsDestroyed (TEXT), EnemyShipsHitted (TEXT), EnemyShipsMissed (TEXT),
         IGaveUp (INTEGER), EnemyGaveUp (INTEGER), Latitude (TEXT), Longitude (TEXT)*/
        String create_table="CREATE TABLE "+MATCHES_TABLE+" ( _id TEXT PRIMARY KEY, MyScore INTEGER, OpponentScore INTEGER, OpponentName TEXT, PlayedTurns INTEGER, MyShipsAvailable TEXT, MyShipsDestroyed TEXT, EnemyShipsHitted TEXT, EnemyShipsMissed TEXT, IGaveUp INTEGER, EnemyGaveUp INTEGER, Latitude TEXT, Longitude TEXT)";
        db.execSQL(create_table);
    }

    //Metodo per l'inserimento di una partita nel database nel caso sia disponibile un riferimento alla posizione
    public void addMatch(GameInfo gi,double lat,double lon){
        StringBuilder id=new StringBuilder(getCurrentDaystamp());
        id.append("-");
        id.append(getCurrentTimestamp());
        int a=gi.i_surrender ? 1 : 0;
        int b=gi.opponent_surrender ? 1 : 0;
        ContentValues cv=new ContentValues();
        cv.put("_id",id.toString());
        cv.put("MyScore",gi.my_points);
        cv.put("OpponentScore",gi.opponent_points);
        cv.put("OpponentName",gi.opponent_name);
        cv.put("PlayedTurns",gi.played_turns);
        cv.put("MyShipsAvailable",gi.my_ships_available.toString());
        cv.put("MyShipsDestroyed",gi.my_ships_destroyed.toString());
        cv.put("EnemyShipsHitted",gi.hitted.toString());
        cv.put("EnemyShipsMissed",gi.missed.toString());
        cv.put("IGaveUp",a);
        cv.put("EnemyGaveUp",b);
        cv.put("Latitude",lat);
        cv.put("Longitude",lon);
        SQLiteDatabase db=this.getWritableDatabase();
        db.insert(MATCHES_TABLE,null,cv);
    }

    //Metodo per l'inserimento di una partita nel database nel caso non sia disponibile un riferimento alla posizione
    public void addMatch(GameInfo gi){
        StringBuilder id=new StringBuilder(getCurrentDaystamp());
        id.append("-");
        id.append(getCurrentTimestamp());
        int a=gi.i_surrender ? 1 : 0;
        int b=gi.opponent_surrender ? 1 : 0;
        ContentValues cv=new ContentValues();
        cv.put("_id",id.toString());
        cv.put("MyScore",gi.my_points);
        cv.put("OpponentScore",gi.opponent_points);
        cv.put("OpponentName",gi.opponent_name);
        cv.put("PlayedTurns",gi.played_turns);
        cv.put("MyShipsAvailable",gi.my_ships_available.toString());
        cv.put("MyShipsDestroyed",gi.my_ships_destroyed.toString());
        cv.put("EnemyShipsHitted",gi.hitted.toString());
        cv.put("EnemyShipsMissed",gi.missed.toString());
        cv.put("IGaveUp",a);
        cv.put("EnemyGaveUp",b);
        cv.put("Latitude","null");
        cv.put("Longitude","null");
        SQLiteDatabase db=this.getWritableDatabase();
        db.insert(MATCHES_TABLE,null,cv);
    }

    //Metodo per la cancellazione di una partita dal database data la sua chiave primaria cioè il timestamp
    public void removeMatch(String primarykey){
        String query="DELETE FROM "+MATCHES_TABLE+" WHERE _id == '"+primarykey+"'";
        this.getWritableDatabase().execSQL(query);
    }

    //Metodo che restituisce un cursore con tutte le partite effettuate
    public Cursor getAllMatches(boolean only_completed){
        String query;
        if(only_completed)
            query="SELECT * FROM "+MATCHES_TABLE+" WHERE (IGaveUp!=1 AND EnemyGaveUp != 1)";
        else
            query="SELECT * FROM "+MATCHES_TABLE;
        return this.getReadableDatabase().rawQuery(query,null);
    }

    /*Metodo che restituisce un cursore contenente tutte le partite vinte. Se only_completed è true allora restituisce
    solo le partite vinte e completate.*/
    public Cursor getAllWinMatches(boolean only_completed){
        String query;
        if(!only_completed)
            query="SELECT * FROM "+MATCHES_TABLE+" WHERE (MyScore > OpponentScore OR EnemyGaveUp=1) ";
        else
            query="SELECT * FROM "+MATCHES_TABLE+" WHERE (MyScore > OpponentScore AND IGaveUp!=1 AND EnemyGaveUp != 1) ";
        return this.getReadableDatabase().rawQuery(query,null);
    }

    /*Metodo che restituisce un cursore contenente tutte le partite pareggiate. Se only_completed è true allora restituisce
    solo le partite pareggiate e completate.*/
    public Cursor getAllTieMatches(boolean only_completed){
        String query;
        if(!only_completed)
            query="SELECT * FROM "+MATCHES_TABLE+" WHERE (MyScore = OpponentScore)";
        else
            query="SELECT * FROM "+MATCHES_TABLE+" WHERE (MyScore = OpponentScore AND IGaveUp != 1 AND EnemyGaveUp != 1)";
        return this.getReadableDatabase().rawQuery(query,null);
    }

    /*Metodo che restituisce un cursore contenente tutte le partite perse. Se only_completed è true allora restituisce
    solo le partite perse e completate.*/
    public Cursor getAllLostMatches(boolean only_completed){
        String query;
        if(!only_completed)
            query="SELECT * FROM "+MATCHES_TABLE+" WHERE (MyScore < OpponentScore OR IGaveUp=1)";
        else
            query="SELECT * FROM "+MATCHES_TABLE+" WHERE (MyScore < OpponentScore AND IGaveUp!=1 AND EnemyGaveUp != 1) ";
        return this.getReadableDatabase().rawQuery(query,null);
    }

    /*Metodo che restituisce un cursore contenente tutte le partite ordinate per punteggio. Se only_completed è true allora
     saranno restituite solo le partite completate. Se desc è true l'ordinamento sarà decrescente sul punteggio, altrimenti
     crescente.*/
    public Cursor OrderMatchesByPoints(boolean desc,boolean only_completed){
        String query;
        if(!desc){
            if(only_completed){
                query="SELECT * FROM "+MATCHES_TABLE+" WHERE (IGaveUp!=1 AND EnemyGaveUp != 1) ORDER BY MyScore ";
            }else{
                query="SELECT * FROM "+MATCHES_TABLE+" ORDER BY MyScore ";
            }
        }else{
            if(only_completed){
                query="SELECT * FROM "+MATCHES_TABLE+" WHERE (IGaveUp!=1 AND EnemyGaveUp != 1) ORDER BY MyScore DESC ";
            }else{
                query="SELECT * FROM "+MATCHES_TABLE+" ORDER BY MyScore DESC";
            }
        }
        return this.getReadableDatabase().rawQuery(query,null);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + MATCHES_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MATCHES_TABLE);
        onCreate(db);
    }

    public static String getCurrentDaystamp() {
        return new SimpleDateFormat("yyyy/MM/dd").format(Calendar.getInstance().getTime());
    }

    public static String getCurrentTimestamp() {
        return new SimpleDateFormat("HH.mm.ss").format(Calendar.getInstance().getTime());
    }

}
