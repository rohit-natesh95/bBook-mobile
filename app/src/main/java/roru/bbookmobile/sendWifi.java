package roru.bbookmobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class sendWifi extends Activity {

    private final String TAG = "sendWifi";
    EditText editText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifisend_activity);
    }

    public void sendWifiDetails(View view) {
        EditText editText = findViewById(R.id.wifiSSID);
        String SSID = editText.getText().toString();
        editText = findViewById(R.id.wifiPass);
        String pass = editText.getText().toString();
        Log.i(TAG, "sendWifi: " + SSID+"\tpass: "+pass);
        NearbySender nearbySender = new NearbySender(getApplicationContext());
        nearbySender.sendData(SSID+"101101"+pass,1);
        startActivity(new Intent(this, MainActivity.class));
    }
}
