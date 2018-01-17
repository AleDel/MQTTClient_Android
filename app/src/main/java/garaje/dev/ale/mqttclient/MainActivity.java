/*
 this code has been writted following Toeshiro Novisu tutorial https://youtu.be/BAkGm02WBc0
 */

package garaje.dev.ale.mqttclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;



import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MyActivity_mqttclient";

    static String MQTTHOST = "tcp://0.tcp.ngrok.io:18538";
    //static String MQTTHOST = "tcp://192.168.0.165:1883";
    static String USERNAME = "";
    static String PASSWORD = "";

    String topicStr = "test/dispositivos";

    MqttAndroidClient client;

    TextView texserver;
    TextView textchat;
    TextView textViewcoor;
    EditText editTextTopic;
    EditText editTextMensaje;
    Button configButton;

    Vibrator vibrator;
    Ringtone myRingTone;

    PowerManager.WakeLock wakeLock;

    NfcAdapter nfcAdapter;
    boolean isConnected;
    private static String clientId;

    public float acclX, acclY, acclZ;
    public SensorManager mSensorManager;
    public Sensor sensor;

    public static final String KEY_PREF_HOST = "host_text_preference";
    public static final String KEY_PREF_PORT = "port_text_preference";

    public void register() {
        // TODO Auto-generated method stub
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void unregister() {
        mSensorManager.unregisterListener(this, sensor);
    }

    protected String getAccelerometerReading() {
        return String.format("%7f" + ": %7f" + ": %7f", acclX, acclY, acclZ);
    }

    private void refreshDatosHost(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String hostPref = sharedPref.getString(MainActivity.KEY_PREF_HOST, "");
        String portPref = sharedPref.getString(MainActivity.KEY_PREF_PORT, "");
        MQTTHOST = "tcp://"+hostPref+":"+portPref;
        Toast.makeText(this, MQTTHOST.toString(),Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDatosHost();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        texserver = findViewById(R.id.textServer);
        textViewcoor = findViewById(R.id.textViewcoor);
        textchat = findViewById(R.id.textChat);
        textchat.setMovementMethod(new ScrollingMovementMethod());
        editTextTopic = findViewById(R.id.editTextTopic);
        editTextMensaje = findViewById(R.id.editTextMensaje);
        editTextMensaje.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMensaje();
                    handled = true;
                }
                return handled;
            }
        });

        configButton = findViewById(R.id.button_config);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });


        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        register();

        wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.getLocalClassName());

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        myRingTone = RingtoneManager.getRingtone(getApplicationContext(), uri);


        /*SharedPreferences mSharedPref = getSharedPreferences("HostConfig", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mSharedPref.edit();
        mEditor.putString("host", "ghghjgj");
        mEditor.apply();*/

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        refreshDatosHost();

        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);
        Toast.makeText(this,clientId.toString(),Toast.LENGTH_LONG).show();

        isConnected = false;


        // conecta con el broker
        connectToServer();


        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                if (topic.equals("dispositivos/chat")) {
                    textchat.append(new String(message.getPayload()) + "\n");
                    //textchat.setText(new String(message.getPayload())+"topic: "+ topic );
                    vibrator.vibrate(500);
                    myRingTone.play();
                } else if (topic.equals("$SYS/broker/clients/connected")) {
                    textchat.append(new String(message.getPayload()) + "\n");
                    Log.v(TAG, "indexdddd=" + message);
                } else if (topic == topicStr + "/sensor") {
                    Log.v(TAG, "indexssssssss=" + message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        ////////////////////////////////////////////////
        final ListView listview = findViewById(R.id.listviewTopics);
        String[] values = new String[]{"Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux",
                "OS/2", "Ubuntu", "Windows7", "Max OS X", "Linux", "OS/2",
                "Android", "iPhone", "WindowsMobile"};

        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; ++i) {
            list.add(values[i]);
        }
        final StableArrayAdapter adapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                /*final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(2000).alpha(0).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                list.remove(item);
                                adapter.notifyDataSetChanged();
                                view.setAlpha(1);
                            }
                });*/
            }

        });

    }



    public void pub(View v) {
        String topic = editTextTopic.getText().toString();
        String message = clientId+":--> "+editTextMensaje.getText().toString();

        if (isConnected) {
            try {

                client.publish(topic, message.getBytes(), 0, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "No estas conectado, no puedes publicar a:" + topicStr, Toast.LENGTH_LONG).show();
        }
    }

    public void sub(View v) {
        String topic = editTextTopic.getText().toString();
        if (isConnected) {
            try {
                client.subscribe(topic, 0);
                Toast.makeText(MainActivity.this, "Te has subscrito a:" + topic, Toast.LENGTH_LONG).show();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "No estas conectado, no se puede suscribir a:" + topic, Toast.LENGTH_LONG).show();
        }
    }

    public void unsub(View v) {
        String topic = editTextTopic.getText().toString();
        if (isConnected) {
            try {
                client.unsubscribe(topic);
                Toast.makeText(MainActivity.this, "Te has Unsubscrito de:" + topic, Toast.LENGTH_LONG).show();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "No estas conectado, no se puede Unsuscribir de:" + topic, Toast.LENGTH_LONG).show();
        }
    }

    public void sendMensaje(String topic, String message) {

        if (isConnected) {
            try {

                client.publish(topic, message.getBytes(), 0, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            // Toast.makeText(MainActivity.this, "No estas conectado, no puedes publicar a:"+topicStr, Toast.LENGTH_LONG ).show();
        }
    }

    public void sendMensaje() {
        String topic = editTextTopic.getText().toString();
        String message = editTextMensaje.getText().toString();
        if (isConnected) {
            try {

                client.publish(topic, message.getBytes(), 0, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            // Toast.makeText(MainActivity.this, "No estas conectado, no puedes publicar a:"+topicStr, Toast.LENGTH_LONG ).show();
        }
    }

    // Se subcribe a si misma y al grupo
    private void setSubscription() {
        if (isConnected) {
            try {
                client.subscribe("dispositivos/"+clientId+"/escucha/#", 0);
                client.subscribe("dispositivos/chat", 0);
                client.subscribe("$SYS/broker/clients/connected",0);
                //client.subscribe(topicStr+"/conexiones",0);

            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "No estas conectado, no se puede suscribir a:" + topicStr, Toast.LENGTH_LONG).show();
        }
    }

    public void connectToServer(){
        //MqttConnectOptions options = new MqttConnectOptions();
        //options.setUserName(USERNAME);
        //options.setPassword(PASSWORD.toCharArray());
        try {
            //IMqttToken token = client.connect(options);
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Conectado a: " +MQTTHOST.toString(), Toast.LENGTH_LONG).show();
                    isConnected = true;
                    texserver.setText("server online");
                    sendMensaje("conexiones/clients/id",clientId.toString());
                    setSubscription();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Conección falló a: " +MQTTHOST.toString(), Toast.LENGTH_LONG).show();
                    isConnected = false;
                    texserver.setText("server offline");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connect(View v) {
        connectToServer();
    }

    public void disconnect(View v) {
        if (isConnected) {
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
        } else {
            Toast.makeText(MainActivity.this, "Ya estas deconectado", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        acclX = sensorEvent.values[0];
        acclY = sensorEvent.values[1];
        acclZ = sensorEvent.values[2];
        String result = getAccelerometerReading();
        textViewcoor.setText(result);

        sendMensaje("dispositivos/"+clientId.toString()+"/sensores/acc",result);


        //Log.v(TAG, "dfffffff=" + acclX);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
}


