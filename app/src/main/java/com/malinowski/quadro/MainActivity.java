package com.malinowski.quadro;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

//библиотека для websocket
import tech.gusavila92.websocketclient.WebSocketClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private WebSocketClient webSocketClient;
    static boolean isConnected = false;

    //Виджеты для отладки, на них выводится инфа о принимаемых/отправляемых данных
    static TextView info;
    String info_text = "Get from drone";
    static TextView info2;
    String info_text2 = "Sent to drone";

    //image - это "лампочка" горит - зеленым есть связь, красным - нет
    static ImageView point;
    static Button start,stop;

    //изображение с камеры дрона (при его наличии)
    private Bitmap decodedByte;
    static ImageView image;

    JoyStick joyStickR;
    JoyStick joyStickL;

    int screenWidth;
    int screenHeight;

    //использование физичесского геймпада
    static boolean useJoyStick = true;

    //переменные общения с дроном
    int throttle = 127;
    int yaw = 127;
    int pitch = 127;
    int roll = 127;
    static int mode = 2;

    //ip - адресс и порт для подключения
    static int[] ip = {192,168,1,186};
    static int port = 8888;

    private int time=0;//время с последнего касания

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        screenWidth = getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        screenHeight = getApplicationContext().getResources().getDisplayMetrics().heightPixels;

        info = findViewById(R.id.info);
        info2 = findViewById(R.id.info2);
        point = findViewById(R.id.point);
        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(webSocketClient != null){//254 254 127 127 - это команда запуска двигателей
                    webSocketClient.send(
                           toByteArray(254,254,127,127,mode));
                    info_text2 = "254 254 127 127 " + mode;
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(webSocketClient != null){//1 254 127 127 - это команда остановки двигателей
                    webSocketClient.send(
                            toByteArray(1,254,127,127,mode));
                    info_text2 = "1 254 127 127 " + mode;
                }
            }
        });

        image = findViewById(R.id.image);
        Bitmap bitmap = BitmapFactory.decodeResource(this.getApplicationContext().getResources(),
                R.drawable.no_video);
        image.setImageBitmap(bitmap);

        MyTimer timer = new MyTimer();
        timer.start();

        createWebSocketClient();
    }



    private void createWebSocketClient() {
        URI uri;
        try {
            uri = new URI("ws://"+ip[0]+"."+ip[1]+"."+ip[2]+"."+ip[3]+":"+port+"/");// ip - адресс и порт для подключения
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        //методы, прослушивающие соединение
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                info_text = "Connection start";
                isConnected = true;
            }

            @Override
            public void onTextReceived(String message) {
                System.out.println("onTextReceived");
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                //isConnected = true;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                //полученные байты конвертируются в изображение и если исходые данные представляли картинку - она будет установлена
                //в ImageView по центру основной активити
                decodedByte = BitmapFactory.decodeByteArray(data, 0, data.length,options);

                info_text = data.toString();
            }

            @Override
            public void onPingReceived(byte[] data) {
                //isConnected = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    info_text = new String(data, StandardCharsets.UTF_8);
                else
                    info_text = Arrays.toString(data);
            }

            @Override
            public void onPongReceived(byte[] data) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    info_text = new String(data, StandardCharsets.UTF_8);
                else
                    info_text = Arrays.toString(data);
            }

            @Override
            public void onException(Exception e) {
                isConnected = false;
                info_text = e.getMessage();
            }

            @Override
            public void onCloseReceived() {
                isConnected = false;
                info_text = "Connection lost";
            }
        };

        //параметры websocket'а
        webSocketClient.setConnectTimeout(5000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.addHeader("Origin", "http://developer.example.com");
        webSocketClient.enableAutomaticReconnection(1000);
        webSocketClient.connect();
    }
    //обновление информации просиходит по таймеру т.к. android не позволяет изменять View вне созданного их потока
    boolean update_info(){
        info.setText(info_text);
        info2.setText(info_text2);
        point.setImageResource((isConnected)?R.drawable.green_point:R.drawable.red_point);
        if(decodedByte!=null)
            image.setImageBitmap(decodedByte);
        return true;
    }
    //изменение левого джойстика
    void onDirectionChanged_left(double degrees, double distance){//значение расстояния ограничено 0..1
        //формулы перевода значений угла и расстояния от центра в тангаж, крен...
        throttle = (int)Math.floor(127-127*distance*Math.cos(degrees));
        yaw = (int)Math.floor(127+127*distance*Math.sin(degrees));

        if(webSocketClient != null) {
            webSocketClient.send(toByteArray(yaw, throttle, pitch, roll, mode));
            info_text2 = yaw + " " + throttle + " " + pitch + " " + roll + " " + mode;
        }
    }
    //изменение правого джойстика
    void onDirectionChanged_right(double degrees, double distance){
        //формулы перевода значений угла и расстояния от центра в тангаж, крен...
        pitch  = (int)Math.floor(127-127*distance*Math.cos(degrees));
        roll  = (int)Math.floor(127+127*distance*Math.sin(degrees));

        if(webSocketClient!= null) {
            webSocketClient.send(toByteArray(yaw, throttle, pitch, roll, mode));
            info_text2 = yaw + " " + throttle + " " + pitch + " " + roll + " " + mode;
        }
    }

    //по протоколу общения первый бит команды - всегда 0xFF
    byte[] toByteArray(int yaw, int throttle,int pitch, int roll, int mode){
        byte[] bytedata = new byte[6];

        bytedata[0] = (byte)0xFF;
        bytedata[1] = (byte)yaw;
        bytedata[2] = (byte)throttle;
        bytedata[3] = (byte)pitch;
        bytedata[4] = (byte)roll;
        bytedata[5] = (byte)mode;

        return  bytedata;
    }

    //работа с виртуальным джойстиком (мультитач внутри)
    /*
    кратко: в зависимости от более левого или более правого касания создается джойстик соотвественно слева или права (ACTION_DOWN и ACTION_POINTER_DOWN)
    Далее прослушиваются действия движения пальцев или их поднятия, соотвественные методы вызываются у JoyStickL и JoyStickR
    */
    @Override
    public boolean onTouchEvent(MotionEvent e) {//для лучшего понимания стоит почитать документацию Android о мультитач
        time = 0;
        if(info.getVisibility()==View.INVISIBLE) info.setVisibility(View.VISIBLE);
        if(info2.getVisibility()==View.INVISIBLE) info2.setVisibility(View.VISIBLE);

        int pointerID = e.getPointerId(e.getActionIndex());

        if(e.getPointerCount()<=pointerID)
            return true;
        try {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    //проверка координат касания и создание нужного джойстика
                    if (e.getX(pointerID) > screenWidth / 2.0f) {
                        if (joyStickR != null)
                            joyStickR.delete();
                        joyStickR = new JoyStick(e.getX(pointerID), e.getY(pointerID),this, pointerID);
                    } else {
                        if (joyStickL != null)
                            joyStickL.delete();
                        joyStickL = new JoyStick(e.getX(pointerID), e.getY(pointerID),this, pointerID);
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    //если касание всего одно - двигаем лишь один сущетсвующий джойстик
                    if (e.getPointerCount() == 1) {
                        if (joyStickR != null && joyStickR.id == e.getPointerId(e.getActionIndex()))
                            joyStickR.move(e.getX(pointerID), e.getY(pointerID));
                        else if (joyStickL != null && joyStickL.id == e.getPointerId(e.getActionIndex()))
                            joyStickL.move(e.getX(pointerID), e.getY(pointerID));
                    //если касаний два - двигаем оба джойстика
                    } else if (e.getPointerCount() == 2) {
                        joyStickR.move(e.getX(joyStickR.id), e.getY(joyStickR.id));
                        joyStickL.move(e.getX(joyStickL.id), e.getY(joyStickL.id));
                    }

                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    //удаление джойстика, что соответсвовал id поднятого пальца
                    if (joyStickR != null && pointerID == joyStickR.id) {
                        joyStickR.delete();
                        joyStickR = null;
                    } else {
                        joyStickL.delete();
                        joyStickL = null;
                    }
                    break;
            }
        }catch (Throwable t){
            Log.e("Error multitouch: ", t.getMessage());
        }
        //обновелние информации и отправка дрону
        if(joyStickL!=null)onDirectionChanged_left(joyStickL.angle,joyStickL.distance);
        if(joyStickR!=null)onDirectionChanged_right(joyStickR.angle,joyStickR.distance);

        Log.i("Касания ", ""+e.getPointerCount() + " id ");
        return true;
    }

    //работа с реальным джойстиком
    /*
    в кратце: первый метод вызывается при получении информации от какого-либо controller'а
    затем обрабатываются движения стиков, у полученного event'а есть методы получения относительных координат
    -для лучшего понимания стоит посетить документацию android controller-

    AXIS_X / AXIS_HAT_X- x левого стика
    AXIS_Y / AXIS_HAT_Y- y левого стика
    AXIS_Z - x правого стика
    AXIS_RZ - y правого стика
    */
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        //проверка, что эвент пришел от джойстика, а не от другого controller'а
        if (((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                || ((event.getSource() & InputDevice.SOURCE_JOYSTICK)
                == InputDevice.SOURCE_JOYSTICK)) {
            processJoystickInput(event,-1);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private void processJoystickInput(MotionEvent event,
                                      int historyPos) {

        InputDevice inputDevice = event.getDevice();
        //строки ниже дают координаты стика относительно джойстика
        float x = getCenteredAxis(event, inputDevice,
                MotionEvent.AXIS_X, historyPos);
        if (x == 0) {
            x = getCenteredAxis(event, inputDevice,
                    MotionEvent.AXIS_HAT_X, historyPos);
        }
        float y = getCenteredAxis(event, inputDevice,
                MotionEvent.AXIS_Y, historyPos);
        if (y == 0) {
            y = getCenteredAxis(event, inputDevice,
                    MotionEvent.AXIS_HAT_Y, historyPos);
        }
        //математика
        double angle = Math.atan2(x,y)+ Math.PI/2;
        double distance = Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
        //отправка данных по левому джойстику
        onDirectionChanged_left(angle,distance);
        Log.i("Reall Joystick",angle + " " + distance );
        //аналогично с правым
        x = getCenteredAxis(event, inputDevice,
                    MotionEvent.AXIS_Z, historyPos);
        y = getCenteredAxis(event, inputDevice,
                    MotionEvent.AXIS_RZ, historyPos);
        angle = Math.atan2(x,y)+Math.PI/2;
        distance = Math.sqrt(Math.pow(x,2)+Math.pow(y,2));
        Log.i("Reall Joystick",angle + " " + distance );
        onDirectionChanged_right(angle,distance);

    }
    //это метод из официальной документации, понятия не имею почему нельзя без него обойтись
    private static float getCenteredAxis(MotionEvent event,
                                         InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range =
                device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value =
                    historyPos < 0 ? event.getAxisValue(axis):
                            event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void update(){
        update_info();//обновление информации для разработки
        if(SettingsActivity.change){//попытка соединения по новому адресу
            webSocketClient.close();
            webSocketClient = null;
            isConnected = false;
            createWebSocketClient();
        }
        time+=100;
        if(time%5000==0){
            //info.setVisibility(View.INVISIBLE);
           // info2.setVisibility(View.INVISIBLE);
        }
    }

    public void onSettings(View view) {
        startActivity (new Intent(this, SettingsActivity.class));
    }
    
    class MyTimer extends CountDownTimer {
        MyTimer() {
            super(Integer.MAX_VALUE, 100);
        }
        @Override
        public void onTick(long millisUntilFinished) {
            update();
        }
        @Override
        public void onFinish() {
        }
    }
}