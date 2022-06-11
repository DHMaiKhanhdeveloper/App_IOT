package com.example.iot;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private TextView textView_startTime;
    private TextView textView_endTime;
    private TextView textView_temperature;
    private TextView textView_airhumidity;
    private TextView textViewlight;
    private TextView textViewsoilmoisture;
    private SwitchCompat switchCompatButton;
    int hour, minute;
    final private String[] list_topic = new String[4];
    final private int[] qos = {0, 0, 0, 0};
    String clientId = MqttClient.generateClientId();

    private MqttAndroidClient mqttAndroidClient;
    IMqttToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        textView_startTime = findViewById(R.id.tv_startTime);
        textView_endTime = findViewById(R.id.tv_endTime);
        TextView textView_link = findViewById(R.id.tv_link_grafana);
        textView_temperature = findViewById(R.id.tv_temperature);
        textView_airhumidity = findViewById(R.id.tvair_humidity);
        textViewlight = findViewById(R.id.tv_light);
        textViewsoilmoisture= findViewById(R.id.tv_soil_moisture);
        switchCompatButton = findViewById(R.id.switchButton);

        textView_link.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://13.229.70.147:3000/"));
            startActivity(browserIntent);
        });

        switchCompatButton.setOnClickListener(v -> {
            if(switchCompatButton.isChecked()){

            }else{
                Toast.makeText(MainActivity.this,"onFailure",Toast.LENGTH_LONG).show();
            }
        });



        ConnectMQTT();

        list_topic[0] = "sensors/soil";
        list_topic[1] = "sensors/temperature";
        list_topic[2] = "sensors/humidity";
        list_topic[3] = "sensors/lux";

    }

    public void ConnectMQTT(){

        mqttAndroidClient = new MqttAndroidClient(
                this.getApplicationContext(),
                "tcp://13.229.70.147:1883",
                clientId
        );

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i("mqtt", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("mqtt", "topic: " + topic + ", msg: " + new String(message.getPayload()));
                if(topic.equals(list_topic[0])){

                    JSONObject js_data = new JSONObject(new String(message.getPayload()));
                    String text = js_data.getDouble("soil") + " %";
                    textViewsoilmoisture.setText(text);

                }else if(topic.equals(list_topic[1])){

                    JSONObject js_data = new JSONObject(new String(message.getPayload()));
                    String text = js_data.getDouble("temperature") + " °C";
                    textView_temperature.setText(text);

                }else if(topic.equals(list_topic[2])){

                    JSONObject js_data = new JSONObject(new String(message.getPayload()));
                    String text = js_data.getDouble("humidity") + " %";
                    textView_airhumidity.setText(text);

                }else if(topic.equals(list_topic[3])){

                    JSONObject js_data = new JSONObject(new String(message.getPayload()));
                    String text = js_data.getDouble("lux") + " lux";
                    textViewlight.setText(text);

                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("mqtt", "msg delivered");
            }
        });


        try {

            token = mqttAndroidClient.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("mqtt", "connect onSuccess");
                    if (mqttAndroidClient.isConnected()) subscribe();
                    else Log.d("mqtt", "connect Fail");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("mqtt", "connect onFailure");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }


    }


    private boolean isConnectToMQTT(){
        if (mqttAndroidClient.isConnected()) return true;
        try {
            mqttAndroidClient.connect();

        } catch (MqttException e) {
            Log.d("mqtt", "reconnect false");
            e.printStackTrace();
        }
         return mqttAndroidClient.isConnected();
    }


    public void PublishMessage( String payload){
//        String topic = "foo/bar";
//        String payload = "the payload";
//        byte[] encodedPayload = new byte[0];
//        try {
//            encodedPayload = payload.getBytes("UTF-8");
//            MqttMessage message = new MqttMessage(encodedPayload);
//            message.setRetained(true);
//            mqttAndroidClient.publish(topic, message);
//        } catch (UnsupportedEncodingException | MqttException e) {
//            e.printStackTrace();
//        }
        String PUB_TOPIC = "android/on";
        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);
            mqttAndroidClient.publish(PUB_TOPIC, message,null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("publish", "publish succeed!") ;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("publish", "publish failed!") ;
                }
            });
        } catch (MqttException e) {
            Log.d("subscribe_failed", e.toString());
            e.printStackTrace();
        }

    }



    public void subscribe(){

        try {
            IMqttToken subToken = mqttAndroidClient.subscribe(list_topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("mqtt", "subscribe onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("mqtt", "subscribe onFailure");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }



    public void Disconnected(){
        try {
            IMqttToken disconToken = mqttAndroidClient.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void popTimePicker1(View view) {
        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view1, hourOfDay, minuteOfDay) -> {
            hour = hourOfDay;
            minute = minuteOfDay;
            textView_startTime.setText(String.format(Locale.getDefault(),"%02d:%02d",hour,minute));

        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,onTimeSetListener,hour,minute,true);

        timePickerDialog.setTitle("Select time");
        timePickerDialog.show();
    }



    public void popTimePicker2(View view) {
        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view1, hourOfDay, minuteOfDay) -> {
            hour = hourOfDay;
            minute = minuteOfDay;
            textView_endTime.setText(String.format(Locale.getDefault(),"%02d:%02d",hour,minute));
        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,onTimeSetListener,hour,minute,true);


        timePickerDialog.setTitle("Select time");
        timePickerDialog.show();
    }



}