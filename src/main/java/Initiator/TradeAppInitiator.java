package Initiator;

import quickfix.*;
import quickfix.fix42.MessageCracker;

public class TradeAppInitiator extends MessageCracker implements Application {
    @Override
    public void onCreate(SessionID sessionID) {
        System.out.println(">>New session create (onCreate), session ID = " + sessionID);
    }
    @Override
    public void onLogon(SessionID sessionID) {
        System.out.println(">>Logon message read (onLogon), session ID = " + sessionID);
    }
    @Override
    public void onLogout(SessionID sessionID) {
        System.out.println(">>Logout message read (onLogout), session ID = " + sessionID);
    }
    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        System.out.println("\n>>Message write (toAdmin): " + message.toString());
    }
    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        System.out.println(">>Message read (fromAdmin):" + message.toString());
        StartInitiator.systemMessages.add(message.toString());
    }
    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        System.out.println("\n>>Message write (toApp): " + message.toString());
    }
    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        System.out.println(">>Message read (fromApp): " +  message.toString());
        StartInitiator.messages.add(message.toString());
    }
}