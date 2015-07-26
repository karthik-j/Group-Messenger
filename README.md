# Group-Messenger
*Distributed Systems CS586 - Spring 2015 class project, UB*

Group Messenger with a Local Persistent Key-Value Table with Total and FIFO Ordering Guarantees
------------------------------------------------------------------------------------------------

This assignment had 2 parts.

**Part 1**

I had designed a group messenger that can send message to multiple AVDs and store them in a permanent key-value storage.

**Part 2**

In this part I added ordering guarantees to the group messenger develped in part-1. The guarantees I implemented are total ordering as well as FIFO ordering. As with part 1, I stored all the messages in my content provider. The difference is that when I store the messages and assign sequence numbers, my mechanism provide total and FIFO ordering guarantees. I designed the algorithm that did this and implement it. An important thing was that there were testing which included failure of an app instance in the middle of the execution. which had to be handled.
