package com.example.iot;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

import java.util.Calendar;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private TextView textView_startTime;
    private TextView textView_endTime;
    private TextView textView_temperature;
    private TextView textView_airhumidity;
    private TextView textViewlight;
    private TextView textViewsoilmoisture;
    private TextView textView_link;
    private SwitchCompat switchCompatButton;
    final private String[] list_topic = new String[4];
    final private int[] qos = {0, 0, 0, 0};
    final private Calendar startTime = Calendar.getInstance();
    final private Calendar endTime = Calendar.getInstance();

    private MqttAndroidClient mqttAndroidClient;
    IMqttToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startTime.set(Calendar.MILLISECOND, 0);
        endTime.set(Calendar.MILLISECOND, 0);
        startTime.set(Calendar.SECOND, 0);
        endTime.set(Calendar.SECOND, 0);

        textView_startTime = findViewById(R.id.tv_startTime);
        textView_endTime = findViewById(R.id.tv_endTime);
        textView_link = findViewById(R.id.tv_link_grafana);
        textView_temperature = findViewById(R.id.tv_temperature);
        textView_airhumidity = findViewById(R.id.tvair_humidity);
        textViewlight = findViewById(R.id.tv_light);
        textViewsoilmoisture = findViewById(R.id.tv_soil_moisture);
        switchCompatButton = findViewById(R.id.switchButton);

        textView_link.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://13.229.70.147:3000/"));
            startActivity(browserIntent);
        });


        ConnectMQTT();

        list_topic[0] = "sensors/soil";
        list_topic[1] = "sensors/temperature";
        list_topic[2] = "sensors/humidity";
        list_topic[3] = "sensors/lux";
    }


    public void ConnectMQTT() {
        mqttAndroidClient = ((IOTApplication) getApplication()).mqttAndroidClient;

        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i("mqtt", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d("mqtt", "topic: " + topic + ", msg: " + new String(message.getPayload()));
                if (topic.equals(list_topic[0])) {

                    JSONObject js_data = new JSONObject(new String(message.getPayload()));
                    String text = js_data.getDouble("soil") + " %";
                    textViewsoilmoisture.setText(text);

                } else if (topic.equals(list_topic[1])) {

                    JSONObject js_data = new JSONObject(new String(message.getPayload()));
                    String text = js_data.getDouble("temperature") + " °C";
                    textView_temperature.setText(text);

                } else if (topic.equals(list_topic[2])) {

                    JSONObject js_data = new JSONObject(new String(message.getPayload()));
                    String text = js_data.getDouble("humidity") + " %";
                    textView_airhumidity.setText(text);

                } else if (topic.equals(list_topic[3])) {

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
                    if (mqttAndroidClient.isConnected()) {
                        subscribe();
                        publishSuccess();
                    } else Log.d("mqtt", "connect Fail");
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

    private void publishSuccess() {
        switchCompatButton.setOnClickListener(v -> {
            if (switchCompatButton.isChecked()) {
                JSONObject jsonParam = new JSONObject();
                publishMessage("1");
                Log.d("checkpublish", "publishsuccess");
            } else {

                publishMessage("0");
                Log.d("checkpublish", "publishfailed");

            }
        });
    }

    private boolean isConnectToMQTT() {
        if (mqttAndroidClient.isConnected()) return true;
        try {
            mqttAndroidClient.connect();

        } catch (MqttException e) {
            Log.d("mqtt", "reconnect false");
            e.printStackTrace();
        }
        return mqttAndroidClient.isConnected();
    }


    public void publishMessage(String payload) {

        String PUB_TOPIC = "motor/turn";
        try {
            if (!mqttAndroidClient.isConnected()) {
                mqttAndroidClient.connect();
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setQos(0);

            mqttAndroidClient.publish(PUB_TOPIC, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("publish succeed", payload);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("publish", "publish failed!");
                }
            });
        } catch (MqttException e) {
            Log.d("subscribe_failed", e.toString());
            e.printStackTrace();
        }

    }


    public void subscribe() {

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


    public void Disconnected() {
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
        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view1, hourOfDay, minute) -> {
            startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            startTime.set(Calendar.MINUTE, minute);
            textView_startTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            sendStartState(hourOfDay, minute, true);
        };

        int hour = startTime.get(Calendar.HOUR_OF_DAY);
        int minute = startTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, onTimeSetListener, hour, minute, true);

        timePickerDialog.setTitle("Select time");
        timePickerDialog.show();
    }

    public void popTimePicker2(View view) {
        TimePickerDialog.OnTimeSetListener onTimeSetListener = (view1, hourOfDay, minute) -> {
            Calendar result = (Calendar) endTime.clone();
            result.set(Calendar.HOUR_OF_DAY, hourOfDay);
            result.set(Calendar.MINUTE, minute);

            if (startTime.compareTo(result) >= 0) {
                Log.e("Error", "Start time > end time");
            } else {
                endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                endTime.set(Calendar.MINUTE, minute);
                textView_endTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                sendStartState(hourOfDay, minute, false);
            }
        };

        int hour = endTime.get(Calendar.HOUR_OF_DAY);
        int minute = endTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, onTimeSetListener, hour, minute, true);


        timePickerDialog.setTitle("Select time");
        timePickerDialog.show();
    }

    private void sendStartState(int hourOfDay, int minute, boolean state) {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinutes = calendar.get(Calendar.MINUTE);
        int currentDate = calendar.get(Calendar.DATE);

        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (hourOfDay < currentHour || (hourOfDay == currentHour && minute <= currentMinutes)) {
            calendar.set(Calendar.DATE, currentDate + 1);
        }

        long timeLong = calendar.getTimeInMillis();

        Intent intent = new Intent(getApplicationContext(), SendStateMotorService.class);
        intent.putExtra("STATE", state);
        PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(),
                state ? 5 : 6,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeLong,
                    pendingIntent
            );
        }
    }
}