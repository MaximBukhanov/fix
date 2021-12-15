package Acceptor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.Message;
import quickfix.fix42.NewOrderSingle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class MessageCreator implements Runnable {

    private String QUEUE_SERVER_TO_WORKER = "server";
    private String QUEUE_WORKER_TO_SERVER = "worker";

    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection;
        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            Channel channelProducer = connection.createChannel();

            channel.queueDeclare(QUEUE_SERVER_TO_WORKER, false, false, false, null);
            channelProducer.queueDeclare(QUEUE_WORKER_TO_SERVER, false, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(">>RabbitMQ received message: " + message);

                Message messageFix = null;
                String response = null;

                try {
                    messageFix = (Message) MessageUtils.parse(new DefaultMessageFactory(), new DataDictionary("./FIX42.xml"), message);

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

                    response = executionReport.toString();

                } catch (InvalidMessage | ConfigError | FieldNotFound e) {
                    e.printStackTrace();
                }
                channelProducer.basicPublish("", QUEUE_WORKER_TO_SERVER, null, response.getBytes(StandardCharsets.UTF_8));
            };
            channel.basicConsume(QUEUE_SERVER_TO_WORKER, true, deliverCallback, consumerTag -> {});

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
