package com.example.myntpclock;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    // To be able to get internet
    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

    // Setting up global variables
    Button ntpButton;
    TextView clock;
    public static String NTP_SERVER = "0.pool.ntp.org";
    Long systemTime;
    boolean isClicked = true;
    long ntp_diff = 0;
    long time_now;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.setThreadPolicy(policy);

        // Converting millis to hour, minutes and second.
        SimpleDateFormat simpleDf = new SimpleDateFormat("HH:mm:ss");
        simpleDf.setTimeZone(TimeZone.getTimeZone("Europe/Stockholm"));

        // Connect the visual/graphic button and text, where clock is displayed.
        ntpButton = findViewById(R.id.button);
        clock = findViewById(R.id.textView);

        // Runs the simulated clock that's displayed on the app.
        runSimClock(simpleDf, ntpButton, clock);

        // Button listener if the button is clicked or not.
        ntpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.button:
                        if (checkConnection() && isClicked) {
                            ntpButton.setText("Stop ntp time");
                            isClicked = false;
                        } else {
                            ntpButton.setText("Start ntp time");
                            isClicked = true;
                        }
                }
            }
        });
    }

    public void runSimClock(SimpleDateFormat simpleDf, Button ntpButton, TextView clock) {

        // Create handler to update Ui.
        Handler uiHandler = new Handler();
        Runnable uiUpdate = () -> {
            // Updating Ui every second
            if (checkConnection() && isClicked) {
                clock.setText(simpleDf.format(time_now));
                ntpButton.setText("Stop ntp time");
                isClicked = true;
            } else {
                clock.setText(simpleDf.format(systemTime));
                ntpButton.setText("Start ntp time");
                isClicked = false;
            }
        };

        Thread sync_time = new Thread() {
            @Override
            // Always running every second.
            public void run() {
                while (true) {
                    try {
                        if (checkConnection() && isClicked) {
                            systemTime = Calendar.getInstance().getTimeInMillis();
                            // Calculating the time difference between system time and NTP server.
                            time_now = systemTime + ntp_diff;
                            uiHandler.post(uiUpdate);
                        } else {
                            // Updating with system time when offline.
                            systemTime = Calendar.getInstance().getTimeInMillis();
                            uiHandler.post(uiUpdate);
                        }
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        // Runs every 15th second.
        Thread sync_ntp = new Thread() {
            @Override
            public void run() {
                while (true) {
                    if (checkConnection() && isClicked) {
                        try {
                            // Calling the getOffset method to get the time difference
                            // Between NTP and system time.
                            getOffset();
                            Thread.sleep(15000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        // Starting both threads.
        sync_ntp.start();
        sync_time.start();
    }

    // To see if the device is online.
    public boolean checkConnection() {
        try {
            int timeoutMs = 1500;
            Socket sock = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);
            sock.connect(sockaddr, timeoutMs);
            sock.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // To get the millis from the NTP Server.
    public long getNtpTime() {
        final NTPUDPClient NTPClient = new NTPUDPClient();
        long millis = 0;
        try {
            InetAddress inet_address = InetAddress.getByName(NTP_SERVER);
            TimeInfo time_info = NTPClient.getTime(inet_address);
            millis = time_info.getMessage().getTransmitTimeStamp().getTime();
        } catch (IOException e) {
            NTPClient.close();
            return 0;
        }
        NTPClient.close();
        return millis;
    }

    // Check the difference between NTP server and system time.
    public void getOffset() {
        long ntp_time = getNtpTime();
        systemTime = Calendar.getInstance().getTimeInMillis();
        if (ntp_time > systemTime) {
            ntp_diff = ntp_time - systemTime;
        } else {
            ntp_diff = 0;
        }
    }
}