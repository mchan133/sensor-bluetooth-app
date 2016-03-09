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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Calendar;


import android.content.Context;
import android.graphics.Color;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.*;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

public class BluetoothDataDisplayActivity extends ActionBarActivity {


    //private final String BLE_MAC_ADDRESS = "20:C3:8F:D5:35:06"; old string
    private final String BLE_MAC_ADDRESS = "74:DA:EA:B2:67:9A";

    //UI ELEMENTS
    private EditText inputData;
    private TextView displayData;
    private Button sendData;
    private TextView pointsData;
    private TextView peakData;
    private Button analyzeData;

    //TESTING (temporary)
    private static int storedPoints = 0;
    private final String filename_raw = "arddata_raw.txt";

    //BLE STUFF
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothHelper helper;

    //PLOTTING
    private static final int SAMPLE_HISTORY = 1000;
    private XYPlot plot;
    private SimpleXYSeries liadata;


    public static File makeExternalFile(String filename) {
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

    //Function for updating the plot maybe update every 5-10?
    public synchronized void updatePlot(Number val) {
        if(liadata.size() > SAMPLE_HISTORY){
            liadata.removeFirst();
        }
        liadata.addLast(null, val);
        plot.redraw();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_data_display);


        //Text fields and buttons
        //inputData = (EditText)findViewById(R.id.input_data_field);
        //displayData = (TextView)findViewById(R.id.display_data_view);
        sendData = (Button)findViewById(R.id.send_data_btn);
        //pointsData = (TextView)findViewById(R.id.display_points_view);
        peakData = (TextView)findViewById(R.id.display_peaks_view);
        analyzeData = (Button)findViewById(R.id.analyze_btn);

        //plotting
        plot = (XYPlot)findViewById(R.id.plot);
        liadata = new SimpleXYSeries("Sensor Data");
        plot.setRangeBoundaries(-5, 5, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 1100, BoundaryMode.FIXED);
        plot.addSeries(liadata, new LineAndPointFormatter(Color.rgb(100, 100, 200), null, null, null));
        plot.setDomainStepValue(11);
        plot.setTicksPerRangeLabel(1);
        plot.setDomainLabel("Sample Index");
        plot.setRangeLabel("Voltage [V]");
        plot.getDomainLabelWidget().pack();
        plot.getRangeLabelWidget().pack();

        /*
        // setup checkboxes:
        hwAcceleratedCb = (CheckBox) findViewById(R.id.hwAccelerationCb);
        final PlotStatistics levelStats = new PlotStatistics(1000, false);
        final PlotStatistics histStats = new PlotStatistics(1000, false);

        aprLevelsPlot.addListener(levelStats);
        aprHistoryPlot.addListener(histStats);
        hwAcceleratedCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    aprLevelsPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                } else {
                    aprLevelsPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            }
        });
        showFpsCb = (CheckBox) findViewById(R.id.showFpsCb);
        showFpsCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                levelStats.setAnnotatePlotEnabled(b);
                histStats.setAnnotatePlotEnabled(b);
            }
        });
        */






        //TODO: change to one central filename
        String filename = "arddata_raw.txt";
        final File file_bytes = makeExternalFile(filename);
        if(file_bytes != null) {
            peakData.setText("Ext. Storage: " + file_bytes.toString());
        }else peakData.setText("No Ext. Storage.");

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

                BufferedWriter bw;
                try {

                    // need checks for no ext. storage
                    bw = new BufferedWriter(new FileWriter(file_bytes, true),1000);
                    for(byte byt : rawValue){
                        bw.write((byt & 0xFF) + "\n");
                    }
                    bw.flush();
                    bw.close();
                    storedPoints++;

                }
                catch (Exception e){
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        // should be 20 bytes/packet
                        pointsData.setText("approx. Points Received: " + storedPoints*20);
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

                    //String data = inputData.getText().toString();
                    String data = "a";

                    byte[] dataBytes = data.getBytes();                    //{(byte)integer};

                    storedPoints = 0;

                    helper.writeDataToCharacteristic(characteristic,dataBytes);

            }
        });

        analyzeData.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String filename = "arddata_raw.txt";
                double minPeakThreshold = .5;
                double peakFallRatio = .9;
                double[] data = null;


                data = parseData(file_bytes);
                if(data != null){
                    int numMaxes = newFindPeak(data, .5);
                    /* Old Find Peak
                    double[] smoothed;
                    smoothed = filterData(detrend(data));
                    smoothed = filterData(data);
                    boolean[] isPeak = findPeaks(smoothed, minPeakThreshold, peakFallRatio);
                    int numMaxes = countPeaks(isPeak);
                    */

                    peakData.setText("Num Peaks Found: " + numMaxes + "\nUsing " + data.length + " received points.");
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
    public static double[] parseData(File f){
        // Reading Input, customize according to source
        //return null;

        if(f!= null && f.exists()) {
            // Reading Input
            FileInputStream fStream;
            BufferedReader bReader = null;
            int points = 0;

            int[] tempData = new int[(int) (f.length()/2)];
            try{
                fStream = new FileInputStream(f);
                bReader = new BufferedReader(new InputStreamReader(fStream));

                // Reading ints from file
                while(bReader.ready()){
                    tempData[points] = Integer.parseInt(bReader.readLine());
                    points++;
                }
            }catch(Exception e){
                System.out.println("Exception caught in first try/catch");
                System.out.println(e.getMessage());
            }finally{
                try{bReader.close();}
                catch(Exception ex){/*ignore*/}
            }

            // Conversion to double
            double[] doubleData = new double[points];
            for(int i=0; i<points; i++){
                doubleData[i] = 2.0*((double)tempData[i]*(5.0/256)) - 5.0;
            }

            //save as text file
            //TODO: Check for file creation failure
            File file_double = makeExternalFile("arddata_double_" + System.currentTimeMillis() + ".txt");
            BufferedWriter bw;
            try {

                bw = new BufferedWriter(new FileWriter(file_double, true),1000);
                for(double d: doubleData){
                    bw.write(d + "\n");
                }
                bw.flush();
                bw.close();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                f.delete();
            }

            return doubleData;

        }else{
            Log.e("BDDA", f.toString() + " does not exist.");
            return null;
        }

        /*
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
        */
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
        // Values chosen depend on the sampling frequency of the Arduino (about 800-1000 Hz)
        final int[] sampleRates = {20};
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
