package com.example.iot;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SendStateMotorService extends Service {
    private MqttAndroidClient mqttAndroidClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mqttAndroidClient = ((IOTApplication) getApplication()).mqttAndroidClient;
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("STATE", false)) {
            publishMessage("1");
        } else {
            publishMessage("0");
        }
        return Service.START_STICKY;
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
}
