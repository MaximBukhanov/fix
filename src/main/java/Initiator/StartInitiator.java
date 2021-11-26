package Initiator;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

import java.util.ArrayList;

public class StartInitiator {

    //Messages fromApp
    public static ArrayList<String> messages;
    //Message fromAdmin
    public static ArrayList<String> systemMessages;

    public static void main(String[] args) {

        NewOrderSingle newOrderSingle = new NewOrderSingle(
                new ClOrdID("456"),
                new HandlInst('3'),
                new Symbol("AJCB"),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.MARKET)
        );

        StartInitiator initiator = new StartInitiator();
        initiator.start(newOrderSingle);
    }

    public void start(quickfix.fix42.Message message) {
        SocketInitiator socketInitiator = null;
        try {
            SessionSettings initiatorSettings = new SessionSettings("./initiatorSettings.txt");
            Application initiatorApplication = new TradeAppInitiator();

            FileStoreFactory fileStoreFactory = new FileStoreFactory(initiatorSettings);
            FileLogFactory fileLogFactory = new FileLogFactory(initiatorSettings);
            MessageFactory messageFactory = new DefaultMessageFactory();

            messages = new ArrayList<>();
            systemMessages = new ArrayList<>();

            socketInitiator = new SocketInitiator(initiatorApplication, fileStoreFactory, initiatorSettings, fileLogFactory, messageFactory);
            socketInitiator.start();

            while (!socketInitiator.isLoggedOn()) {
                continue;
            }

            SessionID sessionId = socketInitiator.getSessions().get(0);
            Session.sendToTarget(message, sessionId);

            Session.lookupSession(sessionId).logout();
            socketInitiator.stop();

        } catch (ConfigError | SessionNotFound configError) {
            configError.printStackTrace();
        }
    }
}