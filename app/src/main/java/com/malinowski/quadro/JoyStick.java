package com.malinowski.quadro;

import android.app.Activity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import static java.lang.Math.sqrt;
//виртуальный джойстик
public class JoyStick {
    ImageView point;
    private float cx, cy; //центральные координаты, определяются первым касанием
    double angle = 0;//угол в полярной системе координат
    double distance = 0;//расстояние в полярной системе координат, оно относительное 0..1 где 0-центр 1-край
    private float size = 100;//размер джойстика(самой image)
    int id;// для привязки к пальцу, который создал джойстик
    JoyStick(float x, float y, Activity activity,int id){
        cx = x;
        cy = y;
        this.id = id;

        point = new ImageView(activity);
        point.setImageResource(R.drawable.purple_point);
        point.setImageAlpha(90);
        point.setX(x-size/2.0f);
        point.setY(y-size/2.0f);

        activity.addContentView(point,new FrameLayout.LayoutParams((int)size,(int)size));

    }

    //math stuff
    void move(float x, float y){

        double len = sqrt(Math.pow(cx-x,2)+Math.pow(cy-y,2));
        //математика
        if(len > size*1.5){//граница, на которую отклоняется джойстик. определена, как 1.5 размера image
            point.setX(cx + (float)((x-cx)/len*size*1.5) - size/2);
            point.setY(cy + (float)((y-cy)/len*size*1.5) - size/2);
        }
        else {
            point.setX(x - size / 2);
            point.setY(y - size / 2);
        }
        //еще математика
        angle = Math.atan2((x-cx),(y-cy))+Math.PI/2;
        distance = (len<size*1.5)?len/(size*1.5):1;

        //Log.i("Dis & angle",distance+" "+angle);
    }
    void delete(){
        FrameLayout parent = (FrameLayout) point.getParent();
        parent.removeView(point);
    }
}
