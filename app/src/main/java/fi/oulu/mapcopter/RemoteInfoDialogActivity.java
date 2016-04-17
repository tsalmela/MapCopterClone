package fi.oulu.mapcopter;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class RemoteInfoDialogActivity extends AppCompatActivity{

    private static final String PREF_REMOTE_IP = "remoteIp";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Remote access info");
        setContentView(R.layout.remote_access_info);
        ButterKnife.bind(this);

        String remoteIp = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(PREF_REMOTE_IP, "");

        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(PREF_REMOTE_IP, "1.1.1.1")
                .apply();
    }



    @OnClick(R.id.button_remoteAccess_ok)
    public void onOKClicked(){
        finish();
    }


}
