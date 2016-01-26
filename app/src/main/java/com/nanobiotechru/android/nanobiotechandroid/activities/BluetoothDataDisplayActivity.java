package com.nanobiotechru.android.nanobiotechandroid.activities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.List;

public class BluetoothDataDisplayActivity extends ActionBarActivity {

//test
    private final String BLE_MAC_ADDRESS = "20:C3:8F:D5:35:06";

    //UI ELEMENTS
    private EditText inputData;
    private TextView displayData;
    private Button sendData;
    private TextView pointsData;
    private TextView peakData;
    private Button analyzeData
    ;
    //TESTING (temporary solution)
    private static String[] dataString = new String[30000];
    private static int storedPoints = 0;

    //BLE STUFF
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothHelper helper;


    public static File checkExternalAvailable(String filename) {
        File file;
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File docsFolder = new File(Environment.getExternalStorageDirectory() + "/Documents/NBTA");
                boolean isPresent = true;
                if (!docsFolder.exists()) {
                    isPresent = docsFolder.mkdir();
                }
                if (isPresent) {
                    file = new File(docsFolder.getAbsolutePath(),filename);
                } else {
                    Log.e("BDDA", "Cannot Create Directory.");
                    file = null;
                }
            } else {
                file = null;
                Log.e("BDDA", "Cannot Create File.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            file = null;
        }
        return file;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_data_display);

        inputData = (EditText)findViewById(R.id.input_data_field);
        displayData = (TextView)findViewById(R.id.display_data_view);
        sendData = (Button)findViewById(R.id.send_data_btn);
        pointsData = (TextView)findViewById(R.id.display_points_view);
        peakData = (TextView)findViewById(R.id.display_peaks_view);
        analyzeData = (Button)findViewById(R.id.analyze_btn);


        String filename = "arddata.txt";
        final File file = checkExternalAvailable(filename);
        if(file != null) {
            peakData.setText("Ext. Storage: " + file.toString());
        }else peakData.setText("No Ext. Storage.");

        /*
        try{

        }catch(Exception e){
            e.printStackTrace();
        }
        */

        helper = new BluetoothHelper(BluetoothDataDisplayActivity.this,new BleWrapperUiCallbacks.Null(){

            @Override
            public void uiNewValueForCharacteristic(BluetoothGatt gatt,
                                                    BluetoothDevice device, BluetoothGattService service,
                                                    BluetoothGattCharacteristic ch, final String strValue, int intValue,
                                                    byte[] rawValue, String timestamp) {


               String val =  bytesToHex(rawValue);



                Log.d("BDDA", "Notification = " + strValue + " or " + val);

                //TODO: strValue is your data...for right now pretend it's just numbers, in whatever form you want it to be
                //create a method OUTSIDE the onCreate method, to do your filtering, and another one for peak detection
                //then call those methods on this data

                String filename = "arddata.txt";
                //File file = null;


                //FileOutputStream outputStream;
                FileWriter fw;
                BufferedWriter bw;

                try {

                    bw = new BufferedWriter(new FileWriter(file),1000);
                    bw.write(strValue);
                    bw.flush();
                    bw.close();
                    //outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    //outputStream.write(rawValue);
                    //outputStream.close();

                }
                catch (Exception e){
                    e.printStackTrace();
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayData.setText(strValue);
                        if(storedPoints < dataString.length){
                            dataString[storedPoints++] = strValue;
                        }
                    }
                });

                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        pointsData.setText("Points Received: " + storedPoints);
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

                    byte[] dataBytes = data.getBytes();                    //{(byte)integer};

                    dataString = new String[30000];
                    storedPoints = 0;

                    helper.writeDataToCharacteristic(characteristic,dataBytes);

            }
        });

        analyzeData.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String filename = "arddata.txt";
                double minPeakThreshold = .5;
                double peakFallRatio = .9;
                double[] data = null;


                data = parseData(filename, false);
                if(data != null){
                    /*
                    double[] smoothed;
                    smoothed = filterData(detrend(data));
                    smoothed = filterData(data);
                    boolean[] isPeak = findPeaks(smoothed, minPeakThreshold, peakFallRatio);
                    int numMaxes = countPeaks(isPeak);
                    */

                    int numMaxes = newFindPeak(data, 1.5);
                    peakData.setText("Num Peaks Found: " + numMaxes + "\nUsing " + data.length + " points.");
                }else{
                    peakData.setText("No Data Found");
                }

            }
        });

    }

    // Triggers on the rising and falling edge to detect a peak.
    public static int newFindPeak(double[] data, double threshold){
        boolean aboveThreshold = false;
        int peaksFound = 0;

        for(int i=0; i<data.length; i++){
            if(data[i] > threshold) aboveThreshold = true;
            else if(aboveThreshold && data[i]<threshold){
                peaksFound++;
                aboveThreshold = false;
            }

        }
        return peaksFound;
    }

    /** This code works on the complete data-set. Working on a point-by-point version.
     * double[] smoothedData = filterData(detrend(parseData(rawData)));
     * boolean[] peakArray = findPeaks(smoothedData, peakThreshold, dropFactor=0);
     */
    public static double[] parseData(String dataStream, boolean test){
        // Reading Input, customize according to source
        //return null;
        if(!test && storedPoints > 0) {
            StringBuilder sb = new StringBuilder();
            int j = 0;
            for(String s : dataString){
                if(j > storedPoints) break;
                sb.append(s);
                j++;
                Log.e("BDDA", "sb_append: \n[" + s + "]");
            }
            String superString = sb.toString();

            String[] values = superString.split("\\s+"); //\\s+
            double[] data = new double[values.length+1];
            int i = 0;
            for(String val : values){

                try {
                    data[i] = Double.valueOf(val);
                    i++;
                }catch(NumberFormatException e){
                    data[i] = -1;
                    i++;
                    //Log.e("BDDA", "NFE: " + val);
                }

            }
            return data;
        }else if(test){
            return null;
        }
        return null;
    }

    public static double[] detrend(double[] data){

        int points = data.length;
        double[] detrended = new double[points];

        // Removes trend from the dataset - helpful for peak-finding [(data) - (baseline)]
        // One-tenth the total sample size seems to be a good window size
        final int trendWindow = (int)(points/10);

        double sum = 0;
        for(int point = 0; point < points; point++){
            if(point < trendWindow){
                sum += data[point];
                detrended[point] = (data[point]) - (sum / (point+1));
            }else{
                sum = sum + data[point] - data[point - trendWindow];
                detrended[point] =(data[point]) - (sum / trendWindow);
            }
        }
        return detrended;
    }

    public static double[] filterData(double[] data){

        //Moving Sum smoothing (not sure if this is the best way, but simple and without use of ext. libs)
        // n-point (equally weighted) causal moving average
        // y[n] = (s[n] + s[n-1] + ...)/n;
        // goes through twice, one pass for each number in sampleRates

        // These values can be tinkered with to change the characteristics of the smoothing
        // Lower number = high-freq smoothing; Higher number = low-freq smoothing
        // Values chosen depend on the sampling frequency of the Arduino
        final int[] sampleRates = {5};
        int passes = sampleRates.length;

        int points = data.length;
        double[] movingAvg = new double[points];
        double[] tempArray;
        tempArray = data.clone();

        for(int pass=0; pass<passes; pass++){

            if(pass > 0) tempArray = movingAvg.clone();

            double sum = 0;
            for(int point = 0; point < points; point++){
                if(point < sampleRates[pass]){
                    sum += tempArray[point];
                    movingAvg[point] = sum / (point+1);
                }else{
                    sum = sum + tempArray[point] - tempArray[point - sampleRates[pass]];
                    movingAvg[point] = sum / sampleRates[pass];
                }
            }
        }
        return movingAvg;
    }

    public static boolean[] findPeaks(double[] data, double minPeakThreshold){ return findPeaks(data, minPeakThreshold, 0); }

    public static boolean[] findPeaks(double[] data, double minPeakThreshold, double peakFallRatio){
        int[] peak = {-1, -1}; // {max index (if not -1), min index (resume search there)}
        boolean[] maximums = new boolean[data.length];
        for(int i=0; i<data.length; i++){
            if(data[i] > minPeakThreshold){
                peak = checkForPeaks(data, i, peakFallRatio);
                if(peak[0] > -1){
                    maximums[peak[0]] = true;
                    i = peak[1];
                }
            }
        }
        return maximums;
    }

    private static int[] checkForPeaks(double[] data, int index, double peakFallRatio){
        double max = 0; double min = 0;
        int maxdex = 0; int mindex = 0;
        boolean foundMax = false;
        boolean foundMin = false;
        int counter = index;
        while(!foundMax && counter<data.length){
            if(data[counter] >= max) max = data[counter];
            else{
                foundMax = true;
                maxdex = counter;
                min = data[maxdex];
            }
            if(counter > index + 1000000){ System.out.println("Error: Could not find max."); break; }
            counter++;
        }
        while(!foundMin && counter<data.length){
            if(data[counter] <= min) min = data[counter];
            else{
                foundMin= true;
                mindex = counter;
                min = data[mindex];
            }
            if(counter > index + 1000000){ System.out.println("Error: Could not find min."); break; }
            counter++;
        }
        int[] result = {-1,-1};
        if(max-min > peakFallRatio*max){
            result[0] = maxdex;
            result[1] = mindex;
        }
        return result;
    }

    public static int countPeaks(boolean[] peaks){
        int numMaxes = 0;
        for(int i=0; i<peaks.length; i++){
            if(peaks[i]) numMaxes++;
        }
        return numMaxes;
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
        // as you specify a parent activity in AndroidManifest.xml
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
