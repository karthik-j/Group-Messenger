package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORT = {"11108","11112","11116","11120","11124"};
//    static final String REMOTE_PORT1 = "11112";
//    static final String REMOTE_PORT2 = "11116";
//    static final String REMOTE_PORT3 = "11120";
//    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;

    static int seqNum = 0;//{0,0,0,0,0};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
//            Log.e(TAG, "ServerSocket Check 1");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
//            Log.e(TAG, "ServerSocket Check 2");
        } catch (IOException e) {
//            e.printStackTrace();
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        final Button myButton = (Button) findViewById(R.id.button4);

        myButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
//                TextView localTextView = (TextView) findView
// one way to display a string.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

        /*
         * Register an OnKeyListener for the input box. OnKeyListener is an event handler that
         * processes each key event. The purpose of the following code is to detect an enter key
         * press event, and create a client thread so that the client thread can send the string
         * in the input box over the network.
         */
//        editText.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
//                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
//                    /*
//                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
//                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
//                     * an AsyncTask that sends the string to the remote AVD.
//                     */
//
////                    TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
////                    remoteTextView.append("\n");
//
//                    /*
//                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
//                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
//                     * the difference, please take a look at
//                     * http://developer.android.com/reference/android/os/AsyncTask.html
//                     */
//
//                    return true;
//                }
//                return false;
//            }
//        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while(true) {
                    Socket socket = serverSocket.accept();//socket that connects to individual clients
                    InputStream is = socket.getInputStream();
                    DataInputStream bis = new DataInputStream(is);
                    byte[] incomingMsg = new byte[128];//msg maximum character size is expected to be 128
                    bis.read(incomingMsg);
                    socket.close(); //closing the socket once the msg is read
                    publishProgress(new String(incomingMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0].trim();
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived);
            localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

//            GroupMessengerProvider contentProvider= new GroupMessengerProvider();
            Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", Integer.toString(seqNum++));
            keyValueToInsert.put("value",strReceived);
            getContentResolver().insert(uri,keyValueToInsert);
//            String key = (String) keyValueToInsert.get("key");
//            String val = (String) keyValueToInsert.get("value");

//            Cursor resultCursor = getContentResolver().query(uri, null, key, null, null);
//            if (resultCursor == null) {
//                Log.e(TAG, "Result null");
////                throw new Exception();
//            }
//
//            int keyIndex = resultCursor.getColumnIndex("key");
//            int valueIndex = resultCursor.getColumnIndex("value");
//            if (keyIndex == -1 || valueIndex == -1) {
//                Log.e(TAG, "Wrong columns");
//                resultCursor.close();
////                throw new Exception();
//            }
//
//            resultCursor.moveToFirst();
//
//            if (!(resultCursor.isFirst() && resultCursor.isLast())) {
//                Log.e(TAG, "Wrong number of rows");
//                resultCursor.close();
////                throw new Exception();
//            }
//
//            String returnKey = resultCursor.getString(keyIndex);
//            String returnValue = resultCursor.getString(valueIndex);
//            if (!(returnKey.equals(key) && returnValue.equals(val))) {
//                Log.e(TAG, "(key, value) pairs don't match\n key="+returnKey + "  Value="+returnValue);
//                resultCursor.close();
////                throw new Exception();
//            }else{
//                Log.e(TAG,"Values Saved key="+returnKey + "  Value="+returnValue);
//            }
//
//            resultCursor.close();
        }
    }

    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Socket socket;
                OutputStream os;
                DataOutputStream dos;
                String msgToSend = msgs[0];
                for(int i= REMOTE_PORT.length-1;i>=0;i--) {
//                    Log.e(TAG, "ClientSocket Check 3"+REMOTE_PORT[i]);
//                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTE_PORT[i]));
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i]));
                    os= socket.getOutputStream();
//                    Log.e(TAG, "ClientSocket Check 1"+REMOTE_PORT[i]);
                    dos= new DataOutputStream(os);
                    dos.write(msgToSend.getBytes());
//                    dos.flush();
//                    dos.close();
                    socket.close();
                }
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */

            } catch (UnknownHostException e) {
//                e.printStackTrace();
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
//                e.printStackTrace();
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}
