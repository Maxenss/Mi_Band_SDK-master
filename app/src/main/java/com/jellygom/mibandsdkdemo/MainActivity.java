package com.jellygom.mibandsdkdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jellygom.miband_sdk.MiBandIO.Listener.HeartrateListener;
import com.jellygom.miband_sdk.MiBandIO.Listener.NotifyListener;
import com.jellygom.miband_sdk.MiBandIO.Listener.RealtimeStepListener;
import com.jellygom.miband_sdk.MiBandIO.MibandCallback;
import com.jellygom.miband_sdk.MiBandIO.Model.UserInfo;
import com.jellygom.miband_sdk.Miband;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TAG";

    private Miband miband;
    private BluetoothAdapter mBluetoothAdapter;

    private TextView heart, step, battery;
    private TextView text;

    // Количество измерений
    private int count = 0;
    private int critical1 = 100;
    private int critical2 = 160;
    private int critical3 = 180;
    private int[] arrayWithHeartRate = new int[3];

    // Метод для получения среднего значение
    private void getAverage(int heartRate) {
        if (heartRate == 0) return;

        arrayWithHeartRate[count] = heartRate;
        ++count;

        if (count == 3) {
            int average = (arrayWithHeartRate[0]
                    + arrayWithHeartRate[1] + arrayWithHeartRate[2]) / 3;
            count = 0;
            average -= 5;

            Date date = new Date();
            int hoursD = date.getHours();
            int minutesD = date.getMinutes();
            int secD = date.getSeconds();

            String hours = hoursD < 10 ? ("0" + hoursD) : (String.valueOf(hoursD));
            String minutes = minutesD < 10 ? ("0" + minutesD) : (String.valueOf(minutesD));
            String sec = secD < 10 ? ("0" + secD) : (String.valueOf(secD));

            String time = hours + ":" + minutes + ":" + sec;

            text.append(time + " - Пульс : " + String.valueOf(average) + " ударов/минута\n");
            heart.setText(String.valueOf(average) + " ударов/минута");

            if (average <= critical1)
                Toast.makeText(this, "Пульс в пределах нормы", Toast.LENGTH_SHORT).show();
            if (average >= critical1 && average < critical2) {
                Toast.makeText(this, "Пульс выше нормы", Toast.LENGTH_SHORT).show();
            } else if (average >= critical2 && average < critical3) {
                Toast.makeText(this, "Опасный уровень пульса. Остановите машину!", Toast.LENGTH_SHORT).show();
            } else if (average >= critical3) {
                Toast.makeText(this, "Пульс достиг критической отметки. Машина остановлена!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private RealtimeStepListener realtimeStepListener = new RealtimeStepListener() {
        @Override
        public void onNotify(final int steps) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    step.setText(steps + " steps");
                    text.append(steps + " steps\n");
                }
            });
        }
    };

    private HeartrateListener heartrateNotifyListener = new HeartrateListener() {
        @Override
        public void onNotify(final int heartRate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //  heart.setText(heartRate + " ударов/минута");
                    //  text.append(heartRate + " ударов/минута\n");
                    getAverage(heartRate);
                    miband.startHeartRateScan(0, mibandCallback);
                }
            });
        }
    };

    private final MibandCallback mibandCallback = new MibandCallback() {
        @Override
        public void onSuccess(Object data, int status) {
            switch (status) {
                case MibandCallback.STATUS_SEARCH_DEVICE:
                    Log.e(TAG, "Success: STATUS_SEARCH_DEVICE");
                    miband.connect((BluetoothDevice) data, this);
                    break;
                case MibandCallback.STATUS_CONNECT:
                    Log.e(TAG, "Success: STATUS_CONNECT");
                    miband.getUserInfo(this);
                    break;
                case MibandCallback.STATUS_SEND_ALERT:
                    Log.e(TAG, "Success: STATUS_SEND_ALERT");
                    break;
                case MibandCallback.STATUS_GET_USERINFO:
                    Log.e(TAG, "Success: STATUS_GET_USERINFO");
                    UserInfo userInfo = new UserInfo().fromByteData(((BluetoothGattCharacteristic) data).getValue());
                    miband.setUserInfo(userInfo, this);
                    break;
                case MibandCallback.STATUS_SET_USERINFO:
                    Log.e(TAG, "Success: STATUS_SET_USERINFO");
                    miband.setHeartRateScanListener(heartrateNotifyListener);
                    break;
                case MibandCallback.STATUS_START_HEARTRATE_SCAN:
                    Log.e(TAG, "Success: STATUS_START_HEARTRATE_SCAN");
                    System.out.println("HR SUCESS");
                    break;
                case MibandCallback.STATUS_GET_BATTERY:
                    Log.e(TAG, "Success: STATUS_GET_BATTERY");
                    final int level = (int) data;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            battery.setText(level + " % заряд батареи");
                            text.append(level + " % заряд батареи\n");
                        }
                    });
                    break;
                case MibandCallback.STATUS_GET_ACTIVITY_DATA:
                    Log.e(TAG, "Success: STATUS_GET_ACTIVITY_DATA");
                    final int steps = (int) data;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            step.setText(steps + " steps");
                            text.append(steps + " steps\n");
                        }
                    });
                    break;
            }
        }

        @Override
        public void onFail(int errorCode, String msg, int status) {
            switch (status) {
                case MibandCallback.STATUS_SEARCH_DEVICE:
                    Log.e(TAG, "Fail: STATUS_SEARCH_DEVICE");
                    break;
                case MibandCallback.STATUS_CONNECT:
                    Log.e(TAG, "Fail: STATUS_CONNECT");
                    break;
                case MibandCallback.STATUS_SEND_ALERT:
                    Log.e(TAG, "Fail: STATUS_SEND_ALERT");
                    break;
                case MibandCallback.STATUS_GET_USERINFO:
                    Log.e(TAG, "Fail: STATUS_GET_USERINFO");
                    break;
                case MibandCallback.STATUS_SET_USERINFO:
                    Log.e(TAG, "Fail: STATUS_SET_USERINFO");
                    break;
                case MibandCallback.STATUS_START_HEARTRATE_SCAN:
                    System.out.println("HR FAILDED");
                    Log.e(TAG, "Fail: STATUS_START_HEARTRATE_SCAN");
                    miband.startHeartRateScan(1, mibandCallback);
                    break;
                case MibandCallback.STATUS_GET_BATTERY:
                    Log.e(TAG, "Fail: STATUS_GET_BATTERY");
                    break;
                case MibandCallback.STATUS_GET_ACTIVITY_DATA:
                    Log.e(TAG, "Fail: STATUS_GET_ACTIVITY_DATA");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_vive).setOnClickListener(this);
        findViewById(R.id.button_steps).setOnClickListener(this);
        findViewById(R.id.button_realtime_steps).setOnClickListener(this);
        findViewById(R.id.button_battery).setOnClickListener(this);
        findViewById(R.id.button_heart_start_one).setOnClickListener(this);
        findViewById(R.id.button_heart_start_many).setOnClickListener(this);

        heart = (TextView) findViewById(R.id.heart);
        step = (TextView) findViewById(R.id.steps);
        battery = (TextView) findViewById(R.id.battery);
        text = (TextView) findViewById(R.id.text);

        setTitle("Измерение пульса");


        miband = new Miband(getApplicationContext());

        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        miband = new Miband(getApplicationContext());

        miband.searchDevice(mBluetoothAdapter, this.mibandCallback);

        miband.setDisconnectedListener(new NotifyListener() {
            @Override
            public void onNotify(byte[] data) {
                miband.searchDevice(mBluetoothAdapter, mibandCallback);
            }
        });
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.button_vive) {
            miband.sendAlert(this.mibandCallback);
        } else if (i == R.id.button_steps) {
            miband.getCurrentSteps(this.mibandCallback);
        } else if (i == R.id.button_realtime_steps) {
            miband.setRealtimeStepListener(realtimeStepListener);
        } else if (i == R.id.button_battery) {
            miband.getBatteryLevel(this.mibandCallback);
        } else if (i == R.id.button_heart_start_one) {
            miband.startHeartRateScan(1, this.mibandCallback);
        } else if (i == R.id.button_heart_start_many) {
            miband.startHeartRateScan(0, this.mibandCallback);
        }
    }

}
