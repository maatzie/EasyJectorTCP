package com.example.easyjector.util;


import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.json.JSONException;
import org.json.JSONObject;

public class TcpMessenger {
    private static TcpMessenger sInstance = null;
    private TcpClientServerAsync mServerInstance = null;
    private static InetAddress mTargetAddress = null;
    private static BufferedWriter out;
    private static BufferedReader in;

    private TcpMessenger() throws SocketException {
        mServerInstance = new TcpClientServerAsync();
        mServerInstance.execute();
        try {
            mTargetAddress = InetAddress.getByName("192.168.0.100");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static TcpMessenger getInstance() throws SocketException {
        if (sInstance == null) {
            sInstance = new TcpMessenger();
        }
        return sInstance;
    }


    public static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return null;
        }
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public void setTargetAddress(InetAddress targetAddress) {
        this.mTargetAddress = targetAddress;
    }

    public void setOnPacketReceiveListener(OnPacketReceiveListener listener) {
        mServerInstance.setOnPacketReceiveListener(listener);
    }

    public void sendData(InetAddress ip, JSONObject data) {
        mServerInstance.sendData(data);
    }

    public void sendData(JSONObject data) {
        if(mTargetAddress == null) {
            return;
        }
        sendData(mTargetAddress, data);
    }

    @Override
    protected void finalize() throws Throwable {
        mServerInstance.stopServer();
        super.finalize();
    }

    private static class TcpClientServerAsync extends AsyncTask<Void, Void, Void> {
        private static final int SERVER_PORT = 4020;
        private static final int CLIENT_PORT = 1234;
        private static final int TIMEOUT = 10;

        private OnPacketReceiveListener mOnPacketReceiveListener = null;
        private JSONObject mData = null;
        private Socket mSocket = null;
        private Boolean mKeepServerRunning = true;

        private TcpClientServerAsync() {

        }

        private void setOnPacketReceiveListener(OnPacketReceiveListener listener) {
            this.mOnPacketReceiveListener = listener;
        }

        private void sendData(JSONObject data) {
            this.mData = data;
        }

        private void stopServer() {
            mKeepServerRunning = false;
        }

        @Override
        protected Void doInBackground(Void... objects) {
            try
            {

                mSocket = new Socket(mTargetAddress, SERVER_PORT);
                in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                while (mKeepServerRunning) {
                    if (mData != null) {
                        send();
                        receive();
                    }

                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        void receive() {
            try {
                String inMsg = in.readLine();
                if(inMsg!=null) {
                    mOnPacketReceiveListener.onTCPMessageReceived(inMsg);
                }

            } catch (IOException ignored) {
            }
        }

        void send() {

            if (mData != null) {
                try {
                    out.write(mData.toString() + System.getProperty("line.separator"));
                    out.flush();
                    mData = null;
                } catch (IOException ignored) {
                }
            }
        }
    }

    public interface OnPacketReceiveListener {
        void onTCPMessageReceived(String msg);
    }
}

