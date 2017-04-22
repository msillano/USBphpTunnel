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
 * Modified for usbPhpTunnel v.1.1  (2017-04-22)
 *   - If WEB server not ready, restarts after 20s
 *   - Addeded 'reboot' field in config.ini
 *   Modified for usbPhpTunnel v.1.0 (2017-03-20)
 * *  GNU Lesser General Public License
 */

package com.hoho.android.usbserial.usbPhpTunnel;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.Config;
import com.hoho.android.usbserial.util.HexDump;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows a {@link ListView} of available USB devices.
 *
 * @author mike wakerly (opensource@hoho.com)
 * @author marco sillano (marco.sillano@gmail-com) 2017 modified for php tunnel
 */


public class DeviceListActivity extends Activity {

    private final String TAG = DeviceListActivity.class.getSimpleName();

    private UsbManager mUsbManager;
    private TextView mProgressBarTitle;
    private ProgressBar mProgressBar;

    //== php: -- 0x1a86 / 0x7523: Qinheng CH340
    private int autoPortSelection = 1;
    private String arduinoVendorId = "1A86";
    private String arduinoProductId = "7523";
    private static String phpPath = "http://localhost:8080";
    private boolean webRunning = false;

    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 5000;
    private int countrf = 0;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    refreshDeviceList();
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
// == added fortunnel app m.s.
                    if ((autoPortSelection > 0) && (countrf++ >= autoPortSelection)) {  // number of refresh before connection
                        for (int i = 0; i < mEntries.size(); i++) {
                            final UsbSerialDriver driver = mEntries.get(i).getDriver();
                            final String vendorId = HexDump.toHexString((short) driver.getDevice().getVendorId());
                            final String productId = HexDump.toHexString((short) driver.getDevice().getProductId());
                            if (vendorId.equals(arduinoVendorId) && productId.equals(arduinoProductId)) {
                                countrf = 0;
                                showConsoleActivity(mEntries.get(i));

                            }
                        }

                    }
// == end added
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    private final List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();
    private ArrayAdapter<UsbSerialPort> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        ListView mListView = (ListView) findViewById(R.id.deviceList);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBarTitle = (TextView) findViewById(R.id.progressBarTitle);


        mAdapter = new ArrayAdapter<UsbSerialPort>(this,
                android.R.layout.simple_expandable_list_item_2, mEntries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TwoLineListItem row;
                if (convertView == null) {
                    final LayoutInflater inflater =
                            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    row = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = (TwoLineListItem) convertView;
                }

                final UsbSerialPort port = mEntries.get(position);
                final UsbSerialDriver driver = port.getDriver();
                final UsbDevice device = driver.getDevice();

                final String title = String.format(getString(R.string.device_format) + " %s",
                        HexDump.toHexString((short) device.getVendorId()),
                        HexDump.toHexString((short) device.getProductId()));
                row.getText1().setText(title);

                final String subtitle = driver.getClass().getSimpleName();
                row.getText2().setText(subtitle);
                return row;
            }

        };
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Pressed item " + position);
                if (position >= mEntries.size()) {
                    Log.w(TAG, "Illegal position.");
                    return;
                }

                final UsbSerialPort port = mEntries.get(position);
                showConsoleActivity(port);
            }
        });

// == added fortunnel app m.s.
        if (isExternalStorageWritable()) {
            File path = Environment.getExternalStoragePublicDirectory(
                    getString(R.string.app_title));
            File file = new File(path, getString(R.string.config));
            if (!file.exists()) {
                if (!path.mkdirs()) {
                    Log.e(TAG, "Directory not created");
                }
            }
            Config myConfig = new Config(file.getPath());
            if (myConfig.load()) {
// if file  exist, read
                arduinoVendorId = myConfig.get("arduinoVendorId");
                arduinoProductId = myConfig.get("arduinoProductId");
                autoPortSelection = Integer.parseInt(myConfig.get("autoPortSelection"));
                phpPath = myConfig.get("phpPath");
            } else {
// else sets all defaults and write
                myConfig.set("autoPortSelection", "1");
                // -- 0x1a86 / 0x7523: Qinheng CH340
                myConfig.set("arduinoVendorId", arduinoVendorId);
                myConfig.set("arduinoProductId", arduinoProductId);
                // used by SerialConsoleActivity
                myConfig.set("baudRate", "9600");
                myConfig.set("phpPath", "http://localhost:8080");
                myConfig.set("colorPhp", "#AA0000");
                myConfig.set("colorArduino", "#00AA00");
                myConfig.set("reboot", "01:00:00");
                myConfig.store();
            }

        }
//=========== end added
    }

    @Override
    protected void onResume() {
        super.onResume();
        countrf = 0;
        mHandler.sendEmptyMessage(MESSAGE_REFRESH);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeMessages(MESSAGE_REFRESH);
    }


    private void refreshDeviceList() {
        showProgressBar();
        if (!webRunning) {
            Log.d(TAG, "Waiting web server ...");
            mProgressBarTitle.setText(getString(R.string.test_web) + phpPath + ") ...");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    SystemClock.sleep(1000);
                    String url = phpPath + "/";
                    try {
                        DefaultHttpClient httpClient = new DefaultHttpClient();
                        HttpGet httpGet = new HttpGet(url);
                        httpClient.execute(httpGet);
                        Log.d(TAG, "Host: running");
                        webRunning = true;
                    } catch (Exception e) {
                        Log.d(TAG, "Host: " + e.getMessage());
// new
                        webRunning = false;

                        Intent restartIntent = DeviceListActivity.this.getPackageManager().getLaunchIntentForPackage("com.hoho.android.usbserial.usbPhpTunnel");
                        restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP  | Intent.FLAG_ACTIVITY_NEW_TASK);
                        restartIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                        PendingIntent pi = PendingIntent.getActivity(
                                getApplicationContext(), 0,
                                restartIntent, PendingIntent.FLAG_CANCEL_CURRENT |PendingIntent.FLAG_ONE_SHOT );

                        AlarmManager am =( AlarmManager) DeviceListActivity.this.getSystemService(Context.ALARM_SERVICE);
                        am.set(AlarmManager.RTC, System.currentTimeMillis()+ 1000 * 20 , pi); // Millisec * Second + now();
                        finish();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    hideProgressBar();
                }
            }.execute((Void) null);
            return;
        }

        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            @Override
            protected List<UsbSerialPort> doInBackground(Void... params) {
                Log.d(TAG, "Refreshing device list ...");
                SystemClock.sleep(1000);
                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                try {   // TODO debug here to find error in release mode in UsbSerialProber
                    final List<UsbSerialDriver> drivers =
                            UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                    for (final UsbSerialDriver driver : drivers) {
                        final List<UsbSerialPort> ports = driver.getPorts();
                        Log.d(TAG, String.format("+ %s: %d port%s",
                                driver, ports.size(), ports.size() == 1 ? "" : "s"));
                        result.addAll(ports);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "List err: " + e.getMessage());
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<UsbSerialPort> result) {
                mEntries.clear();
                mEntries.addAll(result);
                mAdapter.notifyDataSetChanged();
                mProgressBarTitle.setText(
                        String.format(getString(R.string.device_found), mEntries.size()));
                hideProgressBar();
                Log.d(TAG, "Done refreshing, " + mEntries.size() + " entries found.");
            }

        }.execute((Void) null);
    }

    // ======================================================  addaded for tunnel app (m.s.)
// from  https://developer.android.com/guide/topics/data/data-storage.html
    /* Checks if external storage is available for read and write */
    // TODO place in Config ?
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


// ======================================================  end added


    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBarTitle.setText(R.string.refreshing);
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void showConsoleActivity(UsbSerialPort port) {
        SerialConsoleActivity.show(this, port);
    }

}
