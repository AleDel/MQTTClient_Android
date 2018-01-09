package com.example.ale.mqttclient;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.URI;

public class MainActivity extends AppCompatActivity {

    static String MQTTHOST = "tcp://0.tcp.ngrok.io:14056";
    static String USERNAME = "";
    static String PASSWORD = "";

    String topicStr = "test/dispositivos";

    MqttAndroidClient client;

    TextView subText;

    Vibrator vibrator;

    Ringtone myRingTone;

    boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        subText = findViewById(R.id.subText);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        myRingTone = RingtoneManager.getRingtone(getApplicationContext(),uri);

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);

        isConnected = false;
        //MqttConnectOptions options = new MqttConnectOptions();
        //options.setUserName(USERNAME);
        //options.setPassword(PASSWORD.toCharArray());

        try {
            //IMqttToken token = client.connect(options);
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Conectado", Toast.LENGTH_LONG ).show();
                    isConnected = true;
                    setSubscription();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Conección falló", Toast.LENGTH_LONG ).show();
                    isConnected = false;

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }


        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                subText.setText(new String(message.getPayload()));

                vibrator.vibrate(500);

                myRingTone.play();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }




    public void pub(View v){
        String topic = topicStr;
        String message = "Hola";

        if(isConnected){
            try {

                client.publish(topic, message.getBytes(),0,false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }else {
            Toast.makeText(MainActivity.this, "No estas conectado, no puedes publicar a:"+topicStr, Toast.LENGTH_LONG ).show();
        }


    }

    private void setSubscription(){
        if(isConnected){
            try {
                client.subscribe(topicStr,0);
            }catch (MqttException e){
                e.printStackTrace();
            }
        }else {
            Toast.makeText(MainActivity.this, "No estas conectado, no se puede suscribir a:"+topicStr, Toast.LENGTH_LONG ).show();
        }
    }

    public void connect(View v){
        try {
            //IMqttToken token = client.connect(options);
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Conectado", Toast.LENGTH_LONG ).show();
                    isConnected = true;
                    setSubscription();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Conección falló", Toast.LENGTH_LONG ).show();
                    isConnected = false;

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    public void disconnect(View v){
        if(isConnected) {
            try {

                IMqttToken token = client.disconnect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Toast.makeText(MainActivity.this, "desconectado", Toast.LENGTH_LONG).show();
                        isConnected = false;
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Toast.makeText(MainActivity.this, "No puedo desconectar", Toast.LENGTH_LONG).show();
                        isConnected = true;

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }else {
            Toast.makeText(MainActivity.this, "Ya estas deconectado", Toast.LENGTH_LONG ).show();
        }

    }
}
