package Acceptor;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TradeAppAcceptorTest {

    private void createMessageTest(String actualFixMsg, String expectedFixMsg, int inSeqNo, int outSeqNo) throws IOException {
        TradeAppAcceptor tradeAppAcceptor = new TradeAppAcceptor();

        SocketChannel socketChannel = null;
        Attachment attachment = new Attachment();
        attachment.putInMsgSeqNo(socketChannel, inSeqNo);
        attachment.putOutMsgSeqNo(socketChannel, outSeqNo);

        byte[] actual = null;
        byte[] expected = null;
        ByteBuffer actualByteBuffer = null;

        if (expectedFixMsg != null)
            expected = expectedFixMsg.getBytes(StandardCharsets.UTF_8);

        actualByteBuffer = tradeAppAcceptor.createExecutionReport(socketChannel, actualFixMsg, attachment);

        if (actualByteBuffer != null) {

            String actualWithoutTimeAndTrailer = new String(actualByteBuffer.array());

            String timeSending = actualWithoutTimeAndTrailer.substring(
                    actualWithoutTimeAndTrailer.indexOf("52="),
                    actualWithoutTimeAndTrailer.indexOf("\u0001", actualWithoutTimeAndTrailer.indexOf("52=")) + 1);

            actualWithoutTimeAndTrailer = actualWithoutTimeAndTrailer.replace(timeSending, "");
            actualWithoutTimeAndTrailer = actualWithoutTimeAndTrailer.substring(0, actualWithoutTimeAndTrailer.indexOf("10="));

            actual = actualWithoutTimeAndTrailer.getBytes(StandardCharsets.UTF_8);
        }
        assertArrayEquals(actual, expected);
    }

    @Test
    public void whenLogonMsgCorrect () throws IOException {
        String actualFixMsg = "8=FIX.4.2\u00019=68\u000135=A\u000149=CLIENT1\u000156=EXECUTOR\u000134=1" +
                "\u000152=20211124-14:33:40\u000198=0\u0001108=5000\u000110=241\u0001";
        String expectedFixMsg = "8=FIX.4.2\u00019=70\u000135=A\u000134=1\u000149=EXECUTOR\u000156=CLIENT1\u000198=0\u0001108=30\u0001";

        createMessageTest(actualFixMsg, expectedFixMsg, 0, 0);
    }

    @Test
    public void whenLogonMsgSeqNoIncorrect () throws IOException {
        String actualFixMsg = "8=FIX.4.2\u00019=68\u000135=A\u000149=CLIENT1\u000156=EXECUTOR\u000134=4\u0001" +
                "52=20211125-08:10:10\u000198=0\u0001108=5000\u000110=240\u0001";
        String expectedFixMsg = null;

        createMessageTest(actualFixMsg, expectedFixMsg, 0, 0);
    }

    @Test
    public void whenLogoutMsgCorrect () throws IOException {
        String actualFixMsg = "8=FIX.4.2\u00019=54\u000135=5\u000149=CLIENT1\u000156=EXECUTOR\u000134=2\u000152=20211124-15:29:53\u000110=112\u0001";
        String expectedFixMsg = "8=FIX.4.2\u00019=58\u000135=5\u000134=2\u000149=EXECUTOR\u000156=CLIENT1\u0001";

        createMessageTest(actualFixMsg, expectedFixMsg, 1, 1);
    }

    @Test
    public void whenNewOrderSingleMsgCorrect () throws IOException {
        String actualFixMsg = "8=FIX.4.2\u00019=129\u000135=D\u000149=CLIENT1\u000156=EXECUTOR\u000134=2\u0001" +
                "52=20211125-08:10:02\u000111=150275560\u000121=1\u000155=ERICB.ST\u000154=1\u0001" +
                "60=20211125-08:10:02\u000140=2\u000144=50\u000138=1000\u000110=231\u0001";
        String expectedFixMsg = "8=FIX.4.2\u00019=123\u000135=8\u000134=2\u000149=EXECUTOR\u000156=CLIENT1\u0001" +
                "6=0\u000114=0\u000117=789\u000120=0\u000137=123456\u000139=0\u000154=1\u000155=ERICB.ST\u0001150=0\u0001151=0\u0001";

        createMessageTest(actualFixMsg, expectedFixMsg, 1, 1);
    }

    @Test
    public void whenSenderCompIdIncorrect () throws IOException {
        String actualFixMsg = "8=FIX.4.2\u00019=130\u000135=D\u000149=CLIENT1\u000156=EXECUTOR1\u000134=6\u0001" +
                "52=20211125-09:06:36\u000111=150275561\u000121=1\u000155=ERICB.ST\u000154=1\u0001" +
                "60=20211125-09:06:36\u000140=2\u000144=50\u000138=1000\u000110=047\u0001";
        String expectedFixMsg = null;

        createMessageTest(actualFixMsg, expectedFixMsg, 0, 0);
    }

    @Test
    public void whenCheckSumIncorrect () throws IOException {
        String actualFixMsg = "8=FIX.4.2\u00019=68\u000135=7\u000149=CLIENT1\u000156=EXECUTOR\u000134=1" +
                "\u000152=20211124-14:33:40\u000198=0\u0001108=5000\u000110=241\u0001";
        String expectedFixMsg = null;

        createMessageTest(actualFixMsg, expectedFixMsg, 0, 0);
    }

    @Test
    public void whenLengthMsgIncorrect () throws IOException {
        String actualFixMsg = "8=FIX.4.2 9=68\u000135=A\u000149=CLIENT1\u000156=EXECUTOR\u000134=1" +
                "\u000152=20211124-14:33:40\u000198=0\u0001108=5000 10=241\u0001";
        String expectedFixMsg = null;

        createMessageTest(actualFixMsg, expectedFixMsg, 0, 0);
    }

    @Test
    public void whenFormatMsgIncorrect() throws IOException {
        String actualFixMsg = "8=FIX.4.2\u00019=128\u000135=D\u000149=CLIENT1\u000156=EXECUTOR\u000134=2\u0001" +
                "52=20211125-08:10:02\u000111=150275560\u000121=1\u000155=ERICB.ST\u000154=1\u0001" +
                "60=20211125-08:10:02\u000140=2\u000144=50\u000138=1000\u000110=231";
        String expectedFixMsg = null;

        createMessageTest(actualFixMsg, expectedFixMsg, 1, 1);
    }

    @Test
    public void whenNameProtocolIncorrect() throws IOException {
        String actualFixMsg = "8=FIX.4.7\u00019=129\u000135=D\u000149=CLIENT1\u000156=EXECUTOR\u000134=2\u0001" +
                "52=20211125-08:10:02\u000111=150275560\u000121=1\u000155=ERICB.ST\u000154=1\u0001" +
                "60=20211125-08:10:02\u000140=2\u000144=50\u000138=1000\u000110=231\u0001";
        String expectedFixMsg = null;

        createMessageTest(actualFixMsg, expectedFixMsg, 1, 1);
    }
}

