package edu.buffalo.cse.cse486586.simpledynamo;

import android.os.Bundle;
import android.app.Activity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.TextView;

public class SimpleDynamoActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);
    
		TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(
                new Put1ClickListener(tv, getContentResolver()));
        findViewById(R.id.button2).setOnClickListener(
                new Put2ClickListener(tv, getContentResolver()));
        findViewById(R.id.button3).setOnClickListener(
                new Put3ClickListener(tv, getContentResolver()));
        findViewById(R.id.button4).setOnClickListener(
                new OnLDumpClickListener(tv, getContentResolver()));
        findViewById(R.id.button5).setOnClickListener(
                new GetClickListener(tv, getContentResolver()));

		
		
        tv.setMovementMethod(new ScrollingMovementMethod());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_dynamo, menu);
		return true;
	}

}
