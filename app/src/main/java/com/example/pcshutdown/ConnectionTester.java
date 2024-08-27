package com.example.pcshutdown;

import static com.example.pcshutdown.MainActivity.logger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;

public class ConnectionTester {
    // Проверка доступности порта
    public static boolean isPortOpen(String ipAddress, int port) {
        try (Socket socket = new Socket()) {  // Используем try-with-resources для автоматического закрытия сокета
            socket.connect(new InetSocketAddress(ipAddress, port), 2000);  // Timeout в 2 секунды
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Проверка SSH соединения
    public static boolean testSSHConnection(String hostname, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no"); // Отключаем проверку ключа хоста
            session.setConfig(config);
            session.connect(3000); // Timeout в 3 секунды

            session.disconnect();
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to establish SSH connection to " + hostname + " on port " + port, e);
            return false;
        }
    }

    // Проверка подключения к Wifi
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }

        return false;
    }
}
