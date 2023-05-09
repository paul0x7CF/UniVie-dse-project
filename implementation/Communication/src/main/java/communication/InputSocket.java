package communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

public class InputSocket implements Runnable {
    private static final Logger logger = LogManager.getLogger(InputSocket.class);

    private final BlockingQueue<byte[]> input;
    private final int port;

    public InputSocket(BlockingQueue<byte[]> input, int port) {
        this.input = input;
        this.port = port;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (true) {
            byte[] buffer = new byte[1000];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(request);
                input.put(request.getData());
                logger.trace("Received message from {}:{}", request.getAddress(), request.getPort());
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}