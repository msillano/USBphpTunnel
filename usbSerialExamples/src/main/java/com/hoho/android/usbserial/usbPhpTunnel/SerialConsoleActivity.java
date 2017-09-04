/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 *
 * Copyright 2017 marco sillano <marcosillano@gmail.com>
 *
 *  Modified for usbPhpTunnel v.1.2  (2017-09-01)
 *   - modified updateReceivedData(byte[] data): String/data management, added trim to clean first chars
 *  Modified for usbPhpTunnel v.1.1  (2017-04-22)
 *   - Reboot (optional)  every 24h at time in 'reboot' field (config.ini): values: 'none'|'HH\:MM\:SS'
 *  Modified for usbPhpTunnel v.1.0 (2017-03-20)
 *  GNU Lesser General Public License
 *
 */

package com.hoho.android.usbserial.usbPhpTunnel;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.Config;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 * @author marco sillano (marco.sillano@gmail-com) 2017 modified for php tunnel
 */
public class SerialConsoleActivity extends Activity {

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     * <p>
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;
    private final String TAG = SerialConsoleActivity.class.getSimpleName();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private TextView mTitleTextView;
    private TextView mDumpTextView;
    private ScrollView mScrollView;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Try restart after " + e.getMessage());
                    onDeviceStateChange();
                }

                @Override
                public void onNewData(final byte[] data) {
                    SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SerialConsoleActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };
// Runtime.getRuntime().exec(new String[]{"/system/xbin/su","-c","reboot now"});
    // == added for tunnel app m.s.
    //  write limit
    private final static int MAX_LINE = 80;
    //  int[] baud = {2400,  4800, 9600, 19200, 38400, 115200};
    private int baud = 9600;    // default
    private static String phpPath = "http://localhost:8080";
    private String colorPhp = "#AA0000";
    private String colorArduino = "#00AA00";
    private String reboot = "none";

    private SerialInputOutputManager mSerialIoManager;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //
            StringBuilder sb = new StringBuilder();
            sb.append("INTENT Action: " + intent.getAction() + "\n");
            sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
            String log = sb.toString();
            Log.d(TAG, log);
            //
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if ((device.getVendorId() == sPort.getDriver().getDevice().getVendorId()) &&
                        (device.getProductId() == sPort.getDriver().getDevice().getProductId())) {
                    // call your method that cleans up and closes application
                    Log.d(TAG, "USB_DEVICE_DETACHED: pause SerialConsoleActivity");
                    SerialConsoleActivity.this.onPause();
                }
            }
            if (new String("usbSerial.action.REBOOT").equals(action)) {
                Log.d(TAG, "REBOOT: delayed received");
                rebootNow();
            }


            }
    };


    /**
     * Starts the activity, using the supplied driver instance.
     */
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        CheckBox chkDTR = (CheckBox) findViewById(R.id.checkBoxDTR);
        CheckBox chkRTS = (CheckBox) findViewById(R.id.checkBoxRTS);

        // == added for tunnel app m.s.
        IntentFilter mfilter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, mfilter);

// end added

        chkDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    sPort.setDTR(isChecked);
                } catch (IOException x) {
                    // noting
                }
            }
        });

        chkRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    sPort.setRTS(isChecked);
                } catch (IOException x) {
                    // noting
                }
            }
        });

// == added for tunnel app m.s.


        if (isExternalStorageWritable()) {
            File path = Environment.getExternalStoragePublicDirectory(getString(R.string.app_title));
            File file = new File(path, getString(R.string.config));
            if (file.exists()) {
                Config myConfig = new Config(file.getPath());
                if (myConfig.load()) {
                    phpPath = myConfig.get("phpPath");
                    baud = Integer.parseInt(myConfig.get("baudRate"));
                    colorPhp = myConfig.get("colorPhp");
                    colorArduino = myConfig.get("colorArduino");
                    reboot = myConfig.get("reboot");
                }
            }
        }
        scheduleNotification();
// end added

    }

    @Override
    protected void onPause() {
        super.onPause();
        // == added for tunnel app m.s.
        try {
            unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            Log.d(TAG, "unregisterReceiver: " + e.getMessage());
        }
        // end added
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }

        finish();
    }

    void showStatus(TextView theTextView, String theLabel, boolean theValue) {
        String msg = theLabel + ": " + (theValue ? getString(R.string.enabled) : getString(R.string.disabled)) + "\n";
        theTextView.append(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            mTitleTextView.setText(getString(R.string.no_device));
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText(getString(R.string.no_open));
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(baud, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                showStatus(mDumpTextView, "CD  - Carrier Detect", sPort.getCD());
                showStatus(mDumpTextView, "CTS - Clear To Send", sPort.getCTS());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "DTR - Data Terminal Ready", sPort.getDTR());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "RI  - Ring Indicator", sPort.getRI());
                showStatus(mDumpTextView, "RTS - Request To Send", sPort.getRTS());
                String msg = "Baud rate : " + baud + "\n\n";
                mDumpTextView.append(msg);
// test: prints list of running AppProcess:
                /*
                ActivityManager activityManager = (ActivityManager) SerialConsoleActivity.this.getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> runningAppList = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo processInfo : runningAppList ){
                    String processName = processInfo.processName;
                    mDumpTextView.append(processName+"\n");
                }
                */
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText(getString(R.string.err_device) + " " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            mTitleTextView.setText(getString(R.string.ok_device) + " " + sPort.getClass().getSimpleName() + " (" + sPort.getDriver().getDevice().getDeviceName() + ")");
        }
        onDeviceStateChange();
    }


    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    //=================================================== special for tunnel application (m.s.)
    private void scheduleNotification() {
        if (reboot.equals("none")){
            return ;
        }
        GregorianCalendar rightNow = new GregorianCalendar();
        rightNow.setLenient(true);
        rightNow.setTime(new Date());

        long nowMillis =rightNow.getTimeInMillis();
        String[] rebootTime = reboot.split(":");
        rightNow.set(Calendar.HOUR_OF_DAY, Integer.parseInt(rebootTime[0]));
        rightNow.set(Calendar.MINUTE, Integer.parseInt(rebootTime[1]));
        rightNow.set(Calendar.SECOND, Integer.parseInt(rebootTime[2]));
        long  futureInMillis =rightNow.getTimeInMillis();
        if (futureInMillis < nowMillis){
            rightNow.add(Calendar.DAY_OF_MONTH, 1);
            futureInMillis =rightNow.getTimeInMillis();
        }
        final long delay =  futureInMillis - nowMillis;
        Log.d(TAG, "INTENT: set reboot at " + rightNow );
        Timer timer = new Timer();
        timer.schedule( new TimerTask(){
            @Override
            public void run() {
                rebootNow();
            }
        }, delay);
    }

    private void rebootNow() {
        try {
            Runtime.getRuntime().exec(new String[]{"/system/xbin/su", "-c", "reboot now"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // http php read
    public static String GET(String page) {
        String url = phpPath + page;
        String result;
        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);

            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            result = EntityUtils.toString(httpEntity);
        } catch (Exception e) {
            result = "** " + e.getMessage();
        }
        return result;
    }


    // replaces TextView.append(string) and limit lines at MAX_LINES
    public void writeTerminal(String data, String color) {
// * : messages, colored
        if (data.charAt(0) == '*') {
            mDumpTextView.append(Html.fromHtml("<font color='" + color + "'>" + data + "</font>"));
            mDumpTextView.append("\n\n");
        } else
            // else data
            mDumpTextView.append(data);
        // Erase excessive lines
        int excessLineNumber = mDumpTextView.getLineCount() - MAX_LINE;
        if (excessLineNumber > 0) {
            int eolIndex = -1;
            CharSequence charSequence = mDumpTextView.getText();
            for (int i = 0; i < excessLineNumber; i++) {
                do {
                    eolIndex++;
                } while (eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n');
            }
            if (eolIndex < charSequence.length()) {
                mDumpTextView.getEditableText().delete(0, eolIndex + 1);
            } else {
                mDumpTextView.setText("");
            }
        }
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
//debug
        Log.d(TAG, "Memory total: " + Runtime.getRuntime().totalMemory() + "  free: " + Runtime.getRuntime().freeMemory());
    }

    private void sendString(String out) {
        byte[] b = out.getBytes(Charset.forName("UTF-8"));

        // anny messge for arduino  goes to screen
        String sended;
        if (b[0] == '*') {
            sended = out;
        } else {
            // sending commands to Arduino only if it not starts with '*'
            sended = String.format(getString(R.string.send), b.length) + " \n";
            sended += HexDump.dumpHexString(b) + "\n\n";
            if (mSerialIoManager != null) {
                try {
                    mSerialIoManager.writeAsync(b);
                } catch (IOException e) {
                    Log.w(TAG, "send error: " + e.getMessage());
                }
            }
        }
        writeTerminal(sended, colorPhp);
    }


    private void updateReceivedData(byte[] data) {
        // ver 1.2 modified String/data management
        // added trim to clean first chars
        String message;
// any message from arduino to screen
        message = new String(data).trim();
        // tunnelling from arduino to php only if it starts with '/'
        if (message.length() > 1 && message.charAt(0) == '/') {
            // test  new HttpAsyncTask().execute("/testio/add.php?primo=7&secondo=6.8&terzo=14:02");
            new HttpAsyncTask().execute(message);
        }
        if (message.length() > 1 && message.charAt(0) != '*') {
            message = String.format(getString(R.string.read), data.length) + " \n"
                    + HexDump.dumpHexString(data) + "\n\n";
        }

     writeTerminal(message, colorArduino);
    }

    // from  https://developer.android.com/guide/topics/data/data-storage.html
    /* Checks if external storage is available for read and write */
    // TODO place in Config ?
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            String[] parts = result.split("\\r?\\n|\\r");
            //  sendString("* found parts: " + parts.length + "\n");
            for (String toSend : parts) {
                final String out = toSend.trim();
                if (out.length() > 1) {
                    sendString(out + "\n");
                }
            }
            // sendString(result + "\r");
        }
    }

}
