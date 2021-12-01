package Acceptor;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;

public class Attachment {
    private HashMap<SocketChannel, Integer> inMsgSeqNoMap = new HashMap<>();
    private HashMap<SocketChannel, Integer> outMsgSeqNoMap = new HashMap<>();
    private HashMap<SocketChannel, Long> heartbeatTimeMap = new HashMap<>();
    private HashMap<SocketChannel, String> targetMap = new HashMap<>();
    private HashMap<SocketChannel, Integer> heartBtIntMap = new HashMap<>();

    public void putInMsgSeqNo(SocketChannel socketChannel, Integer inMsgSeqNo) {
        inMsgSeqNoMap.put(socketChannel, inMsgSeqNo);
    }

    public void putHeartBtInt(SocketChannel socketChannel, Integer heartBtInt) {
        heartBtIntMap.put(socketChannel, heartBtInt);
    }

    public void putOutMsgSeqNo(SocketChannel socketChannel, Integer OutMsgSeqNo) {
        outMsgSeqNoMap.put(socketChannel, OutMsgSeqNo);
    }

    public void putHeartbeatTime (SocketChannel socketChannel, Long time) {
        heartbeatTimeMap.put(socketChannel, time);
    }

    public void putTarget (SocketChannel socketChannel, String target) {
        targetMap.put(socketChannel, target);
    }

    public Integer getInMsgSeqNo(SocketChannel socketChannel) {
        return inMsgSeqNoMap.get(socketChannel);
    }

    public Integer getHeartBtInt(SocketChannel socketChannel) {
        return heartBtIntMap.get(socketChannel);
    }

    public Integer getOutMsgSeqNo(SocketChannel socketChannel) {
        return outMsgSeqNoMap.get(socketChannel);
    }

    public Long getHeartbeatTime(SocketChannel socketChannel) {
        return heartbeatTimeMap.get(socketChannel);
    }

    public String getTarget(SocketChannel socketChannel) {
        return targetMap.get(socketChannel);
    }

    public void removeInMsgSeqNo(SocketChannel socketChannel) {
        inMsgSeqNoMap.remove(socketChannel);
    }

    public void removeHeartBtInt(SocketChannel socketChannel) {
        heartBtIntMap.remove(socketChannel);
    }

    public void removeOutMsgSeqNo(SocketChannel socketChannel) {
        outMsgSeqNoMap.remove(socketChannel);
    }

    public void removeHeartbeatTime (SocketChannel socketChannel) {
        heartbeatTimeMap.remove(socketChannel);
    }

    public void removeTarget (SocketChannel socketChannel) {
        targetMap.remove(socketChannel);
    }

    public Set<SocketChannel> getAllConnection() {
        return heartbeatTimeMap.keySet();
    }

}
