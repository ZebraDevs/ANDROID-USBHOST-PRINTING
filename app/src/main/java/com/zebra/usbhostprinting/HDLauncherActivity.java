package com.zebra.usbhostprinting;

import com.zebra.sdk.comm.*;
import com.zebra.sdk.printer.discovery.*;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import java.util.*;



import android.app.*;
import android.content.*;
import android.hardware.usb.*;
import android.widget.*;

//https://stackoverflow.com/questions/57909439/how-to-change-resolutionsize-and-density-with-android-code-runtime-getruntime

public class HDLauncherActivity extends AppCompatActivity {

    Intent starterIntent;
    TextView tvOut;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

    private PendingIntent mPermissionIntent;
    private boolean hasPermissionToCommunicate = false;

    private UsbManager mUsbManager;
    private Button buttonRequestPermission;
    private Button buttonPrint;
    private DiscoveredPrinterUsb  discoveredPrinterUsb;

    Button btSetttings;
    //@SuppressLint("JavascriptInterface")



    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            hasPermissionToCommunicate = true;
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        starterIntent = getIntent();

        setContentView(R.layout.activity_hdlauncher);

        btSetttings = findViewById(R.id.btn_SETTINGS);

        tvOut = findViewById(R.id.tvOut);

        // Register broadcast receiver that catches USB permission intent
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0
                |PendingIntent.FLAG_IMMUTABLE);




    }

    protected void onPause() {
        unregisterReceiver(mUsbReceiver);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();

        //add if to manage receiver export behavior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mUsbReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mUsbReceiver, filter);
        }

    }

    @JavascriptInterface
    public String getWebviewVersion() {
        PackageInfo info = WebView.getCurrentWebViewPackage();
        return "WEBVIEW version " + info.versionName+"\n"+ Build.FINGERPRINT;
    }

    @JavascriptInterface
    public void showMsg(String txt) {
        runOnUiThread(new Runnable() {
                          public void run() {
                              tvOut.setText(txt);
                          }
                      }

        );
    }

    public void onClickbtn_SETTINGS(View v) {
        try {
            new Thread(new Runnable() {

                public void run() {
                    // Find connected printers
                    UsbDiscoveryHandler handler = new UsbDiscoveryHandler();
                    UsbDiscoverer.findPrinters(getApplicationContext(), handler);

                    try {
                        while (!handler.discoveryComplete) {
                            Thread.sleep(100);
                        }

                        if (handler.printers != null && handler.printers.size() > 0) {
                            discoveredPrinterUsb = handler.printers.get(0);

                            if (!mUsbManager.hasPermission(discoveredPrinterUsb.device)) {
                                mUsbManager.requestPermission(discoveredPrinterUsb.device, mPermissionIntent);
                            } else {
                                hasPermissionToCommunicate = true;
                                String printerSignature = discoveredPrinterUsb.device.getProductName() + discoveredPrinterUsb.device.getSerialNumber();
                                showMsg("PRINTER FOUND:\n"+printerSignature);
                            }
                        }
                    } catch (Exception e) {
                       // Toast.makeText(getApplicationContext(), e.getMessage() + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        showMsg(e.getMessage() );
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e("TAG", "onClickbtn_SETTINGS "+e.getMessage());
        }
    }
    public void onClickbtn_CALC(View v) {
        try {
            if (hasPermissionToCommunicate) {
                Connection conn = null;
                try {
                    conn = discoveredPrinterUsb.getConnection();
                    conn.open();
                    conn.write("~WC".getBytes());
                } catch (ConnectionException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage() + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (ConnectionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e("TAG", "onClickbtn_CALC "+e.getMessage());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class UsbDiscoveryHandler implements DiscoveryHandler {
        public List<DiscoveredPrinterUsb> printers;
        public boolean discoveryComplete = false;

        public UsbDiscoveryHandler() {
            printers = new LinkedList<DiscoveredPrinterUsb>();
        }

        public void foundPrinter(final DiscoveredPrinter printer) {
            printers.add((DiscoveredPrinterUsb) printer);
        }

        public void discoveryFinished() {
            discoveryComplete = true;
        }

        public void discoveryError(String message) {
            discoveryComplete = true;
        }
    }

}