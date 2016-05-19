package fi.oulu.mapcopter.remote;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Button connectButton = (Button) findViewById(R.id.button_connect);
        if (connectButton != null) {
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(StartActivity.this, MapActivity.class);
                    startActivity(intent);
                }
            });
        }
    }
}
