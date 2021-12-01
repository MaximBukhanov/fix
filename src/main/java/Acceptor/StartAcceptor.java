package Acceptor;

import quickfix.fix42.Heartbeat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StartAcceptor {
    private final int TIMEOUT = 20;
    private HashMap<SocketChannel, ByteBuffer> sessions = new HashMap<>();
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private Attachment attachment = new Attachment();
    private TradeAppAcceptor factoryMessage = new TradeAppAcceptor();

    StartAcceptor(InetSocketAddress address) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(address);

        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                start();
                checkHeartbeatTime();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0,5, TimeUnit.MILLISECONDS);
    }

    public void start() throws IOException{
        if (selector.select(TIMEOUT * 1000) > 0) {
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                keys.remove();
                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        read(key);
                    } else if (key.isWritable()) {
                        write(key);
                    }
                }
            }
            selector.selectedKeys().clear();
        } else {
            System.out.println(">>Timeout");
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.configureBlocking(false);
        ByteBuffer writeBuffer = sessions.get(socketChannel);
        ByteBuffer writeBufferPrint = sessions.get(socketChannel);
        sessions.remove(socketChannel);
        String message = "";

        if (writeBuffer != null) {
            socketChannel.write(writeBuffer);
            attachment.putHeartbeatTime(socketChannel, System.currentTimeMillis());
        }

        if (writeBufferPrint != null) {
            message = new String(writeBufferPrint.array());
            System.out.println(">>Server write: " + message);
            if (message.contains("35=5")) {
                disconnect(key, socketChannel);
                return;
            }
        }

        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();
        socketChannel.configureBlocking(false);
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int numRead = -1;

        try {
            numRead = socketChannel.read(readBuffer);
        } catch (SocketException e) {
            disconnect(key, socketChannel);
            e.printStackTrace();
            return;
        }

        System.out.println("\n>>Server read: " + numRead + " byte(s)");
        byte[] bytesRead = readBuffer.array();
        String message = new String(bytesRead, StandardCharsets.UTF_8).trim();
        message = message + "\u0001";
        System.out.println(">>Received message: " + message);

        if (numRead == -1) {
            disconnect(key, socketChannel);
            return;
        }

        ByteBuffer response = factoryMessage.createExecutionReport(socketChannel, message, attachment);

        sessions.put(socketChannel, response);
        socketChannel.register(selector, SelectionKey.OP_WRITE);
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        attachment.putInMsgSeqNo(socketChannel, 0);
        attachment.putOutMsgSeqNo(socketChannel, 0);

        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("\n>>New Connection: " + socketChannel.getRemoteAddress());
    }

    private void disconnect(SelectionKey key, SocketChannel socketChannel) throws IOException {
        System.out.println("!!>Disconnect..." + socketChannel.getRemoteAddress());
        sessions.remove(socketChannel);
        attachment.removeInMsgSeqNo(socketChannel);
        attachment.removeOutMsgSeqNo(socketChannel);
        attachment.removeHeartbeatTime(socketChannel);
        attachment.removeTarget(socketChannel);
        attachment.removeHeartBtInt(socketChannel);
        key.cancel();
        socketChannel.close();
    }

    private void checkHeartbeatTime() throws ClosedChannelException {
        for (SocketChannel socketChannel : attachment.getAllConnection()) {
            Long heartbeatTime = attachment.getHeartbeatTime(socketChannel);
            if ((heartbeatTime != null) && (System.currentTimeMillis() - heartbeatTime > attachment.getHeartBtInt(socketChannel) * 1000)) {
                Integer outMsgSeqNo = attachment.getOutMsgSeqNo(socketChannel);
                outMsgSeqNo++;

                Heartbeat heartbeat = new Heartbeat();
                factoryMessage.setHeader(
                        attachment.getTarget(socketChannel),
                        outMsgSeqNo,
                        heartbeat);

                attachment.putOutMsgSeqNo(socketChannel, outMsgSeqNo);
                ByteBuffer heartbeatBuffer = ByteBuffer.wrap(heartbeat.toString().getBytes());

                sessions.put(socketChannel, heartbeatBuffer);
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            }
        }
    }

    public static void main(String[] args) {
        final int PORT = 9090;
        try {
            StartAcceptor acceptor = new StartAcceptor(new InetSocketAddress(PORT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
