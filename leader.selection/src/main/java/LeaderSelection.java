import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderSelection implements Watcher{
    private static final String ZOOPKEEPER_ADDRESS = "localhost:2181";
    // it's client's responsibility to send heart beats before session's timeout periodically
    // to tell the zookeeper server it's alive and keeps maintaining its znode!!
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";
    private ZooKeeper zooKeeper;
    private String currentZnodeName;
    private static final String TARGET_ZNODE = "/target_znode";

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderSelection leaderSelection = new LeaderSelection();
        leaderSelection.connectToZookeeper();
        leaderSelection.volunteerForLeadership();
        leaderSelection.reelectLeader();
        leaderSelection.run();
        leaderSelection.close();
        System.out.println("Disconnected from zookeeper. Exiting!!");
    }

    public void volunteerForLeadership() throws KeeperException, InterruptedException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace("/election/", "");
    }

    public void reelectLeader() throws KeeperException, InterruptedException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            String smallestChild = children.get(0);

            if (smallestChild.equals(currentZnodeName)) {
                System.out.println("I am the leader");
                return;
            } else {
                System.out.println("I am not the leader");
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
            }
        }

        System.out.println("Watching znode " + predecessorZnodeName);
        System.out.println();
    }

    public void connectToZookeeper() throws IOException {
        // when creating a new instance of ZooKepper, a child thread IOThread is spawned to connect to
        // the zookeeper server
        // so whatever the main thread is going to execute, IOThread is going to try to connect to the server
        // When IOThread is connecting to the server, an event is fired then caught by EventThread!!!
        this.zooKeeper = new ZooKeeper(ZOOPKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void run() throws InterruptedException {
        // wait() inherited from Object and it blocks the thread that is running this method. In this case it's the main thread
        // since zookeeper spawns child threads for IO and Event, child threads are still running
        synchronized (zooKeeper) {
            this.zooKeeper.wait(); //put the current thread to sleep!!!
        }
    }

    public void close() throws InterruptedException {
        this.zooKeeper.close();
    }

    public void watchTargetZnode() throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(TARGET_ZNODE, this);  // read operation to a node
        if (stat == null) {
            return;
        }

        byte[] data = zooKeeper.getData(TARGET_ZNODE, this, stat); // read operation to a node
        List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this); // read operation to a node

        System.out.println("Data : " + new String(data) + ", children : " + children);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from Zookeeper event");
                        this.zooKeeper.notifyAll();
                    }
                }
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (KeeperException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//            case NodeDeleted:
//                System.out.println(TARGET_ZNODE + " was deleted");
//                break;
//            case NodeCreated:
//                System.out.println(TARGET_ZNODE + " was created");
//                break;
//            case NodeDataChanged:
//                System.out.println(TARGET_ZNODE + " data changed");
//                break;
//            case NodeChildrenChanged:
//                System.out.println(TARGET_ZNODE + " children changed");
//                break;
        }

//        try {
//            watchTargetZnode();  // watch the target node again!!
//        } catch (KeeperException e) {
//        } catch (InterruptedException e) {
//        }
    }
}
