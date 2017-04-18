package com.philips.lighting.quickstart;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

/**
 * MyApplicationActivity - The starting point for creating your own Hue App.  
 * Currently contains a simple view with a button to change your lights to random colours.  Remove this and add your own app implementation here! Have fun!
 * 
 * @author SteveyO
 *
 */
public class MyApplicationActivity extends Activity {

    private PHHueSDK phHueSDK;
    private static final int MAX_HUE=254;
    public static final String TAG = "QuickStart";
    private final int index = 1;
    private RadioButton normal, auto, alarm;
    private SeekBar mSeekBar;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private String mode = "normal";
    private TextView mdisplay;
    private Button setButton, confirmButton;
    private EditText hourText, minText, secText;
    private int hour = 0, min = 0, sec = 0;
    private ToggleButton mSwitch;
    private boolean Switch;
    private CountDownTimer timer;


    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        phHueSDK = PHHueSDK.create();

        RadioGroup rg = (RadioGroup) findViewById(R.id.radioGroup);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                System.out.println("checkedId:" + checkedId);
                System.out.println("radioButton:" + R.id.radioButton2);
                switch (checkedId) {
                    case R.id.radioButton:
                        mode = "normal";
                        break;
                    case R.id.radioButton2:
                        mode = "auto";
                        break;
                    case R.id.radioButton3:
                        mode = "alarm";
                        break;
                }
                //System.out.println("mode:" + mode);
                modeSwitch();
            }
        });



        // normal mode
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);


        // auto mode
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mdisplay = (TextView) findViewById(R.id.textLight);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // alarm mode
        setButton = (Button) findViewById(R.id.set);
        confirmButton = (Button) findViewById(R.id.done);
        hourText = (EditText) findViewById(R.id.hours);
        minText = (EditText) findViewById(R.id.minutes);
        secText = (EditText) findViewById(R.id.seconds);
        hourText.setEnabled(false);
        minText.setEnabled(false);
        secText.setEnabled(false);

        mSwitch = (ToggleButton) findViewById(R.id.start);
        mSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Switch = mSwitch.isChecked();
                //alarmFunction();
                System.out.println("Switch:" + Switch);
                if(Switch && mode.equals("alarm")) {
                    setTimerTask();
                } else {
                  timer.cancel();
                    setLights(0);
                }
            }
        });
    }

    public void onResume() {
        super.onResume();
        modeSwitch();
    }

    /**
     * Mode control
     */
    private void modeSwitch(){
        System.out.println("mode:" + mode);
        if(mode.equals("normal")) {
            mSensorManager.unregisterListener(lightListener);
            mdisplay.setText("-- lux");
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int progressChangedValue = 0;
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    System.out.println("progress:" + progress);
                    progressChangedValue = (int)((MAX_HUE - 0)/10 * progress);
                    System.out.println("progressChangedValue:" + progressChangedValue);
                    setLights(progressChangedValue);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Toast.makeText(MyApplicationActivity.this, "Seek bar progress is :" + progressChangedValue,
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else if(mode.equals("auto")) {
            mSensorManager.registerListener(lightListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            mSensorManager.unregisterListener(lightListener);
            mdisplay.setText("-- lux");
            setLights(0);

            setButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(timer != null) {
                        timer.cancel();
                    }
                    hourText.setEnabled(true);
                    minText.setEnabled(true);
                    secText.setEnabled(true);
                }
            });
            confirmButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(timer != null) {
                        timer.cancel();
                    }
                    hourText.setEnabled(false);
                    minText.setEnabled(false);
                    secText.setEnabled(false);
                    hour = Integer.parseInt(hourText.getText().toString());
                    min = Integer.parseInt(minText.getText().toString());
                    sec = Integer.parseInt(secText.getText().toString());
                    System.out.println("hour:" + hour + " min:" + min + " sec:" + sec);
                    if(hour > 24 || min > 59 || sec > 59) {
                        Toast.makeText(MyApplicationActivity.this, "Invalid Input",
                                Toast.LENGTH_SHORT).show();
                    }
                    System.out.println("switch:" + mSwitch.isChecked());
                    if(mSwitch.isChecked()) {
                      setTimerTask();
                    }
                }
            });
        }
    }

    /**
     * Alarm control
     */

    private void setTimerTask() {
        setLights(0);
        long totalSec = hour * 60 * 60 + min * 60 + sec;
        timer = new CountDownTimer(totalSec * 1000, 1000) {
            long sec = 0;
            public void onTick(long millisUntilFinished) {
                System.out.println("start timer");
                long tick = millisUntilFinished;

                secText.setText(String.valueOf((tick / 1000) % 60));
                minText.setText(String.valueOf((tick / 1000) / 60));
                hourText.setText(String.valueOf((tick / 1000) / 3600));
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                setLights(MAX_HUE);
            }

        }.start();
    }

    /**
     * Light sensor detection & compensation
     */
    public SensorEventListener lightListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) { }

        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            mdisplay.setText((float)x + " lux");
            setLights(0);
            if(x < 100) {
              int progressChangedValue = (int)((MAX_HUE - 0)/10 * 10);
              setLights(progressChangedValue);
            }
        }

    };

    /**
     * Bulb control
     * @param progressChangedValue
     */
    private void setLights(int progressChangedValue) {
        PHBridge bridge = phHueSDK.getSelectedBridge();

        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        //for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setBrightness(progressChangedValue);

            // To validate your lightstate is valid (before sending to the bridge) you can use:
            // String validState = lightState.validateState();
            bridge.updateLightState(allLights.get(index), lightState, listener);
            //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
        //}
    }


    // If you want to handle the response from the bridge, create a PHLightListener object.
    PHLightListener listener = new PHLightListener() {
        
        @Override
        public void onSuccess() {  
        }
        
        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
           Log.w(TAG, "Light has updated");
        }
        
        @Override
        public void onError(int arg0, String arg1) {}

        @Override
        public void onReceivingLightDetails(PHLight arg0) {}

        @Override
        public void onReceivingLights(List<PHBridgeResource> arg0) {}

        @Override
        public void onSearchComplete() {}
    };
    
    @Override
    protected void onDestroy() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge != null) {
            
            if (phHueSDK.isHeartbeatEnabled(bridge)) {
                phHueSDK.disableHeartbeat(bridge);
            }
            
            phHueSDK.disconnect(bridge);
            super.onDestroy();
        }
    }
}
