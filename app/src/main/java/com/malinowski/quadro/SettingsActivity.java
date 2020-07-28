package com.malinowski.quadro;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity  implements PopupMenu.OnMenuItemClickListener {

    Button menu; // определяет режим полета дрона
    Switch aSwitch; // использовать ли реальный джойстик (при его подключении)
    TextView angle; // поворот картинки от дрона
    EditText ip1,ip2,ip3,ip4,port; // ip и порт дрона

    static boolean change = false; // изменение в ip адресе и порте

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // убирает заголовок pop_up activity
        setContentView(R.layout.settings_activity);

        menu = findViewById(R.id.mode_menu);
        aSwitch = findViewById(R.id.switch1);

        angle = findViewById(R.id.angle);
        Log.i("Seek",""+MainActivity.image.getRotation()/360*100);
        angle.setText(MainActivity.image.getRotation()+"");

        menu.setText(""+MainActivity.mode);
        aSwitch.setChecked(MainActivity.useJoyStick);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MainActivity.useJoyStick = b;
            }
        });

        ip1 = findViewById(R.id.ip1);
        ip1.setText(""+MainActivity.ip[0]);

        ip2 = findViewById(R.id.ip2);
        ip2.setText(""+MainActivity.ip[1]);

        ip3 = findViewById(R.id.ip3);
        ip3.setText(""+MainActivity.ip[2]);

        ip4 = findViewById(R.id.ip4);
        ip4.setText(""+MainActivity.ip[3]);

        port = findViewById(R.id.port);
        port.setText(""+MainActivity.port);
    }

    //метод вызывается при клике на кнопку режимов полета
    public void showPopUp(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(SettingsActivity.this);
        popup.inflate(R.menu.popup);
        popup.show();
    }
    @SuppressLint("SetTextI18n")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.zero:
                MainActivity.mode = 0;
                break;
            case R.id.one:
                MainActivity.mode = 1;
                break;
            case R.id.two:
                MainActivity.mode = 2;
                break;
            case R.id.three:
                MainActivity.mode = 3;
                break;
            default:
                break;
        }
        menu.setText(""+MainActivity.mode);
        return true;
    }
    //при закрытии активности проверка на изменение ip - адреса и порта
    @Override
    protected void onDestroy () {
        super.onDestroy();
        change = false;
        if(!ip1.getText().toString().equals("")) {
            int ip_1 = Integer.parseInt(ip1.getText().toString());
                change = change | (ip_1!=MainActivity.ip[0]);
            MainActivity.ip[0] = ip_1;
        }
        if(!ip2.getText().toString().equals("")) {
            int ip_2 = Integer.parseInt(ip2.getText().toString());
            change = change | (ip_2!=MainActivity.ip[1]);
            MainActivity.ip[1] = ip_2;
        }
        if(!ip3.getText().toString().equals("")) {
            int ip_3 = Integer.parseInt(ip3.getText().toString());
            change = change | (ip_3!=MainActivity.ip[2]);
            MainActivity.ip[2] = ip_3;
        }
        if(!ip4.getText().toString().equals("")) {
            int ip_4 = Integer.parseInt(ip4.getText().toString());
            change = change | (ip_4!=MainActivity.ip[3]);
            MainActivity.ip[3] = ip_4;
        }
        if(!port.getText().toString().equals("")) {
            int port_ = Integer.parseInt(port.getText().toString());
            change = change | (port_!=MainActivity.port);
            MainActivity.port = port_;
        }
    }

    public void onPlus(View view) {
        float an = Float.parseFloat(angle.getText().toString())+90;
        MainActivity.image.setRotation(an%360);
        angle.setText(MainActivity.image.getRotation()+"");
    }

    public void onMinus(View view) {
        float an = Float.parseFloat(angle.getText().toString())-90;
        MainActivity.image.setRotation(an%360);
        angle.setText(MainActivity.image.getRotation()+"");
    }
}