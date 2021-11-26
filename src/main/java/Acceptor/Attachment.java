package Acceptor;

import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class Attachment {
    private HashMap<SocketChannel, Integer> inMsgSeqNoMap = new HashMap<>();
    private HashMap<SocketChannel, Integer> outMsgSeqNoMap = new HashMap<>();

    public void putInMsgSeqNo(SocketChannel socketChannel, Integer inMsgSeqNo) {
        inMsgSeqNoMap.put(socketChannel, inMsgSeqNo);
    }

    public void putOutMsgSeqNo(SocketChannel socketChannel, Integer OutMsgSeqNo) {
        outMsgSeqNoMap.put(socketChannel, OutMsgSeqNo);
    }

    public Integer getInMsgSeqNo(SocketChannel socketChannel) {
        return inMsgSeqNoMap.get(socketChannel);
    }

    public Integer getOutMsgSeqNo(SocketChannel socketChannel) {
        return outMsgSeqNoMap.get(socketChannel);
    }

    public void removeInMsgSeqNo(SocketChannel socketChannel) {
        inMsgSeqNoMap.remove(socketChannel);
    }

    public void removeOutMsgSeqNo(SocketChannel socketChannel) {
        outMsgSeqNoMap.remove(socketChannel);
    }
}
