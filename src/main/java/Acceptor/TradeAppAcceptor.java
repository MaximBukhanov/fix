package Acceptor;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.*;
import quickfix.fix42.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TradeAppAcceptor {

    private final String ACCEPTOR_ID = "EXECUTOR";

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
                        NewOrderSingle order = (NewOrderSingle) messageFix;
                        ExecutionReport executionReport = new ExecutionReport(
                                new OrderID("123456"),
                                new ExecID("789"),
                                new ExecTransType(ExecTransType.NEW),
                                new ExecType(ExecType.NEW),
                                new OrdStatus(OrdStatus.NEW),
                                order.getSymbol(),
                                order.getSide(),
                                new LeavesQty(0),
                                new CumQty(0),
                                new AvgPx(0));

                        setHeader(target, outMsgSeqNo, executionReport);

                        response = ByteBuffer.wrap(executionReport.toString().getBytes());
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
