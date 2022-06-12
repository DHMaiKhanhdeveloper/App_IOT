package com.example.iot;

import android.app.Application;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

public class IOTApplication extends Application {
    String clientId = MqttClient.generateClientId();

    MqttAndroidClient mqttAndroidClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mqttAndroidClient = new MqttAndroidClient(
                getApplicationContext(),
                "tcp://13.229.70.147:1883",
                clientId
        );
    }
}
