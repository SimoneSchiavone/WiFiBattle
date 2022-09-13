package it.di.unipi.mat582418.wifibattle.Game;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class TableLayout extends ViewGroup {

    public TableLayout(Context context) {
        super(context);
    }

    public TableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TableLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private int getSide(){
        //Parte intera superiore della radice quadrata del numero di figli
        return (int)Math.ceil(Math.sqrt(getChildCount()));
    }
    @Override
    protected void onLayout(boolean b, int left, int top, int right, int bottom) {
        /*Quando il sistema grafico chiama il nostro onLayout(), ci sta chiedendo
         di disporre i figli in maniera tale che tutto il gruppo occupi lo spazio
         definito da left, top, right, bottom. Noi semplicemente dividiamo il nostro
         spazio in un quadrato di celle di uguale dimensione, e disponiamo i figli
         chiamando su ciascuno il metodo layout() con le coordinate calcolate.*/
        int side=getSide();
        int width=(right-left)/side; //larghezza di una view figlia
        int height=(bottom-top)/side; //altezza di una view figlia

        for(int i=0;i<this.getChildCount();i++){
            View view=getChildAt(i);
            int x=i%side;
            int y=i/side;
            //Non negozio ma semplicemente impongo le dimensioni
            view.layout(x*width,y*height,(x+1)*width,(y+1)*height);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int side=getSide();
        int w=getMeasuredWidth()/side;
        int h=getMeasuredHeight()/side;
        int ws=MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY);
        int hs=MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY);
        for(int i=0;i<this.getChildCount();i++){
            View v=getChildAt(i);
            v.measure(ws,hs);
        }

    }
}
