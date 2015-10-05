package com.nanobiotechru.android.nanobiotechandroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SecondActivity extends AppCompatActivity{

    TextView buttonClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        ((Button)findViewById(R.id.button1)).setOnClickListener(secondActivityListener);
        ((Button)findViewById(R.id.button2)).setOnClickListener(secondActivityListener);
        ((Button)findViewById(R.id.button3)).setOnClickListener(secondActivityListener);

        buttonClicked = (TextView)findViewById(R.id.textView1);

    }

    private View.OnClickListener secondActivityListener = new View.OnClickListener(){
        public void onClick(View view){
            switch(view.getId()){
                case R.id.button1: // Result History
                    buttonClicked.setText("Result History Clicked");
                    break;
                case R.id.button2: // Most Recent Result
                    buttonClicked.setText("Most Recent Clicked");
                    break;
                case R.id.button3: // Collect Data
                    buttonClicked.setText("Collect Data Clicked");
                    break;
                default:
                    break;
            }
        }
    };
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_second, menu);
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
