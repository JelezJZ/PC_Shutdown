package com.example.pcshutdown;

import static com.example.pcshutdown.MainActivity.logger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelExec;

import java.io.IOException;
import java.io.InputStream;

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
    public static String testSSHConnection(String hostname, int port, String username, String password) {
        Session session = null;
        ChannelExec channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, hostname, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no"); // Отключаем проверку ключа хоста
            session.setConfig(config);

            session.connect(3000); // Timeout в 3 секунды

            // --- Начало блока определения ОС ---
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("uname -s");

            java.io.InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            StringBuilder response = new StringBuilder();
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    response.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(50);
            }

            String osName = response.toString().trim().toLowerCase();
            if (osName.contains("linux")) {
                return "LINUX";
            } else {
                return "WINDOWS";
            }
            // --- Конец блока определения ОС ---

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to connect or detect OS for " + hostname, e);
            return "UNKNOWN";
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
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
