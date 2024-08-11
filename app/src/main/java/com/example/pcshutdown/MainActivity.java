package com.example.pcshutdown;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    private EditText macAddressEditText;
    private EditText broadcastIPEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;

    private static final String PREFS_NAME = "PCShutdownPrefs";
    private static final String MAC_ADDRESS_KEY = "macAddress";
    private static final String BROADCAST_IP_KEY = "broadcastIP";
    private static final String SSH_PORT_KEY = "sshPort";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final int DEFAULT_PORT = 9;
    private static final int DEFAULT_SSH_PORT = 22;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        macAddressEditText = findViewById(R.id.macAddressEditText);
        broadcastIPEditText = findViewById(R.id.broadcastIPEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        Typeface typeface = getResources().getFont(R.font.silkscreen);

        DynamicToast.Config.getInstance()
                .setTextTypeface(typeface)
                .apply();

        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        loadProfile();

        Button btnTurnOn = findViewById(R.id.btnTurnOn);
        Button btnTurnOff = findViewById(R.id.btnTurnOff);
        Button btnSaveProfile = findViewById(R.id.btnSaveProfile);
        Button btnDeleteProfile = findViewById(R.id.btnDeleteProfile);
        Button btnSleep = findViewById(R.id.btnSleep);
        Button btnReboot = findViewById(R.id.btnReboot);

        btnTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isProfileAvailable()) {
                    DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String broadcastIP = broadcastIPEditText.getText().toString();
                        String macAddress = macAddressEditText.getText().toString();
                        if (isPortOpen(broadcastIP, DEFAULT_SSH_PORT)) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DynamicToast.make(MainActivity.this, "PC is already online", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            wakeOnLan(macAddress, broadcastIP, DEFAULT_PORT);
                        }
                    }
                }).start();
            }
        });

        btnTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isProfileAvailable()) {
                    DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String broadcastIP = broadcastIPEditText.getText().toString();
                        String username = usernameEditText.getText().toString();
                        String password = passwordEditText.getText().toString();
                        if (isPortOpen(broadcastIP, DEFAULT_SSH_PORT) && testSSHConnection(broadcastIP, DEFAULT_SSH_PORT, username, password)) {
                            shutdownRemotePC(broadcastIP, DEFAULT_SSH_PORT, username, password);
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DynamicToast.make(MainActivity.this, "Failed to connect to the remote PC", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        btnSleep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isProfileAvailable()) {
                    DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String broadcastIP = broadcastIPEditText.getText().toString();
                        String username = usernameEditText.getText().toString();
                        String password = passwordEditText.getText().toString();
                        if (isPortOpen(broadcastIP, DEFAULT_SSH_PORT) && testSSHConnection(broadcastIP, DEFAULT_SSH_PORT, username, password)) {
                            sleepRemotePC(broadcastIP, DEFAULT_SSH_PORT, username, password);
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DynamicToast.make(MainActivity.this, "Failed to connect to the remote PC", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        btnReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isProfileAvailable()) {
                    DynamicToast.make(MainActivity.this, "Profile not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String broadcastIP = broadcastIPEditText.getText().toString();
                        String username = usernameEditText.getText().toString();
                        String password = passwordEditText.getText().toString();
                        if (isPortOpen(broadcastIP, DEFAULT_SSH_PORT) && testSSHConnection(broadcastIP, DEFAULT_SSH_PORT, username, password)) {
                            rebootRemotePC(broadcastIP, DEFAULT_SSH_PORT, username, password);
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    DynamicToast.make(MainActivity.this, "Failed to connect to the remote PC", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        btnSaveProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        saveProfile();
                    }
                }).start();
            }
        });

        btnDeleteProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        deleteProfile();
                    }
                }).start();
            }
        });
    }

    private boolean isProfileAvailable() {
        String broadcastIP = sharedPreferences.getString("broadcastIP", null);
        String macAddress = sharedPreferences.getString("macAddress", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        return broadcastIP != null && macAddress != null && username != null && password != null;
    }

    private boolean isPortOpen(String ipAddress, int port) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, port), 1000);  // Timeout в 1 секунду
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean testSSHConnection(String hostname, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(1000); // Timeout в 1 секунд

            session.disconnect();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void wakeOnLan(String macAddress, String broadcastIP, int port) {
        try {
            byte[] macBytes = getMacBytes(macAddress);
            byte[] packet = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                packet[i] = (byte) 0xff;
            }
            for (int i = 6; i < packet.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, packet, i, macBytes.length);
            }

            InetAddress address = InetAddress.getByName(broadcastIP);
            DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, port);
            DatagramSocket socket = new DatagramSocket();
            socket.send(datagramPacket);
            socket.close();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DynamicToast.make(MainActivity.this, "Magic Packet Sent", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getMacBytes(String macStr) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }

    private void shutdownRemotePC(String hostname, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("shutdown /s /f /t 0");
            channel.connect();

            channel.disconnect();
            session.disconnect();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DynamicToast.make(MainActivity.this, "Shutdown Command Sent", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sleepRemotePC(String hostname, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("rundll32.exe powrprof.dll, SetSuspendState Sleep");
            channel.connect();

            channel.disconnect();
            session.disconnect();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DynamicToast.make(MainActivity.this, "Sleep Command Sent", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rebootRemotePC(String hostname, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("shutdown /r /f /t 0");
            channel.connect();

            channel.disconnect();
            session.disconnect();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DynamicToast.make(MainActivity.this, "Reboot Command Sent", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProfile() {
        final String macAddress = macAddressEditText.getText().toString();
        final String broadcastIP = broadcastIPEditText.getText().toString();
        final String username = usernameEditText.getText().toString();
        final String password = passwordEditText.getText().toString();

        // Проверка корректности введенных данных
        if (!isValidMacAddress(macAddress)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DynamicToast.make(MainActivity.this, "Invalid MAC address", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        if (!isValidIPAddress(broadcastIP)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DynamicToast.make(MainActivity.this, "Invalid IP address", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Проверка SSH соединения
        if (!testSSHConnection(broadcastIP, DEFAULT_SSH_PORT, username, password)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DynamicToast.make(MainActivity.this, "SSH connection failed", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Сохранение профиля
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MAC_ADDRESS_KEY, macAddress);
        editor.putString(BROADCAST_IP_KEY, broadcastIP);
        editor.putInt(SSH_PORT_KEY, DEFAULT_SSH_PORT);
        editor.putString(USERNAME_KEY, username);
        editor.putString(PASSWORD_KEY, password);
        editor.apply();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DynamicToast.make(MainActivity.this, "Profile Saved", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteProfile() {
        // Удаление профиля
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(MAC_ADDRESS_KEY);
        editor.remove(BROADCAST_IP_KEY);
        editor.remove(SSH_PORT_KEY);
        editor.remove(USERNAME_KEY);
        editor.remove(PASSWORD_KEY);
        editor.apply();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DynamicToast.make(MainActivity.this, "Profile Deleted", Toast.LENGTH_SHORT).show();
                macAddressEditText.setText("");
                broadcastIPEditText.setText("");
                usernameEditText.setText("");
                passwordEditText.setText("");
            }
        });
    }

    private void loadProfile() {
        macAddressEditText.setText(sharedPreferences.getString(MAC_ADDRESS_KEY, ""));
        broadcastIPEditText.setText(sharedPreferences.getString(BROADCAST_IP_KEY, ""));
        usernameEditText.setText(sharedPreferences.getString(USERNAME_KEY, ""));
        passwordEditText.setText(sharedPreferences.getString(PASSWORD_KEY, ""));
    }

    private boolean isValidMacAddress(String macAddress) {
        return macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }

    private boolean isValidIPAddress(String ipAddress) {
        return ipAddress.matches(
                "^(([0-9]{1,3})\\.){3}([0-9]{1,3})$");
    }
}
