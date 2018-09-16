package net.dtdns.tqlap089.wirelesscarcontroller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    String TAG = "MAIN_ACTIVITY";
    MqttAndroidClient client;
    String serverURI = "tcp://broker.mqttdashboard.com:1883";
    String topic = "lap089/speed";
    String sendTopic = "lap089/motor";
    int currentSpeed = 210;
    int stepSpeed = 15;
    WebView cameraView;
    int QOS = 2;
    ImageButton carUp, carDown, carLeft, carRight, cameraUp, cameraDown, speedUp, speedDown;
    TextView speedTextView;
    ToggleButton lightToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cameraView = (WebView) findViewById(R.id.camera);
        carUp = (ImageButton) findViewById(R.id.imageUp);
        carDown = (ImageButton) findViewById(R.id.imageDown);
        carLeft = (ImageButton) findViewById(R.id.imageLeft);
        carRight = (ImageButton) findViewById(R.id.imageRight);
        speedDown = (ImageButton) findViewById(R.id.speedDown);
        speedUp = (ImageButton) findViewById(R.id.speedUp);
        cameraDown = (ImageButton) findViewById(R.id.cameraDown);
        cameraUp = (ImageButton) findViewById(R.id.cameraUp);
        speedTextView = (TextView) findViewById(R.id.speedMotor);
        lightToggle = (ToggleButton) findViewById(R.id.toggleLed);

        speedTextView.setText(currentSpeed + "/255");

        cameraView.getSettings().setJavaScriptEnabled(true);
        cameraView.setInitialScale(1);
        cameraView.getSettings().setLoadWithOverviewMode(true);
        cameraView.getSettings().setUseWideViewPort(true);
        cameraView.getSettings().setAppCacheEnabled(false);
        cameraView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        cameraView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // chromium, enable hardware acceleration
//            cameraView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
//        } else {
//            // older android version, disable hardware acceleration
//            cameraView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        }
        cameraView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        final Activity activity = this;
        cameraView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                activity.setProgress(progress * 1000);
            }
        });
        cameraView.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

            }
        });


        View.OnTouchListener motionHandler = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        actionOnTouch(v);
                        return false;
                    case MotionEvent.ACTION_UP:
                        publishToTopic(sendTopic, "0");
                        return false;
                }
                return false;
            }
        };

        View.OnTouchListener cameraHandler = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cameraOnTouch(v);
                        return false;
                    case MotionEvent.ACTION_UP:
                        publishToTopic(sendTopic, "7");
                        return false;
                }
                return false;
            }
        };

        speedUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                publishToTopic(sendTopic, "+s");
                currentSpeed += currentSpeed < 225 ? stepSpeed : 0;
                speedTextView.setText(currentSpeed + "/255");
            }
        });

        speedDown.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                publishToTopic(sendTopic, "-s");
                currentSpeed -= currentSpeed > stepSpeed * 2 ? stepSpeed : 0;
                speedTextView.setText(currentSpeed + "/255");
            }
        });

        lightToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (lightToggle.isChecked()) {
                    publishToTopic(sendTopic, "8");
                } else {
                    publishToTopic(sendTopic, "-8");
                }
            }
        });

        cameraDown.setOnTouchListener(cameraHandler);
        cameraUp.setOnTouchListener(cameraHandler);
        carUp.setOnTouchListener(motionHandler);
        carDown.setOnTouchListener(motionHandler);
        carLeft.setOnTouchListener(motionHandler);
        carRight.setOnTouchListener(motionHandler);

        Log.d(TAG, "Start MQTT connection");
        new MqttClientAsync(this).execute();

    }

    public void actionOnTouch(View v) {
        switch (v.getId()) {
            case R.id.imageUp:
                publishToTopic(sendTopic, "1");
                break;
            case R.id.imageDown:
                publishToTopic(sendTopic, "2");
                break;
            case R.id.imageLeft:
                publishToTopic(sendTopic, "3");
                break;
            case R.id.imageRight:
                publishToTopic(sendTopic, "4");
                break;
        }
    }

    public void cameraOnTouch(View v) {
        switch (v.getId()) {
            case R.id.cameraUp:
                publishToTopic(sendTopic, "5");
                break;
            case R.id.cameraDown:
                publishToTopic(sendTopic, "6");
                break;
        }
    }

    public void subscribeToTopic() {
        try {
            client.subscribe(topic, QOS, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            client.subscribe(topic, QOS, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    processReceivedData(topic, message);
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void processReceivedData(String topic, MqttMessage message) {
        String data = new String(message.getPayload()).trim();
        Log.d(TAG, "Message Arrived: " + topic + " : " + data);
        try {
            currentSpeed = Integer.valueOf(data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishToTopic(final String sendTopic, final String payload) {
        final byte[][] encodedPayload = new byte[1][1];
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    encodedPayload[0] = payload.getBytes("UTF-8");
                    MqttMessage message = new MqttMessage(encodedPayload[0]);
                    client.publish(sendTopic, message);
                } catch (UnsupportedEncodingException | MqttException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reload) {
            //cameraView.loadUrl("https://youtube.com");
            cameraView.reload();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class MqttClientAsync extends AsyncTask<Void, Void, Void> {

        private ProgressDialog mProgress;
        private Context context;

        MqttClientAsync(Context context) {
            this.context = context;
            mProgress = new ProgressDialog(context);
        }

        @Override
        protected Void doInBackground(Void... params) {
            String clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(context, serverURI, clientId);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        Log.d(TAG, "Reconnected to : " + serverURI);
                        // Because Clean Session is true, we need to re-subscribe
                        subscribeToTopic();
                    } else {
                        Log.d(TAG, "Connected to: " + serverURI);
                        //
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "The Connection was lost.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    processReceivedData(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(true);

            try {
                IMqttToken token = client.connect(mqttConnectOptions);
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        Log.d(TAG, "onSuccess");

                        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                        disconnectedBufferOptions.setBufferEnabled(true);
                        disconnectedBufferOptions.setBufferSize(100);
                        disconnectedBufferOptions.setPersistBuffer(false);
                        disconnectedBufferOptions.setDeleteOldestMessages(false);
                        client.setBufferOpts(disconnectedBufferOptions);
                        subscribeToTopic();

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        Log.e(TAG, "onFailure");

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPreExecute() {
            mProgress.setTitle("Connecting...");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Void value) {
            Log.d(TAG, "Start loading URL");
            cameraView.loadUrl("http://192.168.10.1:8080/?action=stream");
            mProgress.dismiss();
        }

    }
}
