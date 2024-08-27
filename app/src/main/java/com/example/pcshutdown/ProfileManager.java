package com.example.pcshutdown;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.Toast;

import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import java.lang.ref.WeakReference;

public class ProfileManager {
    private final EditText macAddressEditText;
    private final EditText broadcastIPEditText;
    private final EditText usernameEditText;
    private final EditText passwordEditText;
    private final SharedPreferences sharedPreferences;
    private final WeakReference<Activity> activityReference;

    // Конструктор для инициализации полей
    public ProfileManager(EditText macAddressEditText, EditText broadcastIPEditText,
                          EditText usernameEditText, EditText passwordEditText,
                          SharedPreferences sharedPreferences, Activity activity) {
        this.macAddressEditText = macAddressEditText;
        this.broadcastIPEditText = broadcastIPEditText;
        this.usernameEditText = usernameEditText;
        this.passwordEditText = passwordEditText;
        this.sharedPreferences = sharedPreferences;
        this.activityReference = new WeakReference<>(activity);
    }

    // Проверка существования профиля
    public boolean isProfileNotAvailable() {
        String broadcastIP = sharedPreferences.getString("broadcastIP", null);
        String macAddress = sharedPreferences.getString("macAddress", null);
        String username = sharedPreferences.getString("username", null);
        String password = sharedPreferences.getString("password", null);

        return broadcastIP == null || macAddress == null || username == null || password == null;
    }

    // Загрузка профиля
    public void loadProfile() {
        macAddressEditText.setText(sharedPreferences.getString(Constants.MAC_ADDRESS_KEY, ""));
        broadcastIPEditText.setText(sharedPreferences.getString(Constants.BROADCAST_IP_KEY, ""));
        usernameEditText.setText(sharedPreferences.getString(Constants.USERNAME_KEY, ""));
        passwordEditText.setText(sharedPreferences.getString(Constants.PASSWORD_KEY, ""));
    }

    // Сохранение профиля
    public void saveProfile() {
        Activity activity = activityReference.get();

        final String macAddress = macAddressEditText.getText().toString().trim();
        final String broadcastIP = broadcastIPEditText.getText().toString().trim();
        final String username = usernameEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();

        // Проверка корректности введенных данных
        if (!isValidMacAddress(macAddress)) {
            activity.runOnUiThread(() -> DynamicToast.make(activity, "Invalid MAC address", Toast.LENGTH_SHORT).show());
            return;
        }

        if (!isValidIPAddress(broadcastIP)) {
            activity.runOnUiThread(() -> DynamicToast.make(activity, "Invalid IP address", Toast.LENGTH_SHORT).show());
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.MAC_ADDRESS_KEY, macAddress);
        editor.putString(Constants.BROADCAST_IP_KEY, broadcastIP);
        editor.putInt(Constants.SSH_PORT_KEY, Constants.DEFAULT_SSH_PORT);
        editor.putString(Constants.USERNAME_KEY, username);
        editor.putString(Constants.PASSWORD_KEY, password);
        editor.apply();

        activity.runOnUiThread(() -> DynamicToast.make(activity, "Profile Saved", Toast.LENGTH_SHORT).show());
    }

    // Удаление профиля
    public void deleteProfile() {
        Activity activity = activityReference.get();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Constants.MAC_ADDRESS_KEY);
        editor.remove(Constants.BROADCAST_IP_KEY);
        editor.remove(Constants.SSH_PORT_KEY);
        editor.remove(Constants.USERNAME_KEY);
        editor.remove(Constants.PASSWORD_KEY);
        editor.apply();

        activity.runOnUiThread(() -> {
            DynamicToast.make(activity, "Profile Deleted", Toast.LENGTH_SHORT).show();
            macAddressEditText.setText("");
            broadcastIPEditText.setText("");
            usernameEditText.setText("");
            passwordEditText.setText("");
        });
    }

    // Проверка Mac-адреса
    public boolean isValidMacAddress(String macAddress) {
        return macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }

    // Проверка ip-адреса
    public boolean isValidIPAddress(String ipAddress) {
        return ipAddress.matches("^(([0-9]{1,3})\\.){3}([0-9]{1,3})$");
    }
}
