package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static ArrayList<String> REMOTE_PORT = new ArrayList<String>(Arrays.asList("11108", "11112", "11116", "11120", "11124"));//
    static final int SERVER_PORT = 10000;

    //Global variables for Msg identification
    static final String MSG = "MSG";
    static final String PRP = "PRP";
    static final String AGR = "AGR";

    //delimiters
    static final String msgDelimiter ="#%@";
    static final String processMsgDelimiter ="_";
    static volatile int activeClients =5;

    int insertKeySeqNum = 0;//{0,0,0,0,0};
    volatile int currentMaxAgreed=0;

    volatile int msgCounter=1;

    volatile int seqCounter=1;

    private final Object lock = new Object();

    private String thisPort=null;

    //Queues for Sender side processing
    Hashtable<String,String> senderMsgList= new Hashtable<String,String>();
    Hashtable<String,Integer> proposalTracker= new Hashtable<String, Integer>();
    Hashtable<String, ArrayList<Double>> proposalList= new Hashtable<String,ArrayList<Double>>();



    //Queues for Receiver side processing
    Hashtable<String,String> receivedMsgList= new Hashtable<String, String>();
    Hashtable<String,MessageAgreed> receiverProposalList= new Hashtable<String, MessageAgreed>();
    PriorityBlockingQueue<MessageAgreed> holdBackQueue= new PriorityBlockingQueue<MessageAgreed>(5);
    PriorityBlockingQueue<MessageAgreed> deliveryQueue= new PriorityBlockingQueue<MessageAgreed>(5);
    PriorityBlockingQueue<MessageAgreed> finalQueue= new PriorityBlockingQueue<MessageAgreed>(25);
    PriorityBlockingQueue<MessageAgreed> msgQueue= new PriorityBlockingQueue<MessageAgreed>(25,new MessageComparator());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * Use the TextView to display your messages. Though there is no grading component
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
         * You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        thisPort=myPort;

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Server Socket IOException from "+ myPort);
            return;
        } catch (Exception e){
            Log.e(TAG, "Server Socket Exception from "+ myPort);
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        final Button myButton = (Button) findViewById(R.id.button4);

        myButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                editText.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort, String.valueOf(msgCounter));
                String p_counter= myPort+ processMsgDelimiter + msgCounter;
                senderMsgList.put(p_counter, msg);
                proposalTracker.put(p_counter,0);
                proposalList.put(p_counter,new ArrayList<Double>()); //initializing a spot for the list of values.
                msgCounter++;
//                Log.v(TAG,"Exiting myButton click event");
            }
        });
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
            String resultMsg=null;

            try {
                while(true) {
                    Socket socket = serverSocket.accept();//socket that connects to individual clients
//                    socket.setSoTimeout(500);
                    InputStream is = socket.getInputStream();
                    DataInputStream bis = new DataInputStream(is);
                    byte[] incomingMsg = new byte[128];//msg maximum character size is expected to be 128
                    bis.read(incomingMsg);
                    socket.close(); //closing the socket once the msg is read

                    String msgReceived = new String(incomingMsg);
                    String[] msgs= msgReceived.split(msgDelimiter);

//                    int senderMsgCount;
//                    Log.v(TAG,"Server Task processing ==> "+ msgReceived);
                    switch(msgs[0]){
                         case MSG:
                            //MSG+ msgDelimiter + SenderPort+ processMsgDelimiter+ SenderMSgCount + msgDelimiter + MsgData;
//                            Log.d(TAG,"Server Task Case MSG Msgdata ==>" + msgs[2]);
                            receivedMsgList.put(msgs[1], msgs[2]);
//                            publishProgress(msgs[2]);
                            resultMsg=msgs[0]+msgDelimiter+msgs[1]; //MSG delimt SenderPort_SendermsgCounter
                            callClientTask(resultMsg);
//                            Log.d(TAG,"Server Task Case MSG End ");
                            break;
                        case PRP:
//                            Log.d(TAG,"Server Task Case Prp ");
                            //PRP delimt SenderPort_SenderMsgCount delimt proposedNum
//                            tempStr=msgs[1].split(processMsgDelimiter);
//                            String senderPort= tempStr[0];
//                            senderMsgCount=Integer.parseInt(tempStr[1]);
                            int trackerCount;
                            ArrayList<Double> tempProposalList=proposalList.get(msgs[1]);
                            if((trackerCount= proposalTracker.get(msgs[1])+1) < activeClients){
                                //still waiting for all receivers
                                tempProposalList.add(Double.parseDouble(msgs[2]));
//                                Log.d(TAG,msgs[1]+"==>"+ tempProposalList.toString());
                                proposalList.put(msgs[1],tempProposalList);
                                proposalTracker.put(msgs[1],trackerCount);
                            }else{
                                //all receivers replied
                                Double agreedSeq = Collections.max(tempProposalList);
                                resultMsg= msgs[0]+msgDelimiter+msgs[1]+msgDelimiter+String.valueOf(agreedSeq);
                                callClientTask(resultMsg);
//                                new AgreementClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgs[1], String.valueOf(agreedSeq));
                            }
//                            Log.d(TAG,"Server Task Case Prp END ");
                            break;
                        case AGR:
//                            Log.d(TAG,"Server Task Case ARG ");
                            //AGR delimt Process_MSgCount delimit seqNum
//                            String[] tempStr=msgs[1].split(processMsgDelimiter);
//                            int senderMsgCount=Integer.parseInt(tempStr[1]);
                            String msgData=receivedMsgList.get(msgs[1]);
                            if(msgData!=null && msgs[2]!=null) {
                                MessageAgreed msgAgreed = new MessageAgreed(Double.parseDouble(msgs[2]), msgs[1], msgData, true);
                                checkDeliveryQueue(msgs[1],msgAgreed);
//                                publishProgress(msgData);
                            }
//                            Log.d(TAG,"Server Task Case ARG END");
                    }

                }
            } catch(SocketTimeoutException ste){
                Log.e(TAG, "Server Task IOException ==>" + ste.getMessage());
            } catch(SocketException se){
                Log.e(TAG, "Server Task IOException ==>" + se.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Server Task IOException ==>" + e.getMessage());
            } catch (Exception e){
                Log.e(TAG, "Server Task Exception ==>" + e.getMessage());
            }
//            Log.d(TAG,"Server Task end with result ="+ resultMsg);
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

/*
            //Code for Inserting the msg - Starts
            Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put("key", Integer.toString(seqNum++));
            keyValueToInsert.put("value",strReceived);
            getContentResolver().insert(uri,keyValueToInsert);
            //Code for Inserting the msg - Ends
*/
        }

//        @Override
//        protected void onPostExecute(String msg) {
//
//        }

        protected void callClientTask(String msg){
//            Log.d(TAG,"Reached Post execute with msg==>"+msg);
            if(msg!=null) {
                String[] msgs = msg.split(msgDelimiter);
                switch (msgs[0]) {
                    case MSG:
                        //MSG delimt SenderPort_SendermsgCounter
                        new ProposalClientTask().execute(msgs[1]);
                        break;
                    case PRP:
                        new AgreementClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgs[1], msgs[2]);
                        break;
                    case AGR:

                }
            }
        }

        private synchronized void checkDeliveryQueue(String msgId,MessageAgreed msgAgreed){
            //remove the current msg from holdback queue and see if it is less agreed seq is less then the next heldbackMsg.
            //If so insert the msg else add the msg to deliveryqueue.

//            synchronized (lock) {
            holdBackQueue.remove(receiverProposalList.get(msgId));
            deliveryQueue.add(msgAgreed);

//                PriorityBlockingQueue<MessageAgreed> tempDeliveryQueue= deliveryQueue;
//
//                String deliveryQueueList ="";
//                for(MessageAgreed tempMsg;(tempMsg=tempDeliveryQueue.poll())!=null;){
//                    deliveryQueueList += tempMsg.getSeqNum() + "||||";
//                }
//                Log.w("DeliveryQueue values", deliveryQueueList);

            MessageAgreed heldBackMsg = holdBackQueue.peek();
//            if(heldBackMsg.getSeqNum()>msgAgreed.getSeqNum()){
//                insertData(msgAgreed.getMsgData());
            MessageAgreed tempMsgArd = deliveryQueue.peek();
//            Log.i("HoldBackQueue", holdBackQueue.toString());
//            Log.i("DeliveryQueue", deliveryQueue.toString());
            if (heldBackMsg != null) {
                while (true) {
                    if (tempMsgArd != null && heldBackMsg.getSeqNum() > tempMsgArd.getSeqNum()) {
//                        insertData(tempMsgArd.getMsgData());
                        finalQueue.add(tempMsgArd);
//                        Log.w("FinalQueue Size",finalQueue.size()+"");
                        deliveryQueue.poll();
                        tempMsgArd = deliveryQueue.peek();
                    } else
                        break;
                }
            } else {
                while (true) {
                    if (tempMsgArd != null) {
//                        insertData(tempMsgArd.getMsgData());
                        finalQueue.add(tempMsgArd);
//                        Log.w("FinalQueue Size",finalQueue.size()+"");
                        deliveryQueue.poll();
                        tempMsgArd = deliveryQueue.peek();
                    } else
                        break;
                }
            }
           /* PriorityBlockingQueue<MessageAgreed> dupFinalQueue = new PriorityBlockingQueue<MessageAgreed>(finalQueue);
            insertKeySeqNum=0;
            for(MessageAgreed tempMsg; (tempMsg=dupFinalQueue.poll())!=null;){
                insertData(tempMsg.getMsgData());
            }*/
//            }
        }


    }

    private void printQueue(PriorityBlockingQueue<MessageAgreed> objQueue){
        PriorityBlockingQueue<MessageAgreed> dupQueue = new PriorityBlockingQueue<MessageAgreed>(objQueue);
        insertKeySeqNum=0;
        for(MessageAgreed tempMsg; (tempMsg=dupQueue.poll())!=null;){
            insertData(tempMsg.getMsgData());
        }
    }

    private void insertData(String msgData){
        Uri uri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        ContentValues keyValueToInsert = new ContentValues();
        keyValueToInsert.put("key", Integer.toString(insertKeySeqNum++));
        keyValueToInsert.put("value",msgData);
        getContentResolver().insert(uri,keyValueToInsert);
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
        protected Void doInBackground(String... msgs) {  //msgs==>msg,portid,msgCount
            String currentPort = null;
            try {
                Socket socket;
                OutputStream os;
                DataOutputStream dos;
                String msgToSend = MSG+ msgDelimiter + msgs[1]+ processMsgDelimiter+ msgs[2] + msgDelimiter + msgs[0] +msgDelimiter;
//                Log.d(TAG, "ClientSocket msg ==>" +msgToSend);
                for(int i= activeClients-1;i>=0;i--) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT.get(i)));
                    currentPort= REMOTE_PORT.get(i);
                    socket.setSoTimeout(500);
                    os= socket.getOutputStream();
                    dos= new DataOutputStream(os);
                    dos.write(msgToSend.getBytes());
                    socket.close();
                }
            }catch(SocketTimeoutException ste){
                Log.e(TAG, "SocketTimeout Exception at port "+currentPort);
                removeFailedClient(currentPort);
            } catch(SocketException se){
                Log.e(TAG, "Socket Exception at port "+currentPort);
                removeFailedClient(currentPort);
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException"+currentPort);
                removeFailedClient(currentPort);
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException"+currentPort);
                removeFailedClient(currentPort);
            } catch(Exception e){
                Log.e(TAG, e.getMessage());
            }
            return null;
        }
    }

    /***
     * Used for unicassting proposal to sender
     */
    private class ProposalClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {  //msgs==>senderportid_msgCount
            String currentPort=null;
            try {
                Socket socket;
                OutputStream os;
                DataOutputStream dos;
                String[] tempStr=msgs[0].split(processMsgDelimiter);
                String senderPort= tempStr[0];
//                String senderMsgCount= tempStr[1];
                int seqNumtoSend;
//                Log.d(TAG, "Proposal Client Socket Chk1 ==>" +msgs[0]);
                synchronized (lock){
                    seqNumtoSend = seqCounter > currentMaxAgreed ? seqCounter : currentMaxAgreed;
                    seqCounter = seqNumtoSend+1;
                }
                String strSeqNumToSend;

                if(!thisPort.equals(null)) {
                    strSeqNumToSend = seqNumtoSend + "." + thisPort;
                }else
                    strSeqNumToSend= seqNumtoSend+".0";

                MessageAgreed objMessageAgreed= new MessageAgreed(Double.parseDouble(strSeqNumToSend),msgs[0],receivedMsgList.get(msgs[0]),false);
//                Log.w("AddToHoldBackqueue",objMessageAgreed.getSeqNum()+"<+++++>"+receivedMsgList.get(objMessageAgreed.getOriginalId()));
                holdBackQueue.add(objMessageAgreed);
                msgQueue.add(objMessageAgreed);
                printQueue(msgQueue);
                receiverProposalList.put(msgs[0],objMessageAgreed);
                String msgToSend = PRP+ msgDelimiter +msgs[0] + msgDelimiter + strSeqNumToSend+msgDelimiter; //PRP delimt SenderPort_SenderMsgCount delimt proposedNum
//                Log.d(TAG, "ProposalClientSocket msg Chk 2 ==>" +msgToSend);
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(senderPort));
                currentPort= senderPort;
                socket.setSoTimeout(500);
                os= socket.getOutputStream();
                dos= new DataOutputStream(os);
                dos.write(msgToSend.getBytes());
                socket.close();
            }catch(SocketTimeoutException ste){
                Log.e(TAG, "SocketTimeout Exception at port "+currentPort);
                removeFailedClient(currentPort);
            } catch(SocketException se){
                Log.e(TAG, "Socket Exception at port "+currentPort);
                removeFailedClient(currentPort);
            }  catch (UnknownHostException e) {
                Log.e(TAG, "Proposal Client  UnknownHostException"+currentPort);
                removeFailedClient(currentPort);
            } catch (IOException e) {
                Log.e(TAG, "Proposal Client  socket IOException"+currentPort);
                removeFailedClient(currentPort);
            } catch(Exception e){
                Log.e(TAG, "Proposal Client Exception ==>" + e.getMessage());
            }
            return null;
        }
    }

    /***
     * Used for multicasting agreed sequence number
     */
    private class AgreementClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {  //msgs==>senderportid_msgCount,seqNum.receiverPortNum
            String currentPort=null;
            try {
                Socket socket;
                OutputStream os;
                DataOutputStream dos;
                String msgToSend = AGR+ msgDelimiter + msgs[0]+ msgDelimiter + msgs[1]+msgDelimiter;//AGR delimt Process_MSgCount delimit seqNum.receiverPortNum
//                Log.v(TAG, "Agreement Client Socket msg ==>" +msgToSend);
                for(int i= activeClients-1;i>=0;i--) {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT.get(i)));
                    currentPort= REMOTE_PORT.get(i);
                    socket.setSoTimeout(500);
                    os= socket.getOutputStream();
                    dos= new DataOutputStream(os);
                    dos.write(msgToSend.getBytes());
                    socket.close();
                }

            } catch(SocketTimeoutException ste){
                Log.e(TAG, "SocketTimeout Exception at port "+currentPort);
                removeFailedClient(currentPort);
            } catch(SocketException se){
                Log.e(TAG, "Socket Exception at port "+currentPort);
                removeFailedClient(currentPort);
            } catch (UnknownHostException e) {
                Log.e(TAG, "Agreement ClientTask UnknownHostException"+currentPort);
                removeFailedClient(currentPort);
            } catch (IOException e) {
                Log.e(TAG, "Agreement ClientTask socket IOException"+currentPort);
                removeFailedClient(currentPort);
            } catch(Exception e){
                Log.e(TAG, "Agreement ClientTask" + e.getMessage());
            }
            return null;
        }
    }

    private synchronized void removeFailedClient(String failedClientPort){
        if(REMOTE_PORT.contains(failedClientPort)){
            REMOTE_PORT.remove(failedClientPort);
            activeClients=REMOTE_PORT.size();
            PriorityBlockingQueue<MessageAgreed> dupHoldBackQueue= new PriorityBlockingQueue<MessageAgreed>(holdBackQueue);
            for(MessageAgreed tempMsg;(tempMsg = dupHoldBackQueue.poll())!=null;){
                if(tempMsg.getOriginalId().contains(failedClientPort)){
                    holdBackQueue.remove(tempMsg);
                }
            }
            for(String p_count:proposalTracker.keySet()){
                int count=0;
                if(proposalTracker.get(p_count) == activeClients ){
                    ArrayList<Double> tempList= proposalList.get(p_count);
                    for(Double port_seq: tempList){

                        String[] tempStr=port_seq.toString().split("\\.");
                        if(tempStr[1].length()==4){
                            tempStr[1]+="0";
                        }
                        if(tempStr[1]==failedClientPort)
                            break;
                        else
                            count++;
                    }
                    if(count==activeClients){
                        Double agreedSeq = Collections.max(tempList);
                        String resultMsg= PRP+msgDelimiter+p_count+msgDelimiter+String.valueOf(agreedSeq);
                        new ServerTask().callClientTask(resultMsg);
                    }
                }
            }

        }

    }

}
