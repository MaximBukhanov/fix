package Acceptor;

import Initiator.StartInitiator;
import org.junit.BeforeClass;
import org.junit.Test;
import quickfix.field.*;
import quickfix.fix42.Message;
import quickfix.fix42.NewOrderSingle;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StartAcceptorTest {

    @BeforeClass
    public static void startServer() {
        final int PORT = 9090;
        try {
            StartAcceptor acceptor = new StartAcceptor(new InetSocketAddress(PORT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createRequest(Message actual, String expected) {
        StartInitiator initiator = new StartInitiator();
        initiator.start(actual);

        String actualStr = parseString(StartInitiator.messages.get(0));

        assertEquals(actualStr, expected);
    }

    private ArrayList<String> createSystemMessage(Message actual, String expected) {
        StartInitiator initiator = new StartInitiator();
        initiator.start(actual);
        return StartInitiator.systemMessages;
    }


    private String parseString(String actual) {
        String timeSending = actual.substring(
                actual.indexOf("52="),
                actual.indexOf("\u0001", actual.indexOf("52=")) + 1);

        String actualWithoutTimeAndTrailer = actual.replace(timeSending, "");
        actualWithoutTimeAndTrailer = actualWithoutTimeAndTrailer.substring(0, actualWithoutTimeAndTrailer.indexOf("10="));

        return actualWithoutTimeAndTrailer;
    }

    @Test
    public void whenMsgCorrect() {
        NewOrderSingle newOrderSingle = new NewOrderSingle(
                new ClOrdID("456"),
                new HandlInst('3'),
                new Symbol("AJCB"),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.MARKET)
        );

        String expected = "8=FIX.4.2\u00019=118\u000135=8\u000134=2\u000149=EXECUTOR\u0001" +
                "56=CLIENT\u00016=0\u000114=0\u000117=789\u000120=0\u000137=123456\u000139=0\u0001" +
                "54=1\u000155=AJCB\u0001150=0\u0001151=0\u0001";

        createRequest(newOrderSingle, expected);

    }

    @Test
    public void whenLogonCorrect() {
        NewOrderSingle newOrderSingle = new NewOrderSingle(
                new ClOrdID("456"),
                new HandlInst('3'),
                new Symbol("AJCB"),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.MARKET)
        );

        String expected = "8=FIX.4.2\u00019=69\u000135=A\u000134=1\u000149=EXECUTOR" +
                "\u000156=CLIENT\u000198=0\u0001108=30\u0001";

        ArrayList<String> systemMessages = createSystemMessage(newOrderSingle, expected);
        String actualStr = parseString(systemMessages.get(0));

        assertEquals(actualStr, expected);
    }

    @Test
    public void whenLogoutCorrect() {
        NewOrderSingle newOrderSingle = new NewOrderSingle(
                new ClOrdID("456"),
                new HandlInst('3'),
                new Symbol("AJCB"),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.MARKET)
        );

        String expected = "8=FIX.4.2\u00019=57\u000135=5\u000134=3\u000149=EXECUTOR\u000156=CLIENT\u0001";

        ArrayList<String> systemMessages = createSystemMessage(newOrderSingle, expected);
        String actualStr = parseString(systemMessages.get(1));

        assertEquals(actualStr, expected);

    }

}