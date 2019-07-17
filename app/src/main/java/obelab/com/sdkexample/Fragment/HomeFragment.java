package obelab.com.sdkexample.Fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.regex.Pattern;

import obelab.com.sdkexample.Activity.MainActivity;
import obelab.com.sdkexample.R;
import obelab.com.nirsitsdk.NirsitData;
import obelab.com.nirsitsdk.NirsitProvider;

public class HomeFragment extends Fragment {
    private final String TAG = "[OpenFragment]";
    TextView txtTime;

    final static int Init = 0;
    final static int Run = 1;

    int cur_Status = Init;
    int myCount = 1;

    long myBaseTime;
    long myPauseTime;

    String ip = "192.168.0.1";
    final int port = 50007;
    final int TIME_OUT = 3000;

    double [] splittedHbO2 = new double[16];
    double [] splittedHbR = new double[16];

    TextView data780TextView;
    TextView data850TextView;

    NirsitProvider nirsitProvider;

    public HomeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ViewPager vp;
        final Button btnStart = (Button) view.findViewById(R.id.btn_start);
        txtTime = (TextView) view.findViewById(R.id.txt_time);
        vp = (ViewPager) view.findViewById(R.id.vp_main_product);
        data780TextView = (TextView) view.findViewById(R.id.data780TextView);
        data850TextView = (TextView) view.findViewById(R.id.data780mTextView);

        nirsitProvider = new NirsitProvider(getActivity(), ip);
        nirsitProvider.setDataListener(new NirsitProvider.NirsitDataListener() {

            @Override
            public void onReceiveData(NirsitData data) {
                for (int i=16; i<32; i++) {
                    splittedHbO2[i - 16] = data.getHbO2()[i];
                    splittedHbR[i - 16] = data.getHbR()[i];
                }
                data780TextView.setText("[d780]\n" + Arrays.toString(splittedHbO2));
                data850TextView.setText("[d850]\n" + Arrays.toString(splittedHbR));
                Log.d("Time", data.getTimestamp());
                Log.d(TAG, "raw:" + data.getRaw());
            }

            @Override
            public void onDisconnected() {
                Toast.makeText(getActivity(), "onDisconnected()", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onReceiveDemodData(NirsitData data) {
                Log.d(TAG, "raw:" + data.getRaw());
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*      https://rjswn0315.tistory.com/23        */
                Log.i("STATE", "Button Pressed");

                switch (cur_Status) {
                    case Init:
                        myBaseTime = SystemClock.elapsedRealtime();
                        System.out.println(myBaseTime);
                        //myTimer이라는 핸들러를 빈 메시지를 보내서 호출
                        myTimer.sendEmptyMessage(0);
                        btnStart.setText("Stop");
                        cur_Status = Run;
                        nirsitProvider.startMonitoring();
                        break;

                    case Run:
                        Toast.makeText(getActivity(), txtTime.getText() + " 동안 학습하셨습니다.", Toast.LENGTH_SHORT).show();
                        myTimer.removeMessages(0);
                        myPauseTime = SystemClock.elapsedRealtime();
                        btnStart.setText("Start!");
                        cur_Status = Init;
                        if(nirsitProvider == null) { return; }
                        nirsitProvider.stopMonitoring();
                        txtTime.setText("00:00:00");
                }
            }
        });
        return view;
    }

    Handler myTimer = new Handler() {
        public void handleMessage(Message msg) {
            txtTime.setText(getTimeOut());
            myTimer.sendEmptyMessage(0);    // sendEmptyMessage는 비어있는 메시지를 Handler에게 전송.
        }
    };

    String getTimeOut() {
        long now = SystemClock.elapsedRealtime(); //애플리케이션이 실행되고 나서 실제로 경과된 시간
        long outTime = now - myBaseTime;
        String resultTime = String.format("%02d:%02d:%02d", (outTime / 1000) / 60, (outTime / 1000) % 60, (outTime % 1000) / 10);
        return resultTime;
    }
}
