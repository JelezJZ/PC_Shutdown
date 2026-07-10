package com.example.pcshutdown;

import static com.example.pcshutdown.MainActivity.logger;

import android.app.Activity;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.pranavpandey.android.dynamic.toasts.DynamicToast;

import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.logging.Level;

public class ActionManager {
    private final WeakReference<Activity> activityReference;

    public ActionManager(Activity activity) {
        this.activityReference = new WeakReference<>(activity);
    }

    private void sendSshCommand(String hostname, String username, String password, String command, String successMessage) {
        try {
            Activity activity = activityReference.get();
            if (activity == null) return;

            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, Constants.DEFAULT_SSH_PORT);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(3000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.connect();

            channel.disconnect();
            session.disconnect();

            activity.runOnUiThread(() -> DynamicToast.make(activity, successMessage, Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "SSH Execution failed: ", e);
        }
    }

    // Метод включения пк
    public void wakeOnLan(String macAddress, String broadcastIP) {
        try {
            Activity activity = activityReference.get();

            byte[] macBytes = getMacBytes(macAddress);
            byte[] packet = new byte[6 + 16 * macBytes.length];
            for (int i = 0; i < 6; i++) {
                packet[i] = (byte) 0xff;
            }
            for (int i = 6; i < packet.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, packet, i, macBytes.length);
            }

            InetAddress address = InetAddress.getByName(broadcastIP);
            DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, address, Constants.DEFAULT_PORT);
            DatagramSocket socket = new DatagramSocket();
            socket.send(datagramPacket);
            socket.close();

            activity.runOnUiThread(() -> DynamicToast.make(activity, "Magic Packet Sent", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred: ", e);
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

    // Метод выключения пк
    public void shutdownRemotePC(String hostname, String username, String password, String osType) {
        String command = "LINUX".equals(osType) ? "systemctl poweroff" : "shutdown /s /f /t 0";
        sendSshCommand(hostname, username, password, command, "Shutdown Command Sent");
    }

    // Метод перевода пк в спящий режим
    public void sleepRemotePC(String hostname, String username, String password, String osType) {
        String command = "LINUX".equals(osType) ? "systemctl suspend" : "rundll32.exe powrprof.dll,SetSuspendState Sleep";
        sendSshCommand(hostname, username, password, command, "Sleep Command Sent");
    }

    // Метод перезагрузки пк
    public void rebootRemotePC(String hostname, String username, String password, String osType) {
        String command = "LINUX".equals(osType) ? "systemctl reboot" : "shutdown /r /f /t 0";
        sendSshCommand(hostname, username, password, command, "Reboot Command Sent");
    }
}
