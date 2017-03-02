package edu.wpi.alcowatch.alcowatch;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.actions.ReserveIntents;

/**
 * Phone activity that allows a user to see that they are too drunk to drive and allows them to click basically a "save me, I'm too drunk" button.
 * @author Benjamin Bianchi
 * @version  1
 */
public class DrunkActivity extends Activity {
    /**
     * The Callback referenced from the Shared Preferences.
     */
    String callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drunk);
        /**
         * Get callback from intent.
         */
        Button button = (Button) findViewById(R.id.callbackButton);
        Bundle data = getIntent().getExtras();
        if (data != null) {
            callback = data.getString("callback");
            button.setText(callback);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /**
                     * If killswitch is selected... TODO implement.
                     */
                    if (callback.equals("kill"))
                        return;

                    /**
                     * If uber is selected... Call a cab.
                     */
                    else if (callback.equals("uber")) {
                        Intent intent = new Intent(ReserveIntents.ACTION_RESERVE_TAXI_RESERVATION);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    } else {
                        /**
                         * If a contact is selected, then call that person.
                         */
                        if (callback.startsWith("contact-")) {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:" + callback.split("contact-")[1]));
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        }

                    }

                }
            });
        }
    }
}
