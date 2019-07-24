package obelab.com.sdkexample.Fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

import obelab.com.nirsitsdk.NirsitData;
import obelab.com.nirsitsdk.NirsitProvider;
import obelab.com.sdkexample.R;

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

    TextView dataHbO2TextView;
    TextView dataHbRTextView;
    TextView inputDataTextView;

    NirsitProvider nirsitProvider;

    double[] splittedHbO2 = new double[16];
    double[] splittedHbR = new double[16];

    ArrayList<double[]> inputData = new ArrayList();

    //timer
    long outTime;

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
        dataHbRTextView = (TextView) view.findViewById(R.id.dataHbRTextView);
        dataHbO2TextView = (TextView) view.findViewById(R.id.dataHbO2TextView);
        inputDataTextView = (TextView) view.findViewById(R.id.inputDataTextView);

        nirsitProvider = new NirsitProvider(getActivity(), ip);
        nirsitProvider.setMbll(true);
        nirsitProvider.setLpf(true);
        nirsitProvider.setHeartbeat(true);
        nirsitProvider.setDataListener(new NirsitProvider.NirsitDataListener() {
            @Override
            public void onReceiveData(NirsitData data) {
                double[] splittedHbO2HbR = new double[33];
                String resultTime = String.format("%02d.%02d", (outTime / 1000), (outTime % 1000));
                for (int i = 16; i < 32; i++) {
                    splittedHbO2[i - 16] = data.getHbO2()[i];
                    splittedHbR[i - 16] = data.getHbR()[i];
                    splittedHbO2HbR[0] = Double.parseDouble(resultTime);     //timestamp
                    splittedHbO2HbR[2 * i - 31] = data.getHbO2()[i];    //2k+1
                    splittedHbO2HbR[2 * i - 30] = data.getHbR()[i];     //2(k+1)
                }
                Log.d("현주", data.getTimestamp());


                inputData.add(splittedHbO2HbR);
                dataHbO2TextView.setText("[d780]\n" + Arrays.toString(splittedHbO2));
                dataHbRTextView.setText("[d850]\n" + Arrays.toString(splittedHbR));
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
                        //System.out.println(myBaseTime);
                        //myTimer이라는 핸들러를 빈 메시지를 보내서 호출
                        myTimer.sendEmptyMessage(0);
                        btnStart.setText("Stop");
                        cur_Status = Run;
                        nirsitProvider.startMonitoring();
                        inputDataTextView.setText("[InputData]\n");
                        inputData.clear();
                        break;

                    case Run:
                        Toast.makeText(getActivity(), txtTime.getText() + " 동안 학습하셨습니다.", Toast.LENGTH_SHORT).show();
                        myTimer.removeMessages(0);
                        myPauseTime = SystemClock.elapsedRealtime();
                        btnStart.setText("Start!");
                        cur_Status = Init;
                        if (nirsitProvider == null) {
                            return;
                        }
                        nirsitProvider.stopMonitoring();
                        txtTime.setText("00:00:00");

                        String input = "";
                        for (int i = 0; i < inputData.size(); i++) {
                            input = input.concat(Arrays.toString(inputData.get(i))).concat(("\n"));
                        }
                        inputDataTextView.setText("[InputData]\n" + input);
                        //Log.v("현주", input);
                        System.out.println(input);
                        //writeCSV(inputData);
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
        outTime = now - myBaseTime;
        String resultTime = String.format("%02d:%02d:%02d", (outTime / 1000) / 60, (outTime / 1000) % 60, (outTime % 1000) / 10);
        return resultTime;
    }

//    public void writeCSV(ArrayList<double[]> input) {
//        //현재 인코딩 확인
//        String enc = new java.io.OutputStreamWriter(System.out).getEncoding();
//        System.out.println("현재 인코딩 : " + enc);
//
//        try {
//            //csv 파일을 생성한다.
////            String csvFilePath = "C:\\Users\\a0104\\Desktop\\testFile.csv";
////
////
////            BufferedWriter writer = new BufferedWriter(
////                    new OutputStreamWriter(new FileOutputStream(csvFilePath), "MS949")
////            );
////            FileOutputStream fileOutputStream = new FileOutputStream(csvFilePath);
////            OutputStreamWriter OutputStreamWriter = new OutputStreamWriter(fileOutputStream, "MS949"); // csv파일은 MS949로 인코딩.
////            BufferedWriter writer = new BufferedWriter(OutputStreamWriter);
//
////            BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath, true));
//            String csvFilePath = getContext().getFilesDir().getPath().toString() + "/testAndroid.csv";
//            Log.v("엑셀 경로", getContext().getFilesDir().getPath());
//
//
//            BufferedWriter writer = new BufferedWriter(
//                    new OutputStreamWriter(new FileOutputStream(csvFilePath), "MS949")
//            );
//
//            Log.v("엑셀", "성공");
//
//            for (int i = 0; i< input.size(); i++){
//                double[] splittedHbO2HbR = input.get(i);
//                writer.write(splittedHbO2HbR.toString());
//            }
//            writer.flush();
//            writer.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Log.v("엑셀", "또 그 문제");
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.v("엑셀", "실패");
//        }
//    }
}