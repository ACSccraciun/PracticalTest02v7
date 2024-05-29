package ro.pub.cs.systems.eim.practicaltest02v7;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button serverStartBtn, setBtn, resetBtn, pollBtn;

    TextView outTxt;

    EditText serverPortTxt, clientPortTxt, clientAddrTxt, timeTxt;

    Integer PORT;
    String ADDR;

    ServerThread server;

    private class CommunicationThread extends Thread {
        private Socket socket;
        private String data;
        private HashMap<String, String> cache;
        public CommunicationThread(Socket socket, String data, HashMap<String, String> cache) {
            this.socket = socket;
            this.data = data;
            this.cache = cache;
        }


        @Override
        public void run() {
            try {
                Log.v(Constants.TAG, "Connection opened with "+socket.getInetAddress()+":"+socket.getLocalPort());
                PrintWriter printWriter = Utilities.getWriter(socket);

                ArrayList<String> allData = new ArrayList<>(Arrays.asList(data.split(",")));

                if (allData.get(0).equals("set")) {
                    String hour = allData.get(1);
                    String minute = allData.get(2).strip();

                    cache.put(socket.getInetAddress().toString(), hour + "," + minute);

                } else if (allData.get(0).equals("reset")) {
                    cache.put(socket.getInetAddress().toString(), "");
                } else if (allData.get(0).equals("poll")) {
                    if (!cache.containsKey(socket.getInetAddress().toString())) {
                        printWriter.println("none");
                    } else {
                        String time = cache.get(socket.getInetAddress().toString());
                        if (time == null) {
                            printWriter.println("none");
                        } else if (time.equals("")) {
                            printWriter.println("none");
                        }else {

                            ArrayList<String> timeList = new ArrayList<>(Arrays.asList(time.split(",")));

                            Integer hour = new Integer(timeList.get(0));
                            Integer minute = new Integer(timeList.get(1).strip());

                            Socket nist_socket = new Socket(Constants.NIST, Constants.NIST_PORT);

                            BufferedReader nist_reader = Utilities.getReader(nist_socket);
                            nist_reader.readLine();
                            String nist_data = nist_reader.readLine();

                            ArrayList<String> nistList = new ArrayList<>(Arrays.asList(nist_data.split(" ")));
                            String hourMinuteDate = nistList.get(1) + " " + nistList.get(2);
                            String hourMinute = Utilities.getHourAndMinute(hourMinuteDate);

                            ArrayList<String> currentTime = new ArrayList<>(Arrays.asList(hourMinute.split(",")));

                            Integer currentHour = new Integer(currentTime.get(0));
                            Integer currentMinute = new Integer(currentTime.get(1));

                            //printWriter.println(hour + " " + minute + " " + currentHour + " " + currentMinute);

                            if (currentHour < hour) {
                                printWriter.println("inactive");
                            } else if (currentHour > hour){
                                printWriter.println("active");
                            } else {
                                if (currentMinute <= minute) {
                                    printWriter.println("inactive");
                                } else {
                                    printWriter.println("active");
                                }
                            }
                        }
                    }

                }


                socket.close();
                Log.v(Constants.TAG, "Connection closed");
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private class ServerThread extends Thread {
        private boolean isRunning;

        private ServerSocket serverSocket;

        private HashMap<String, String> cache;

        public void startServer() {
            cache = new HashMap<>();
            isRunning = true;
            start();
            Log.d(Constants.TAG, "Started Server");
        }

        public void stopServer() {
            isRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                        Log.v(Constants.TAG, "stopServer() method invoked "+serverSocket);
                    } catch(IOException ioException) {
                        Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                        if (Constants.DEBUG) {
                            ioException.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT);
                while (isRunning) {
                    Socket socket = serverSocket.accept();

                    BufferedReader reader = Utilities.getReader(socket);
                    String data = reader.readLine();

                    new CommunicationThread(socket, data, cache).start();
                }
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "An exception has occurred: "+ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    private class ClientAsyncTask extends AsyncTask<Void, Void, String> {

        private String addr;
        private int port;

        private String data;

        String response;

        public ClientAsyncTask(String addr, int port, String data) {
            this.addr = addr;
            this.port = port;

            this.data = data;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Socket socket = new Socket(addr, port);
                PrintWriter writer = Utilities.getWriter(socket);
                writer.println(data);

                BufferedReader reader = Utilities.getReader(socket);
                response = reader.readLine();

                Log.d(Constants.TAG, "The server returned: " + response);
            } catch (UnknownHostException unknownHostException) {
                Log.d(Constants.TAG, unknownHostException.getMessage());
                if (Constants.DEBUG) {
                    unknownHostException.printStackTrace();
                }
            } catch (IOException ioException) {
                Log.d(Constants.TAG, ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            outTxt.setText(result);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02v7_main);

        serverStartBtn = findViewById(R.id.serverCreateBtn);
        setBtn = findViewById(R.id.setBtn);
        resetBtn = findViewById(R.id.resetBtn);
        pollBtn = findViewById(R.id.pollBtn);

        outTxt = findViewById(R.id.outTxt);

        serverPortTxt = findViewById(R.id.serverPortTxt);
        clientAddrTxt = findViewById(R.id.clientAddrTxt);
        clientPortTxt = findViewById(R.id.clientPortTxt);
        timeTxt = findViewById(R.id.timeTxt);

        serverStartBtn.setOnClickListener(v -> {
            PORT = new Integer(serverPortTxt.getText().toString());

            server = new ServerThread();
            server.startServer();
        });

        ADDR = clientAddrTxt.getText().toString();

        setBtn.setOnClickListener(v -> {
            ClientAsyncTask client = new ClientAsyncTask(ADDR, PORT, "set," + timeTxt.getText().toString());
            client.execute();
        });

        resetBtn.setOnClickListener(v -> {
            ClientAsyncTask client = new ClientAsyncTask(ADDR, PORT, "reset\n");
            client.execute();
        });

        pollBtn.setOnClickListener(v -> {
            ClientAsyncTask client = new ClientAsyncTask(ADDR, PORT, "poll\n");
            client.execute();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        server.stopServer();
    }
}