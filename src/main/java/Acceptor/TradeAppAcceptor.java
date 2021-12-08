package Acceptor;

import com.rabbitmq.client.*;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.*;
import quickfix.fix42.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class TradeAppAcceptor {

    private final String ACCEPTOR_ID = "EXECUTOR";
    private String QUEUE_SERVER_TO_WORKER = "server";
    private String QUEUE_WORKER_TO_SERVER = "worker";

    /**
     * Создание ответного сообщения
     *
     * @param socketChannel
     * @param message
     * @return
     * @throws IOException
     */
    public ByteBuffer createExecutionReport(SocketChannel socketChannel, String message, Attachment attachment) throws IOException {
        StringField field = null;
        ByteBuffer response = null;
        AtomicReference<byte []> responseAtomic = null;
        quickfix.Message messageFix = null;

        Integer inMsgSeqNo = attachment.getInMsgSeqNo(socketChannel);
        Integer outMsgSeqNo = attachment.getOutMsgSeqNo(socketChannel);

        /** Проверка длины сообщения <9> и протокола передачи <8> **/
        if(!checkLengthMsg(message) || !checkNameProtocol(message)) {
            inMsgSeqNo++;
            attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
            return null;
        }

        try {
            messageFix = MessageUtils.parse(new DefaultMessageFactory(), new DataDictionary("./FIX42.xml"), message);

            /** Проверка SenderCompID **/
            if (!messageFix.getHeader().getField(new TargetCompID()).getValue().equals(ACCEPTOR_ID))
                return null;

            field = messageFix.getHeader().getField(new MsgType());
            String target = messageFix.getHeader().getField(new SenderCompID()).getValue();
            attachment.putTarget(socketChannel, target);
            int msgSeqNo = messageFix.getHeader().getField(new MsgSeqNum()).getValue();

            switch (field.getValue()) {
                case "A": {
                    /** Ответ на LogonMessage **/
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    if (msgSeqNo == 1) {
                        attachment.putHeartBtInt(socketChannel, messageFix.getField(new HeartBtInt()).getValue());
                        Logon logon = new Logon(
                                new EncryptMethod(0),
                                new HeartBtInt(30));

                        setHeader(target, outMsgSeqNo, logon);

                        response = ByteBuffer.wrap(logon.toString().getBytes());
                    }

                    attachment.putOutMsgSeqNo(socketChannel, outMsgSeqNo);
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "D": {
                    /** Ответ на NewOrderSingleMessage **/
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    if (msgSeqNo > 1 && msgSeqNo == inMsgSeqNo) {

                        /** Создание подключения к серверу RabbitMq **/
                        ConnectionFactory factory = new ConnectionFactory();
                        factory.setHost("localhost");
                        try (Connection connection = factory.newConnection();
                             Channel channel = connection.createChannel();
                             Channel channelAnswer = connection.createChannel()) {

                            channel.queueDeclare(QUEUE_SERVER_TO_WORKER, false, false, false, null);
                            channel.basicPublish("", QUEUE_SERVER_TO_WORKER, null, message.getBytes(StandardCharsets.UTF_8));

                            channelAnswer.queueDeclare(QUEUE_WORKER_TO_SERVER, false, false, false, null);

                            String responseStr = new String (channel.basicGet(QUEUE_WORKER_TO_SERVER, true).getBody(), "UTF-8");
                            Message fixAnswer = null;

                            try {
                                fixAnswer = (Message) MessageUtils.parse(new DefaultMessageFactory(), new DataDictionary("./FIX42.xml"), responseStr);
                            } catch (InvalidMessage | ConfigError invalidMessage) {
                                invalidMessage.printStackTrace();
                            }

                            setHeader(target, attachment.getOutMsgSeqNo(socketChannel), fixAnswer);
                            responseStr = fixAnswer.toString();

                            System.out.println(">>RabbitMQ write message: " + responseStr);
                            response = ByteBuffer.wrap(responseStr.getBytes());

                        } catch (TimeoutException | IOException e) {
                            e.printStackTrace();
                        }
                    } else if (msgSeqNo > 1 && msgSeqNo != inMsgSeqNo) {
                        //ResendRequest send
                        response = createResendRequest(msgSeqNo, inMsgSeqNo, outMsgSeqNo, target);
                        break;
                    }

                    attachment.putOutMsgSeqNo(socketChannel, outMsgSeqNo);
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "5": {
                    /** Ответ на LogoutMessage **/
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    if (msgSeqNo > 1 && msgSeqNo == inMsgSeqNo) {
                        Logout logout = new Logout();
                        setHeader(target, outMsgSeqNo, logout);
                        response = ByteBuffer.wrap(logout.toString().getBytes());
                    } else if (msgSeqNo > 1 && msgSeqNo != inMsgSeqNo) {
                        response = createResendRequest(msgSeqNo, inMsgSeqNo, outMsgSeqNo, target);
                        break;
                    }

                    attachment.putOutMsgSeqNo(socketChannel, outMsgSeqNo);
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "0": {
                    /** Ответ на HeartbeatMessage **/
                    inMsgSeqNo++;
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "1": {
                    /** Ответ на TestRequestMessage **/
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    Heartbeat heartbeat = new Heartbeat();
                    setHeader(target, outMsgSeqNo, heartbeat);

                    response = ByteBuffer.wrap(heartbeat.toString().getBytes());
                }
                default: {
                    response = null;
                    break;
                }
            }
        } catch (InvalidMessage | ConfigError | FieldNotFound invalidMessage) {
            /** invalidMessage.printStackTrace(); **/
        }
        return response;
    }

    /**
     * Проверка длины сообщения <9>
     * @param message
     * @return
     */
    private boolean checkLengthMsg (String message) {
        String [] groups = message.split("\u0001");
        String body = message.substring(message.indexOf("35="), message.length() - 7);
        if (!groups [1].equals("9=" + body.length()))
            return false;

        return true;
    }

    /**
     * Проверка версии протокола <8>
     * @param message
     * @return
     */
    private boolean checkNameProtocol(String message) {
        String [] groups = message.split("\u0001");
        if (!groups [0].equals("8=FIX.4.2"))
            return false;

        return true;
    }

    /**
     * Создание ResendRequestMessage
     * @param msgSeqNo
     * @param inMsgSeqNo
     * @param outMsgSeqNo
     * @param target
     * @return
     */
    private ByteBuffer createResendRequest(int msgSeqNo, int inMsgSeqNo, int outMsgSeqNo, String target) {
        ResendRequest resendRequest = new ResendRequest();
        if (msgSeqNo == 0) {
            resendRequest.set(new BeginSeqNo(0));
        } else {
            resendRequest.set(new BeginSeqNo(inMsgSeqNo));
        }
        resendRequest.set(new EndSeqNo(msgSeqNo));

        setHeader(target, outMsgSeqNo, resendRequest);

        return ByteBuffer.wrap(resendRequest.toString().getBytes());
    }

    /**
     * Создание Header для FIX
     * @param targetCompID
     * @param outMsgSeqNo
     * @param message
     */
    public void setHeader (String targetCompID, int outMsgSeqNo, Message message) {
        quickfix.fix42.Message.Header header = (quickfix.fix42.Message.Header) message.getHeader();
        header.set(new SenderCompID("EXECUTOR"));
        header.set(new TargetCompID(targetCompID));
        header.set(new SendingTime());
        header.set(new MsgSeqNum(outMsgSeqNo));
    }
}
