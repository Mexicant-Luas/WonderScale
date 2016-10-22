package com.example.student.wonder_scale;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.github.mikephil.charting.utils.Utils;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

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
    public static String EXTRA_DEVICE_ADDRESS;//used to take the MAC address to the next activity


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
        //Bluetooth
        ConnectionStatus = (TextView) findViewById(R.id.connecting);
        ConnectionStatus.setTextSize(40);
        //initialize array adapter for paired devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        //find and set up the ListView for paired devices
        pairedListView = (ListView) findViewById(R.id.devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);

        //create a blank graph
        LineData data = new LineData(labels,dataSet);
        Lchart.setData(data);
        Lchart.setDescription("Calorie Chart");


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
}
