package info.motodell.arduino_android_dht11;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final String DEVICE_ADDRESS="98:D3:36:00:B0:B0";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    String string;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Button startButton, sendButton,clearButton,stopButton;
    TextView textView;
    EditText editText;
    boolean deviceConnected = false;
    Thread thread;
    byte buffer[];
    boolean stopThread;

    public MainActivity() {
        string = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = findViewById(R.id.buttonStart);
        sendButton = findViewById(R.id.buttonSend);
        clearButton = findViewById(R.id.buttonClear);
        stopButton = findViewById(R.id.buttonStop);
        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);
        setUiEnabled(false);
    }
    private void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);
    }
    public boolean BTinit() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesn't Support Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if(bluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if(bondedDevices.isEmpty()) {
                Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
            } else {
                for (BluetoothDevice iterator : bondedDevices) {
                    if(iterator.getAddress().equals(DEVICE_ADDRESS)) {
                        device = iterator;
                        found = true;
                        break;
                    }
                }
            }
        } else {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return found;
    }
    public boolean BTconnect() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
        if(connected) {
            try {
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            textView.append("" + inputStream.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connected;
    }
    public void onClickStart(View view) {
        if(BTinit()) {
            if(BTconnect()) {
                setUiEnabled(true);
                deviceConnected = true;
//                beginListenForData();
                textView.append("\nConnection Opened!\n");
            }
        }
    }
    void beginListenForData() {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
//        Toast.makeText(getApplicationContext(),"Holis",Toast.LENGTH_SHORT).show();
        Thread thread  = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopThread)  {
                    try {
                        int byteCount = socket.getInputStream().available();
                        if(byteCount > 0) {

                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            string = new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run() {
                                    textView.append(string);
                                }
                            });
                        }
                    }
                    catch (IOException ex) {

                        stopThread = true;
                    }
                }
            }
        });
        thread.start();
    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        string = string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();

        }
        textView.append("\nSent Data: "+string+"\n");

        try {
            int byteCount = inputStream.available();
            textView.append("\nSent Data: "+byteCount+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickStop(View view) throws IOException {
        stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected = false;
        textView.append("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        textView.setText("");
    }
}
