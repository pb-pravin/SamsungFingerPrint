package com.prav.fingerprint;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;
import com.samsung.android.sdk.pass.SpassInvalidStateException;

public class FingerprintActivity extends Activity {
    private SpassFingerprint spassFingerprint;
    private Spass spass;
    private Context context;
    private TextView statusText;
    private Button authFingerprintButton;
    private Button registerFingerprintButton;
    private boolean isFeatureEnabled =false;
    private boolean onReadyIdentify=false;
    private boolean onReadyEnroll=false;

    private SpassFingerprint.IdentifyListener listener = new SpassFingerprint.IdentifyListener() {

        @Override
        public void onFinished(int eventStatus) {
            log("identify finished : reason=" + getEventStatusName(eventStatus));
            onReadyIdentify = false;
            int FingerprintIndex = 0;
            try {
                FingerprintIndex = spassFingerprint.getIdentifiedFingerprintIndex();
            } catch (IllegalStateException ise) {
                log(ise.getMessage());
            }
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
                log("onFinished() : Identify authentification Success with FingerprintIndex : " + FingerprintIndex);
            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
                log("onFinished() : Password authentification Success");
            } else {
                log("onFinished() : Authentification Fail for identify");
            }
        }

        @Override
        public void onReady() {
            log("identify state is ready");
        }

        @Override
        public void onStarted() {
            log("User touched fingerprint sensor!");
        }
    };

    private SpassFingerprint.RegisterListener registerListener = new SpassFingerprint.RegisterListener() {

        @Override
        public void onFinished() {
            onReadyEnroll = false;
            log("RegisterListener.onFinished()");

        }

    };
    private static String getEventStatusName(int eventStatus) {
        switch (eventStatus) {
            case SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS:
                return "STATUS_AUTHENTIFICATION_SUCCESS";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS:
                return "STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS";
            case SpassFingerprint.STATUS_TIMEOUT_FAILED:
                return "STATUS_TIMEOUT";
            case SpassFingerprint.STATUS_SENSOR_FAILED:
                return "STATUS_SENSOR_ERROR";
            case SpassFingerprint.STATUS_USER_CANCELLED:
                return "STATUS_USER_CANCELLED";
            case SpassFingerprint.STATUS_QUALITY_FAILED:
                return "STATUS_QUALITY_FAILED";
            case SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE:
                return "STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE";
            case SpassFingerprint.STATUS_AUTHENTIFICATION_FAILED:
            default:
                return "STATUS_AUTHENTIFICATION_FAILED";
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);

        statusText = (TextView) findViewById(R.id.fingerprint_status_text);
        authFingerprintButton = (Button) findViewById(R.id.auth_fingerprint_button);
        registerFingerprintButton = (Button) findViewById(R.id.register_fingerprint_button);


        spass = new Spass();

        try {
            spass.initialize(FingerprintActivity.this);
        } catch (SsdkUnsupportedException e) {
            log("Exception: " + e);
        } catch (UnsupportedOperationException e){
            log("Fingerprint Service is not supported in the device");
        }
        isFeatureEnabled = spass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);

        if(isFeatureEnabled){
            spassFingerprint = new SpassFingerprint(FingerprintActivity.this);
            log("Fingerprint Service is supported in the device. " + " SDK version : " + spass.getVersionName());
        } else {
            logClear();
            log("Fingerprint Service is not supported in the device.");
        }

        SparseArray<View.OnClickListener> listeners = new SparseArray<View.OnClickListener>();
        listeners.put(R.id.auth_fingerprint_button, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logClear();
                try {
                    if (!spassFingerprint.hasRegisteredFinger()) {
                        log("Please register finger first");
                    } else {
                        if (onReadyIdentify == false) {
                            try {
                                onReadyIdentify = true;
                                spassFingerprint.startIdentify(listener);
                                log("Please identify finger to verify you");
                            } catch (SpassInvalidStateException ise) {
                                onReadyIdentify = false;
                                if (ise.getType() == SpassInvalidStateException.STATUS_OPERATION_DENIED) {
                                    log("Exception: " + ise.getMessage());
                                }
                            } catch (IllegalStateException e) {
                                onReadyIdentify = false;
                                log("Exception: " + e);
                            }
                        } else {
                            log("Please cancel Identify first");
                        }
                    }
                } catch (UnsupportedOperationException e) {
                    log("Fingerprint Service is not supported in the device");
                }
            }
        });

        listeners.put(R.id.register_fingerprint_button, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logClear();
                try {
                    if (onReadyIdentify == false) {
                        if (onReadyEnroll == false) {
                            onReadyEnroll = true;
                            spassFingerprint.registerFinger(FingerprintActivity.this, registerListener);
                            log("Jump to the Enroll screen");
                        } else {
                            log("Please wait and try to register again");
                        }
                    } else {
                        log("Please cancel Identify first");
                    }
                } catch (UnsupportedOperationException e){
                    log("Fingerprint Service is not supported in the device");
                }
            }
        });


        final int N = listeners.size();
        for (int i = 0; i < N; i++) {
            int id = listeners.keyAt(i);
            Button button = (Button)findViewById(id);
            if (button != null) {
                button.setOnClickListener(listeners.valueAt(i));

                if (!isFeatureEnabled) {
                    button.setEnabled(false);
                } else {
                    if (id == R.id.register_fingerprint_button) {
                        if (!spass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT_UNIQUE_ID)) {
                           // button.setEnabled(false);
                        }
                    }
                }
            }
        }

    }

    public void log(String text) {
        final String txt = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText(txt);
            }
        });
    }

    public void logClear() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText("");
            }
        });
    }
            @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fingerprint, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
