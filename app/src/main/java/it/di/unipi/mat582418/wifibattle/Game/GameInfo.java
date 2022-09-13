package it.di.unipi.mat582418.wifibattle.Game;

import android.location.Location;
import java.util.ArrayList;

/*Classe che incapsula tutte le informazioni relative ad una partita svolta*/
public class GameInfo {
    //Punteggi ed esito partita
    public int my_points;
    public int opponent_points;
    public boolean i_surrender;
    public boolean opponent_surrender;
    public int played_turns;

    //Nome avversario
    public String opponent_name;

    //Situazione navi
    public ArrayList<Integer> my_ships_available; //mie disponibili
    public ArrayList<Integer> my_ships_destroyed; //mie distrutte
    public ArrayList<Integer> missed; //miei colpi mancati
    public ArrayList<Integer> hitted; //miei colpi a segno
    public ArrayList<Integer> enemy_missed; //colpi nemici mancati

    //Dove si è svolta la partita
    public Location battle_location;

    @Override
    public String toString() {
        return "GameInfo{" +
                "my_points=" + my_points +
                ", opponent_points=" + opponent_points +
                ", i_surrender=" + i_surrender +
                ", opponent_surrender=" + opponent_surrender +
                ", played_turns=" + played_turns +
                ", opponent_name='" + opponent_name + '\'' +
                ", my_ships_available=" + my_ships_available +
                ", my_ships_destroyed=" + my_ships_destroyed +
                ", missed=" + missed +
                ", hitted=" + hitted +
                ", enemy_missed=" + enemy_missed +
                ", battle_location=" + battle_location +
                '}';
    }
}
