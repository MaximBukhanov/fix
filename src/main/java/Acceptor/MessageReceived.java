package Acceptor;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MessageReceived implements Runnable {
    private String QUEUE_WORKER_TO_SERVER = "worker";

    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection;
        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    String response = new String(body, "UTF-8");
                    System.out.println(" [x] Received '" + response + "'");
                    synchronized (Attachment.queueMessage) {
                        Attachment.queueMessage.add(response);
                    }
                }
            };
            channel.queueDeclare(QUEUE_WORKER_TO_SERVER, false, false, false, null);
            channel.basicConsume(QUEUE_WORKER_TO_SERVER, true, consumer);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
