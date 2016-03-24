package fi.oulu.mapcopter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import fi.oulu.mapcopter.event.CopterConnectionEvent;

public class StartActivity extends AppCompatActivity {

    private Bus eventBus;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        eventBus = MapCopterApplication.getDefaultBus();

        Button startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(StartActivity.this, MapActivity.class);
                StartActivity.this.startActivity(myIntent);

            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    @Subscribe
    public void onCopterConnectionChanged(CopterConnectionEvent event) {
        if (event.isConnected()) {
            TextView mInfo = (TextView) findViewById(R.id.textView_Connection);
            mInfo.setText("Yhdistetty");
        } else {
            TextView mInfo = (TextView) findViewById(R.id.textView_Connection);
            mInfo.setText("Ei yhteyttä");
        }
    }
}
