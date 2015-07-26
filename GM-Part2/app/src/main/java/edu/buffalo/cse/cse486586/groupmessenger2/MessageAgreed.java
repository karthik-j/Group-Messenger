package edu.buffalo.cse.cse486586.groupmessenger2;

/**
 * Created by karthikj on 3/14/15.
 */
public class MessageAgreed implements Comparable<MessageAgreed> {
    double seqNum;
    String originalId;
    String msgData;
    boolean isDeliverable;

    public MessageAgreed(double seqNum, String originalId, String msgData, boolean isDeliverable) {
        this.seqNum = seqNum;
        this.originalId = originalId;
        this.msgData = msgData;
        this.isDeliverable = isDeliverable;
    }

    public double getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(double seqNum) {
        this.seqNum = seqNum;
    }

    public String getMsgData() {
        return msgData;
    }

    public void setMsgData(String msgData) {
        this.msgData = msgData;
    }

    public boolean isDeliverable() {
        return isDeliverable;
    }

    public void setDeliverable(boolean isDeliverable) {
        this.isDeliverable = isDeliverable;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }

    @Override
    public int compareTo(MessageAgreed another) {

        if(this.seqNum > another.seqNum){
            return 1;
        }else if(this.seqNum < another.seqNum){
            return -1;
        }
        return 0;
    }
}
