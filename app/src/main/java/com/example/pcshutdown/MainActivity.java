package com.example.pcshutdown;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private EditText macAddressEditText, broadcastIPEditText, usernameEditText, passwordEditText;
    private SharedPreferences sharedPreferences;
    private ProfileManager profileManager;
    private ActionManager actionManager;
    private String currentRemoteOS = "UNKNOWN";
    public static final Logger logger = Logger.getLogger(MainActivity.class.getName());

    private void executeRemoteAction(RemoteActionRunnable actionRunnable) {
        if (!ConnectionTester.isWifiConnected(MainActivity.this)) {
            DynamicToast.make(MainActivity.this, "Connect to Wifi to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profileManager.isProfileNotAvailable()) {
            DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
            return;
        }

        final String broadcastIP = broadcastIPEditText.getText().toString().trim();
        final String username = usernameEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString();

        new Thread(() -> {
            try {
                if (ConnectionTester.isPortOpen(broadcastIP, Constants.DEFAULT_SSH_PORT) && !"UNKNOWN".equals(currentRemoteOS)) {
                    actionRunnable.run(broadcastIP, username, password, currentRemoteOS);
                } else {
                    runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Failed to connect to the remote PC", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
                runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void executeWolAction(WolActionRunnable actionRunnable) {
        if (!ConnectionTester.isWifiConnected(MainActivity.this)) {
            DynamicToast.make(MainActivity.this, "Connect to Wifi to continue", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profileManager.isProfileNotAvailable()) {
            DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
            return;
        }

        final String broadcastIP = broadcastIPEditText.getText().toString().trim();
        final String macAddress = macAddressEditText.getText().toString().trim();

        new Thread(() -> {
            try {
                actionRunnable.run(macAddress, broadcastIP);       
            } catch (Exception e) {
                logger.log(Level.SEVERE, "WoL Error occurred: ", e);
                runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    interface RemoteActionRunnable {
        void run(String ip, String user, String pass, String os);
    }

    interface WolActionRunnable {
        void run(String mac, String ip);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUIElements();
        initializeSharedPreferences();
        initializeManagers();

        profileManager.loadProfile();

        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);
        Button btnDeleteProfile = findViewById(R.id.btnDeleteProfile);
        Button btnTestSSHConnection = findViewById(R.id.btnTestSSHConnection);
        Button btnTurnOn = findViewById(R.id.btnTurnOn);
        Button btnTurnOff = findViewById(R.id.btnTurnOff);
        Button btnSleep = findViewById(R.id.btnSleep);
        Button btnReboot = findViewById(R.id.btnReboot);

        btnSaveProfile.setOnClickListener(v -> {
            try {
                profileManager.saveProfile();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
                runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        btnDeleteProfile.setOnClickListener(v -> {
            try {
                profileManager.deleteProfile();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
                runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        btnTestSSHConnection.setOnClickListener(v -> {
            final String broadcastIP = broadcastIPEditText.getText().toString().trim();
            final String username = usernameEditText.getText().toString().trim();
            final String password = passwordEditText.getText().toString().trim();

            new Thread(() -> {
                String remoteOS = ConnectionTester.testSSHConnection(
                    broadcastIP, 
                    Constants.DEFAULT_SSH_PORT, 
                    username, 
                    password
                );

                runOnUiThread(() -> {
                    if ("UNKNOWN".equals(remoteOS)) {
                        DynamicToast.make(MainActivity.this, "SSH connection failed", Toast.LENGTH_SHORT).show();
                    } else {
                        DynamicToast.make(MainActivity.this, "SSH connection success", Toast.LENGTH_SHORT).show();
                        MainActivity.this.currentRemoteOS = remoteOS;
                    }
                });
            }).start();
        });
        
        btnTurnOn.setOnClickListener(v -> executeWolAction((mac, ip) -> 
            actionManager.wakeOnLan(mac, ip)));

        btnTurnOff.setOnClickListener(v -> executeRemoteAction((ip, user, pass, os) -> 
            actionManager.shutdownRemotePC(ip, user, pass, os)));

        btnSleep.setOnClickListener(v -> executeRemoteAction((ip, user, pass, os) -> 
            actionManager.sleepRemotePC(ip, user, pass, os)));

        btnReboot.setOnClickListener(v -> executeRemoteAction((ip, user, pass, os) -> 
            actionManager.rebootRemotePC(ip, user, pass, os)));
    }

    private void initializeUIElements() {
        macAddressEditText = findViewById(R.id.macAddressEditText);
        broadcastIPEditText = findViewById(R.id.broadcastIPEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        Typeface typeface = getResources().getFont(R.font.silkscreen);
        DynamicToast.Config.getInstance()
                .setTextTypeface(typeface)
                .apply();
    }

    private void initializeSharedPreferences() {
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    Constants.PREFS_NAME,
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
        }
    }

    private void initializeManagers() {
        profileManager = new ProfileManager(macAddressEditText, broadcastIPEditText, usernameEditText, passwordEditText, sharedPreferences, this);
        actionManager = new ActionManager(this);
    }
}
