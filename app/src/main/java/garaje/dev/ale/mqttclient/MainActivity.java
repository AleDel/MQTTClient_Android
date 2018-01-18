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
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;


import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MyActivity_mqttclient";

    static String MQTTHOST = "tcp://0.tcp.ngrok.io:18538";
    //static String MQTTHOST = "tcp://192.168.0.165:1883";
    static String USERNAME = "";
    static String PASSWORD = "";

    String topicStr = "test/dispositivos";

    MqttAndroidClient client;

    /*TextView texserver;
    TextView textchat;
    TextView textViewcoor;
    EditText editTextTopic;
    EditText editTextMensaje;*/
    Button configButton;
    Switch connectswitch;
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

    protected MessagesListAdapter<Message> messagesAdapter;
    private Author autor;

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
        Toast.makeText(this, MQTTHOST.toString(),Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isConnected){
            disconnectFromServer();
        }

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

        configButton = findViewById(R.id.button_config);
        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        connectswitch = findViewById(R.id.switch_connect);
        connectswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    // The toggle is enabled
                    connectToServer();
                } else {
                    // The toggle is disabled
                    disconnectFromServer();
                }
            }
        });


        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //register();

        wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.getLocalClassName());
        //wakeLock.acquire();

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        myRingTone = RingtoneManager.getRingtone(getApplicationContext(), uri);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        refreshDatosHost();

        /////////////

        MessagesList messagesList = findViewById(R.id.messagesList);
        messagesAdapter = new MessagesListAdapter<>("0", null);
        messagesList.setAdapter(messagesAdapter);

        MessageInput input = findViewById(R.id.input);
        input.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(CharSequence input) {

                publish("dispositivos/chat",input.toString());
                //validate and send message
                //messagesAdapter.addToStart(message, true);
                return true;
            }
        });
        /////////////////

        isConnected = false;

        // conecta con el broker
        connectToServer();

    }

    private void createClientAutor(){
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);
        Toast.makeText(this,clientId.toString(),Toast.LENGTH_SHORT).show();
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                if (topic.equals("dispositivos/chat")) {
                    //textchat.append(new String(message.getPayload()) + "\n");

                    String randId = Long.toString(UUID.randomUUID().getLeastSignificantBits());
                    Message newmensaje = new Message(randId,autor,new String(message.getPayload()));
                    messagesAdapter.addToStart(newmensaje,true);
                    //textchat.setText(new String(message.getPayload())+"topic: "+ topic );
                    //vibrator.vibrate(500);
                    myRingTone.play();
                } else if (topic.equals("$SYS/broker/clients/connected")) {
                    //textchat.append(new String(message.getPayload()) + "\n");
                    Log.v(TAG, "indexdddd=" + message);
                } else if (topic == topicStr + "/sensor") {
                    Log.v(TAG, "indexssssssss=" + message);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        autor = new Author(clientId,clientId,null,true);
        Message newmensaje = new Message("1",autor,"hola");
        messagesAdapter.addToStart(newmensaje,true);
    }

    public void connectToServer(){
        //MqttConnectOptions options = new MqttConnectOptions();
        //options.setUserName(USERNAME);
        //options.setPassword(PASSWORD.toCharArray());
        createClientAutor();
        register(); //registra sensor
        try {
            //IMqttToken token = client.connect(options);
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Conectado a: " +MQTTHOST.toString(), Toast.LENGTH_SHORT).show();
                    //connectswitch.setChecked(true);
                    isConnected = true;
                    //texserver.setText("server online");
                    publish("conexiones/clients/id",clientId.toString());
                    setSubscription();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(MainActivity.this, "Conección falló a: " +MQTTHOST.toString(), Toast.LENGTH_SHORT).show();
                    isConnected = false;
                    //connectswitch.setChecked(false);
                    //texserver.setText("server offline");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnectFromServer() {
        if (isConnected) {
            try {

                publish("desconexiones/clients/id",clientId.toString());
                unregister(); //unregistra sensor
                IMqttToken token = client.disconnect();

                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Toast.makeText(MainActivity.this, "desconectado", Toast.LENGTH_SHORT).show();
                        isConnected = false;
                        client.close();

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Toast.makeText(MainActivity.this, "No puedo desconectar", Toast.LENGTH_SHORT).show();
                        isConnected = true;

                    }
                });/**/
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "Ya estas deconectado", Toast.LENGTH_SHORT).show();
        }

    }

    public void sub(String topic) {

        if (isConnected) {
            try {
                client.subscribe(topic, 0);
                Toast.makeText(MainActivity.this, "Te has subscrito a:" + topic, Toast.LENGTH_SHORT).show();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "No estas conectado, no se puede suscribir a:" + topic, Toast.LENGTH_SHORT).show();
        }
    }

    public void unsub(String topic) {
        if (isConnected) {
            try {
                client.unsubscribe(topic);
                Toast.makeText(MainActivity.this, "Te has Unsubscrito de:" + topic, Toast.LENGTH_SHORT).show();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MainActivity.this, "No estas conectado, no se puede Unsuscribir de:" + topic, Toast.LENGTH_SHORT).show();
        }
    }

    public void publish(String topic, String message) {

        if (isConnected) {
            try {

                client.publish(topic, message.getBytes(), 0, false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
             //Toast.makeText(MainActivity.this, "No estas conectado, no puedes publicar a:"+topic, Toast.LENGTH_SHORT ).show();
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
            Toast.makeText(MainActivity.this, "No estas conectado, no se puede suscribir a:" + topicStr, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        acclX = sensorEvent.values[0];
        acclY = sensorEvent.values[1];
        acclZ = sensorEvent.values[2];
        String result = getAccelerometerReading();
        //textViewcoor.setText(result);

        publish("dispositivos/"+clientId.toString()+"/sensores/acc",result);


        //Log.v(TAG, "dfffffff=" + acclX);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class Message implements IMessage {

        private String id;
        private String text;
        private Date createdAt;
        private Author author;

        public Message(String id, Author author, String text) {
            this(id, author, text, new Date());
        }

        public Message(String id, Author author, String text, Date createdAt) {
            this.id = id;
            this.text = text;
            this.author = author;
            this.createdAt = createdAt;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public Author getUser() {
            return author;
        }

        @Override
        public Date getCreatedAt() {
            return createdAt;
        }
    }

    public class Author implements IUser {

        private String id;
        private String name;
        private String avatar;
        private boolean online;

        public Author(String id, String name, String avatar, boolean online) {
            this.id = id;
            this.name = name;
            this.avatar = avatar;
            this.online = online;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAvatar() {
            return avatar;
        }

        public boolean isOnline() {
            return online;
        }
    }
}


