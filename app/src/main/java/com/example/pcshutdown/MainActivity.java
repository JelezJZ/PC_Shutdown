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
    public static final Logger logger = Logger.getLogger(MainActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUIElements();
        initializeSharedPreferences();
        initializeManagers();

        profileManager.loadProfile();

        Button btnTurnOn = findViewById(R.id.btnTurnOn);
        Button btnTurnOff = findViewById(R.id.btnTurnOff);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);
        Button btnDeleteProfile = findViewById(R.id.btnDeleteProfile);
        Button btnSleep = findViewById(R.id.btnSleep);
        Button btnReboot = findViewById(R.id.btnReboot);

        btnTurnOn.setOnClickListener(v -> {
            if(!ConnectionTester.isWifiConnected(MainActivity.this)) {
                DynamicToast.make(MainActivity.this, "Connect to Wifi to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            if (profileManager.isProfileNotAvailable()) {
                DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Считываем значения в UI-потоке
            final String broadcastIP = broadcastIPEditText.getText().toString();
            final String macAddress = macAddressEditText.getText().toString();

            // Запускаем выполнение фоновой задачи
            new Thread(() -> {
                try {
                    if (ConnectionTester.isPortOpen(broadcastIP, Constants.DEFAULT_SSH_PORT)) {
                        runOnUiThread(() -> DynamicToast.make(MainActivity.this, "PC is already online", Toast.LENGTH_SHORT).show());
                    } else {
                        actionManager.wakeOnLan(macAddress, broadcastIP);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "An error occurred: ", e);
                    runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnTurnOff.setOnClickListener(v -> {
            if(!ConnectionTester.isWifiConnected(MainActivity.this)) {
                DynamicToast.make(MainActivity.this, "Connect to Wifi to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            if (profileManager.isProfileNotAvailable()) {
                DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                return;
            }

            final String broadcastIP = broadcastIPEditText.getText().toString();
            final String username = usernameEditText.getText().toString();
            final String password = passwordEditText.getText().toString();

            new Thread(() -> {
                try {
                    if (ConnectionTester.isPortOpen(broadcastIP, Constants.DEFAULT_SSH_PORT) && ConnectionTester.testSSHConnection(broadcastIP, Constants.DEFAULT_SSH_PORT, username, password)) {
                        actionManager.shutdownRemotePC(broadcastIP, username, password);
                    } else {
                        runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Failed to connect to the remote PC", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "An error occurred: ", e);
                    runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnSleep.setOnClickListener(v -> {
            if(!ConnectionTester.isWifiConnected(MainActivity.this)) {
                DynamicToast.make(MainActivity.this, "Connect to Wifi to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            if (profileManager.isProfileNotAvailable()) {
                DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                return;
            }

            final String broadcastIP = broadcastIPEditText.getText().toString();
            final String username = usernameEditText.getText().toString();
            final String password = passwordEditText.getText().toString();

            new Thread(() -> {
                try {
                    if (ConnectionTester.isPortOpen(broadcastIP, Constants.DEFAULT_SSH_PORT) && ConnectionTester.testSSHConnection(broadcastIP, Constants.DEFAULT_SSH_PORT, username, password)) {
                        actionManager.sleepRemotePC(broadcastIP, username, password);
                    } else {
                        runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Failed to connect to the remote PC", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "An error occurred: ", e);
                    runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnReboot.setOnClickListener(v -> {
            if(!ConnectionTester.isWifiConnected(MainActivity.this)) {
                DynamicToast.make(MainActivity.this, "Connect to Wifi to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            if (profileManager.isProfileNotAvailable()) {
                DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                return;
            }

            final String broadcastIP = broadcastIPEditText.getText().toString();
            final String username = usernameEditText.getText().toString();
            final String password = passwordEditText.getText().toString();

            new Thread(() -> {
                try {
                    if (ConnectionTester.isPortOpen(broadcastIP, Constants.DEFAULT_SSH_PORT) && ConnectionTester.testSSHConnection(broadcastIP, Constants.DEFAULT_SSH_PORT, username, password)) {
                        actionManager.rebootRemotePC(broadcastIP, username, password);
                    } else {
                        runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Failed to connect to the remote PC", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "An error occurred: ", e);
                    runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnSaveProfile.setOnClickListener(v -> new Thread(() -> {
            try {
                profileManager.saveProfile();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
                runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start());

        btnDeleteProfile.setOnClickListener(v -> new Thread(() -> {
            try {
                profileManager.deleteProfile();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An error occurred: ", e);
                runOnUiThread(() -> DynamicToast.make(MainActivity.this, "Error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start());
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
