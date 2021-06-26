package com.example.easyjector;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.example.easyjector.util.TcpMessenger;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity implements TcpMessenger.OnPacketReceiveListener {

    private TcpMessenger mUdpMessenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("msg", "client request");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mUdpMessenger.sendData(obj);
            }
        });
        try {
            mUdpMessenger = TcpMessenger.getInstance();

        } catch (SocketException e) {

        }
    }

    @Override
    public void onTCPMessageReceived(String msg) {
        Log.i("Client received", msg);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mUdpMessenger.setOnPacketReceiveListener(this);
    }
}