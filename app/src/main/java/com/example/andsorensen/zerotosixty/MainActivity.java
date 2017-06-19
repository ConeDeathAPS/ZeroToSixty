package com.example.andsorensen.zerotosixty;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;

import java.util.List;


public class MainActivity extends Activity implements SensorEventListener {

    public static String appLabel = "zeroToSixty";
    //Init variables for holding values to display in UI
    public long startTime;
    public float elapsedTime;
    public float seconds;
    public float xAccel;
    public float yAccel;
    public float zAccel;
    public float xAccelSquared;
    public float yAccelSquared;
    public float zAccelSquared;
    public boolean xIsNeg;
    public boolean yIsNeg;
    public boolean zIsNeg;
    public float xSpeed;
    public float ySpeed;
    public float zSpeed;
    public float timeToSixty;
    public float maxAccel;
    public float cAccel;
    public float accel;
    public float speed;
    public float xOff = 0;
    public float yOff = 0;
    public float zOff = 0;
    //Float arrays used for orientation/acceleration results
    public float[] accelerations;
    public float[] linearAccelerations; //Can possibly get rid of this
    public float[] magneticOrientation;
    public float[] rArr = new float[9];
    public float[] orientation = new float[3];
    public float[] weight = new float[2];
    //Variables for holding instances of sensor classes
    private SensorManager mSensorManager;
    private Sensor LinearAccelerometer;
    //Variables for holding instances of UI components
    private TextView xAccelOutput;
    private TextView yAccelOutput;
    private TextView zAccelOutput;
    private TextView xSpeedOutput;
    private TextView ySpeedOutput;
    private TextView zSpeedOutput;
    private TextView accelOutput;
    private TextView maxAccelOutput;
    private TextView timeOutput;
    private TextView timeToSixtyOutput;
    private TextView speedOutput;
    private Button startButton;
    //Variables for monitoring status of app and timer
    private Handler timeHandler = new Handler();
    private boolean isRunning = false;

    //=========================== BEGIN APPLICATION INIT CODE ==========================//
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set local variables to their respective UI components;
        xAccelOutput = (TextView) findViewById(R.id.xAccel);
        yAccelOutput = (TextView) findViewById(R.id.yAccel);
        zAccelOutput = (TextView) findViewById(R.id.zAccel);
        xSpeedOutput = (TextView) findViewById(R.id.xSpeed);
        ySpeedOutput = (TextView) findViewById(R.id.ySpeed);
        zSpeedOutput = (TextView) findViewById(R.id.zSpeed);
        accelOutput = (TextView) findViewById(R.id.accelValue);
        maxAccelOutput = (TextView) findViewById(R.id.maxAccelValue);
        timeOutput = (TextView) findViewById(R.id.timerValue);
        timeToSixtyOutput = (TextView) findViewById(R.id.timeToSixtyValue);
        speedOutput = (TextView) findViewById(R.id.speedValue);
        startButton = (Button) findViewById(R.id.goButton);

        //Init the sensor manager and get a list of all sensors present on the device.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.i(appLabel, "All Sensors:"+allSensors.toString());

        //Check for required sensors
        LinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (LinearAccelerometer != null) {
            Log.i(appLabel, "Linear Accelerometer detected of type:"+LinearAccelerometer.getName());
            mSensorManager.registerListener(this, LinearAccelerometer, 100000);
        } else {
            Log.i(appLabel, "No linear accelerometer found.");
        }
    }
    //======================= END APPLICATION INIT CODE =======================//
    //===================== BEGIN APPLICATION CONTROL CODE ====================//

    //Rounding utility function
    public static double round(double val, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        val = val*factor;
        long tmp = Math.round(val);
        return (double) tmp/factor;
    }

    //Converts radians to degrees
    public static float radToDeg(float rad) {
        return (float) (rad*(180/Math.PI));
    }

    //Convert meters per second to miles per hour
    public static double mpsToMph(double speed) {
        double feetPerSecond = speed*3.28084;
        double milesPerSecond = feetPerSecond/5280;
        double milesPerHour = milesPerSecond*3600;
        return round(milesPerHour, 2);
    }

    public void calibrateAccelerometer(View view) {
        xOff = linearAccelerations[0];
        yOff = linearAccelerations[1];
        zOff = linearAccelerations[2];
    }

    //Starts the timer if it isn't already running
    public void beginTimer(View view) {
        if (!isRunning) {
            isRunning = true;
            startButton.setText(R.string.stop_text);
            startTime = System.currentTimeMillis();
            timeHandler.removeCallbacks(startTimer);
            timeHandler.postDelayed(startTimer, 0);
        } else {
            startButton.setText(R.string.go_button);
            stopTimer();
        }
    }

    //Resets the timer, speed, and acceleration
    public void resetTimer(View view) {
        elapsedTime = 0;
        speed = 0;
        xSpeed = 0;
        ySpeed = 0;
        zSpeed = 0;
        maxAccel = 0;
        timeToSixty = 0;
        xSpeedOutput.setText(String.valueOf(xSpeed));
        ySpeedOutput.setText(String.valueOf(ySpeed));
        zSpeedOutput.setText(String.valueOf(zSpeed));
        maxAccelOutput.setText(null);
        timeToSixtyOutput.setText(null);
        speedOutput.setText(null);
        timeOutput.setText(null);
    }

    //The actual function that stops the timer. Cannot be called from the UI.
    public void stopTimer() {
        maxAccelOutput.setText(String.valueOf(maxAccel));
        timeHandler.removeCallbacks(startTimer);
        startButton.setText(R.string.go_button);
        isRunning = false;
    }

    //Update the value of the timer
    private void updateTimer(float eTime) {
        seconds = (float) round((eTime / 1000), 2);
        timeOutput.setText(String.valueOf(seconds));
    }

    //This is the function that actually runs the timer
    private Runnable startTimer = new Runnable() {
        public void run() {
            elapsedTime = System.currentTimeMillis() - startTime;
            updateTimer(elapsedTime);
            timeHandler.postDelayed(this, 100);
        }
    };

    //Determine overall velocity from individual acceleration vectors
    //Updates the current speed
    private void setSpeed() {
        double cVel = cAccel*0.1;
        speed += round(mpsToMph(cVel), 2);
        speedOutput.setText(String.valueOf(speed));
    }
    //===================== END APPLICATION CONTROL CODE ==================//
    //===================== BEGIN SENSOR EVENT HANDLERS ===================//

    //Do this if the sensor's accuracy changes
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(appLabel, "Accuracy set to:"+accuracy);
    }

    //This is executed every time a reading is captured from the sensor
    @Override
    public final void onSensorChanged(SensorEvent event) {
        linearAccelerations = event.values;
        linearAccelerations[0] = linearAccelerations[0]-xOff;
        linearAccelerations[1] = linearAccelerations[1]-yOff;
        linearAccelerations[2] = linearAccelerations[2]-zOff;
        xAccel = (float) round(linearAccelerations[0]/9.8, 3);
        yAccel = (float) round(linearAccelerations[1]/9.8, 3);
        zAccel = (float) round(linearAccelerations[2]/9.8, 3);
//        Log.i(appLabel, "Acceleration: ("+xAccel+", "+yAccel+", "+zAccel+")");
        xAccelSquared = (float) round(xAccel*xAccel, 3);
        yAccelSquared = (float) round(yAccel*yAccel, 3);
        zAccelSquared = (float) round(zAccel*zAccel, 3);
        //If they are negative accelerations, squaring them will have made them positive
        if (xAccel < 0) {
            xAccelSquared = xAccelSquared*-1;
        }
        if (yAccel < 0) {
            yAccelSquared = yAccelSquared*-1;
        }
        if (zAccel < 0) {
            zAccelSquared = zAccelSquared*-1;
        }
        xAccelOutput.setText(String.valueOf(xAccel));
        yAccelOutput.setText(String.valueOf(yAccel));
        zAccelOutput.setText(String.valueOf(zAccel));
//        Log.i(appLabel, "Acceleration squares: "+xAccelSquared+", "+yAccelSquared+", "+zAccelSquared);
        float accelsSum = xAccelSquared+yAccelSquared+zAccelSquared;
        double accelsRoot;
        if (accelsSum < 0) {
            accelsSum = accelsSum * -1;
            accelsRoot = Math.sqrt(accelsSum) * -1;
        } else {
            accelsRoot = Math.sqrt(accelsSum);
        }
//        Log.i(appLabel, "Acceleration squares sum: "+accelsSum);
//        Log.i(appLabel, "Acceleration sqrt:"+accelsRoot);
        cAccel = (float) round(accelsRoot, 3);
//        Log.i(appLabel, "Net Acceleration: "+cAccel);
        //Store the maximum acceleration
        if (cAccel > maxAccel) {
            maxAccel = cAccel;
        }
        accelOutput.setText(String.valueOf(cAccel));
        if (isRunning) {
            setSpeed();
        }
        if (speed >= 60) {
            timeToSixty = seconds;
            timeToSixtyOutput.setText(String.valueOf(timeToSixty));
            stopTimer();
        }
    }

    //Called when the app is resumed
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, LinearAccelerometer, 75000);
    }

    //Called when the app is paused
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    //===================== END SENSOR EVENT HANDLERS ===================//

}
