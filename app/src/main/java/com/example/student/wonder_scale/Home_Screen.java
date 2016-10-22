package com.example.student.wonder_scale;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;

import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static java.lang.Double.parseDouble;

public class Home_Screen extends AppCompatActivity {

    //Global variables used throughout the app
    AutoCompleteTextView foodName;
    AutoCompleteTextView servingSize;
    AutoCompleteTextView calories;
    AutoCompleteTextView carbs;
    EditText CalTotal;
    EditText Weight;
    RadioButton Grams;
    RadioButton Ounces;
    LineChart Lchart;
    private int totalCalories = 0;
    private ArrayList<Integer> calList = new ArrayList<>();
    ArrayList<Entry> list = new ArrayList<>();
    ArrayList<String> labels = new ArrayList<>();
    LineDataSet dataSet = new LineDataSet(list,"# of Calories");
    //Bluetooth
    TextView ConnectionStatus;
    ListView pairedListView;
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    //public static String EXTRA_DEVICE_ADDRESS;//used to take the MAC address to the next activity
    //Communication bluetooth
    Button GetWeight;
    EditText displayWeight;
    //private BluetoothAdapter BtAdapter;
    private BluetoothSocket BtSocket;
    private OutputStream outStream;

    // UUID service - This is the type of Bluetooth device that the BT module is
    // It is very likely yours will be the same, if not google UUID for your manufacturer
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module
    public String newAddress;


    @Override//used every time the app is created to set up the environment
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home__screen);

        //variable used to fill and look at different fields within the app
        foodName = (AutoCompleteTextView)findViewById(R.id.autoFoodName);
        servingSize = (AutoCompleteTextView)findViewById(R.id.autoServingSize);
        calories = (AutoCompleteTextView)findViewById(R.id.autoCalories);
        carbs = (AutoCompleteTextView)findViewById(R.id.autoCarbs);
        CalTotal = (EditText)findViewById(R.id.calTotal);
        Grams = (RadioButton)findViewById(R.id.grams);
        Grams.setClickable(false);
        Ounces = (RadioButton)findViewById(R.id.ounces);
        Weight = (EditText)findViewById(R.id.calculatedWeight);
        Lchart = (LineChart)findViewById(R.id.graph);

        //create a blank graph
        LineData data = new LineData(labels,dataSet);
        Lchart.setData(data);
        Lchart.setDescription("Calorie Chart");

        //Bluetooth
        ConnectionStatus = (TextView) findViewById(R.id.connecting);
        ConnectionStatus.setTextSize(40);
        //initialize array adapter for paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        //find and set up the ListView for paired devices
        pairedListView = (ListView) findViewById(R.id.devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        addKeyListener();
        //Initialize button
        GetWeight = (Button) findViewById(R.id.getWeight);

        //getting the bluetooth adapter value and calling checkBTstate function
        //BtAdapter = BluetoothAdapter.getDefaultAdapter();
        //checkBTState();

        GetWeight.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendData("R");
                Toast.makeText(getBaseContext(), "Getting Weight", Toast.LENGTH_SHORT).show();
            }
        });

    }



    @Override//save the state of the app
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt("totalCalories", totalCalories);
        outState.putIntegerArrayList("calList", calList);
        outState.putParcelableArrayList("listValues", list);
        outState.putStringArrayList("labelValues", labels);

    }

    @Override//restore the state of the app
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        totalCalories = savedInstanceState.getInt("totalCalories");
        calList = savedInstanceState.getIntegerArrayList("calList");
        labels = savedInstanceState.getStringArrayList("labelValues");
        list =  savedInstanceState.getParcelableArrayList("listValues");
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Pausing can be the end of an app if the device kills it or the user doesn't open it again
        //close all connections so resources are not wasted

        //Close BT socket to device
        try{
            BtSocket.close();
        } catch (IOException e2) {
            Toast.makeText(getBaseContext(), "ERROR - Failed to close Bluetooth socket", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //best to check BT status onResume in case something changed
        checkBTState();

        mPairedDevicesArrayAdapter.clear();// clears the array so items aren't duplicated when resuming from onPause

        ConnectionStatus.setText(" "); //makes the textview blank

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices and append to pairedDevices list
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Add previously paired devices to the array
        if (pairedDevices.size() > 0) {
            findViewById(R.id.Paired_Devices).setVisibility(View.VISIBLE);//make title viewable
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            mPairedDevicesArrayAdapter.add("no devices paired");
        }



    }
    //method to check if the device has Bluetooth and if it is on.
    //Prompts the user to turn it on if it is off
    private void checkBTState()
    {
        // Check device has Bluetooth and that it is turned on
        mBtAdapter=BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if(mBtAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!mBtAdapter.isEnabled()) {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
       /* // Check device has Bluetooth and that it is turned on
        if(BtAdapter==null) {
            Toast.makeText(getBaseContext(), "ERROR - Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!BtAdapter.isEnabled()) {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }*/
    }
    //takes the UUID and creates a com socket
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }
    // Method to send data
    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        try {
            //attempt to place data on the outStream to the BT device
            outStream.write(msgBuffer);
        } catch (IOException e) {
            //if the sending fails this is most likely because device is no longer there
            Toast.makeText(getBaseContext(), "ERROR - Device not found", Toast.LENGTH_SHORT).show();
            ConnectionStatus.setText(R.string.Error);
        }
    }
    public void addKeyListener() {

        // get display component
        displayWeight = (EditText) findViewById(R.id.calculatedWeight);

        // add a key listener to keep track user input
        displayWeight.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                // if key down and send is pressed implement the sendData method
                if ((keyCode == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
                    //I have put the * in automatically so it is no longer needed when entering text
                    sendData('*' + displayWeight.getText().toString());
                    Toast.makeText(getBaseContext(), "Sending text", Toast.LENGTH_SHORT).show();

                    return true;
                }

                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
    //used to write fields to a txt file to be used at a later time as well as build up the components for the graph
    public void save(View v){
        if((calories.getText().toString().equals("")))
            Toast.makeText(getApplicationContext(), "Please Enter a number in calories", Toast.LENGTH_SHORT).show();
        else{
            try {
                //write to a txt file to keep track of calories
                FileOutputStream TrackCal = openFileOutput("calories.txt", MODE_APPEND);
                OutputStreamWriter trackCal = new OutputStreamWriter(TrackCal);
                FileOutputStream totalCal = openFileOutput("Totalcalories.txt", MODE_PRIVATE);
                OutputStreamWriter TotalCalories = new OutputStreamWriter(totalCal);
                //read();
                //totalCalories = Integer.parseInt(CalTotal.getText().toString());
                //Add up calories
                totalCalories += Integer.parseInt(calories.getText().toString());
                calList.add(totalCalories);

                TotalCalories.write(CalTotal.getText().toString());
                trackCal.write(calories.getText().toString());
                trackCal.close();
                totalCal.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            graph();

            //Reset all text fields
            foodName.setText("");
            servingSize.setText("");
            calories.setText("");
            carbs.setText("");

            //used to hide keyboard
            InputMethodManager input = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            input.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            //set new total calories
            CalTotal.setText(String.valueOf(totalCalories));

            //Display conformation of saved text
            Toast.makeText(getApplicationContext(), "All Fields saved", Toast.LENGTH_SHORT).show();
        }
    }

    //used to build the graph
    public void graph(){
        //Populate the graph
        list.add(new Entry(calList.get(calList.size() - 1), calList.size() - 1));

        //timestamp
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm", Locale.US);
        String format = simpleDateFormat.format(new Date());
        System.out.println("date : " + format);

        //Populate the labels for the graph
        labels.add(format);

        //make the curve of the line graph color under it
        dataSet.setDrawCubic(true);
        dataSet.setDrawFilled(true);


        //Put data into the chart and add animation and reload graph
        LineData data = new LineData(labels, dataSet);
        Lchart.setData(data);
        Lchart.animateXY(1000, 1000);
        Lchart.setDescription("Calorie Chart");
        Lchart.invalidate();
    }

   //used to read the txt file and fill an array to be used later on
    /*public void read() throws IOException {

        String Message;
        FileInputStream inputStream = openFileInput("Totalcalories.txt");
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuffer stringBuffer = new StringBuffer();
        while ((Message=bufferedReader.readLine()) != null)
            stringBuffer.append(Message);
        if((Message=bufferedReader.readLine()) == null)
            CalTotal.setText("0");
        else
            CalTotal.setText(String.valueOf(Message));

    }*/

    public void reset(View v){

        totalCalories = 0;
        calList.clear();
        list.clear();
        labels.clear();
        LineData data = new LineData(labels,dataSet);
        Lchart.setData(data);
        Lchart.invalidate();
        CalTotal.setText(String.valueOf(totalCalories));
    }

    //used to convert grams to ounces and vise versa. Ounces to grams: Multiply by 28.3495 Grams to ounces: Multiply by .0353
    public void convert(View v) {

        double tempweight = parseDouble(Weight.getText().toString());

        if(Ounces.isChecked()) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            decimalFormat.setRoundingMode(RoundingMode.CEILING);
            Weight.setText(String.valueOf(decimalFormat.format(tempweight * .0353)));
            Ounces.setClickable(false);
            Grams.setClickable(true);
        }
        else {
            Weight.setText(String.valueOf(Float.valueOf(Math.round(tempweight * 28.3495))));
            Grams.setClickable(false);
            Ounces.setClickable(true);
        }

    }
    // Set up on-click listener for the listview
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
        {
            ConnectionStatus.setText(R.string.Connected);
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            newAddress = info.substring(info.length() - 17);


            //bluetooth Connection
            BluetoothDevice device = mBtAdapter.getRemoteDevice(newAddress);

            //Attempt to create a bluetooth socket for com
            try {
                BtSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e1) {
                Toast.makeText(getBaseContext(), "ERROR - Could not create Bluetooth socket", Toast.LENGTH_SHORT).show();
                ConnectionStatus.setText(R.string.Error);
            }
            try {
                BtSocket.connect();
            } catch (IOException e) {
                try {
                    BtSocket.close();        //If IO exception occurs attempt to close socket
                } catch (IOException e2) {
                    Toast.makeText(getBaseContext(), "ERROR - Could not close Bluetooth socket", Toast.LENGTH_SHORT).show();
                    ConnectionStatus.setText(R.string.Error);
                }
            }
            // Create a data stream so we can talk to the device
            try {
                outStream = BtSocket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "ERROR - Could not create bluetooth outStream", Toast.LENGTH_SHORT).show();
                ConnectionStatus.setText(R.string.Error);
            }
            //When activity is resumed, attempt to send a piece of junk data ('x') so that it will fail if not connected
            // i.e don't wait for a user to press button to recognise connection failure
            sendData("x");

        }
    };
}
