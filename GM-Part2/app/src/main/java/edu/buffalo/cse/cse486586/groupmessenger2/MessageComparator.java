package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

/**
 * Created by karthikj on 3/14/15.
 */
public class MessageComparator implements Comparator<MessageAgreed> {
    @Override
    public int compare(MessageAgreed lhs, MessageAgreed rhs) {
        try {
            int lhs_port = Integer.parseInt(lhs.getOriginalId().split(GroupMessengerActivity.processMsgDelimiter)[0]);
            int rhs_port = Integer.parseInt(rhs.getOriginalId().split(GroupMessengerActivity.processMsgDelimiter)[0]);
            int lhs_count = Integer.parseInt(lhs.getOriginalId().split(GroupMessengerActivity.processMsgDelimiter)[1]);
            int rhs_count = Integer.parseInt(rhs.getOriginalId().split(GroupMessengerActivity.processMsgDelimiter)[1]);


            if (lhs_port > rhs_port) {
                return 1;
            } else if (lhs_port < rhs_port) {
                return -1;
            } else if (lhs_count > rhs_count) {
                return 1;
            } else if (lhs_count < rhs_count) {
                return -1;
            }
            return 0;
        }catch(Exception e){
            return 0;
        }

    }
}
