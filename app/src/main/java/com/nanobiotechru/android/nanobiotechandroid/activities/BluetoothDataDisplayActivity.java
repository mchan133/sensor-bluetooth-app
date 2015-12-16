package com.nanobiotechru.android.nanobiotechandroid.activities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.nanobiotechru.android.nanobiotechandroid.R;
import com.nanobiotechru.android.nanobiotechandroid.bluetooth.BluetoothHelper;
import com.nanobiotechru.android.nanobiotechandroid.bluetooth.interfaces.BleWrapperUiCallbacks;
import com.nanobiotechru.android.nanobiotechandroid.bluetooth.utils.BleDefinedUUIDs;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class BluetoothDataDisplayActivity extends ActionBarActivity {


    private final String BLE_MAC_ADDRESS = "20:C3:8F:D5:35:06";

    //UI ELEMENTS
    private EditText inputData;
    private TextView displayData;
    private Button sendData;


    //BLE STUFF
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothHelper helper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_data_display);

        inputData = (EditText)findViewById(R.id.input_data_field);
        displayData = (TextView)findViewById(R.id.display_data_view);
        sendData = (Button)findViewById(R.id.send_data_btn);

        helper = new BluetoothHelper(BluetoothDataDisplayActivity.this,new BleWrapperUiCallbacks.Null(){

            @Override
            public void uiNewValueForCharacteristic(BluetoothGatt gatt,
                                                    BluetoothDevice device, BluetoothGattService service,
                                                    BluetoothGattCharacteristic ch,final String strValue, int intValue,
                                                    byte[] rawValue, String timestamp) {


               String val =  bytesToHex(rawValue);



                Log.d("BDDA", "Notification = " + strValue + " or " + val);


                String filename = "arddata.txt";

                FileOutputStream outputStream;

                try {
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);

                    outputStream.write(rawValue);

                    outputStream.close();

                }
                catch (Exception e){
                    e.printStackTrace();
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayData.setText(strValue);
                    }
                });




            }
            @Override
            public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device,
                                          BluetoothGattService service,
                                          BluetoothGattCharacteristic characteristic) {

                Log.d("BDDA","Got notificiation");

            }

            @Override
            public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device,
                                            List<BluetoothGattService> services) {

                Log.d("BDDA", "Got services");

                for(BluetoothGattService s : services){
                    if(s.getUuid().equals(BleDefinedUUIDs.Service.CUSTOM_SERVICE)){
                        service = s;
                        Log.d("BDDA","Found our service");
                        helper.getCharacteristicsForService(s);
                    }
                }


            }
            @Override
            public void uiCharacteristicForService(BluetoothGatt gatt,
                                                   BluetoothDevice device, BluetoothGattService service,
                                                   List<BluetoothGattCharacteristic> chars) {


                Log.d("BDDA","Got characteristics");

                for(BluetoothGattCharacteristic chara : chars){

                    if(chara.getUuid().equals(BleDefinedUUIDs.Characteristic.CUSTOM_CHARACTERISTIC)){

                        Log.d("BDDA","Found our characteristic");

                        characteristic = chara;
                        helper.setNotificationForCharacteristic(chara,true);

                    }

                }

            }

            @Override
            public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {

                Log.d("BDDA","device connected");

                helper.getSupportedServices();

            }
            @Override
            public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {

                Log.d("BDDA","device disconnected");

            }

            @Override
            public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device,
                                          BluetoothGattService service, BluetoothGattCharacteristic ch,
                                          String description) {

                Log.d("BDDA,","success! " + description);

            }
            @Override
            public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device,
                                      BluetoothGattService service, BluetoothGattCharacteristic ch,
                                      String description) {
                Log.d("BDDA,","fail! " + description);

            }

        });

        helper.initialize();

        helper.connect(BLE_MAC_ADDRESS);
        

        sendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    String data = inputData.getText().toString();

                    //int integer = Integer.parseInt(data);

                    //Log.d("BDDA","int value = " + integer);

                    byte[] dataBytes = data.getBytes();
                    //{(byte)integer};

                    helper.writeDataToCharacteristic(characteristic,dataBytes);

            }
        });

    }


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int byteArrayToInt(byte[] b)
    {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_data_display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
