import gov.ismonnet.arduino.ArduinoClient;
import gov.ismonnet.arduino.ReceiveThread;
import gov.ismonnet.arduino.netty.ArduinoSerialManager;
import gov.ismonnet.arduino.netty.apacket.APacket;
import gov.ismonnet.arduino.netty.apacket.impl.PrintPacket;
import gov.ismonnet.arduino.serial.CliSerialPortSelector;
import gov.ismonnet.arduino.serial.PacketsToCommandsConverter;
import gov.ismonnet.arduino.serial.PacketsToCommandsConverterImpl;
import gov.ismonnet.arduino.serial.SerialPortSelector;
import gov.ismonnet.shared.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ArduinoMain {

    private static final Logger LOGGER = LogManager.getLogger(ArduinoMain.class);
    private static final int SHUTDOWN_TIMEOUT = 5000;

    public static void main(String[] args) throws SocketException {

        final Scanner scanner = new Scanner(System.in);
        final SerialPortSelector portSelector = new CliSerialPortSelector(System.out, scanner);

        final ArduinoSerialManager serial = new ArduinoSerialManager(portSelector.choosePort());
        final PacketsToCommandsConverter converter = new PacketsToCommandsConverterImpl();
        serial.start();

        final ArduinoClient client = new ArduinoClient(Constants.SERVER_IP_ADDRESS, Constants.SEVER_PORT);
        client.connect();

        final Thread clientToSerial = new ReceiveThread(
                client::read,
                toSend -> serial.sendPacket(converter.convert(toSend)));

        final BlockingDeque<APacket> received = new LinkedBlockingDeque<>();
        final Thread serialToClient = new ReceiveThread(
                () -> {
                    try {
                        return converter.convert(received.take());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                client::send);
        serial.register(received::push);
        serial.register(packet -> {
            if(!(packet instanceof PrintPacket))
                return;
            System.out.println("[Arduino] " + ((PrintPacket) packet).getToPrint());
        });

        clientToSerial.start();
        serialToClient.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serial.stop();
            client.close();
        }));

        try {
            clientToSerial.join(SHUTDOWN_TIMEOUT);
        } catch (InterruptedException e) {
            LOGGER.fatal("Exception while waiting for socket shutdown", e);
        }

        try {
            serialToClient.join(SHUTDOWN_TIMEOUT);
        } catch (InterruptedException e) {
            LOGGER.fatal("Exception while waiting for serial shutdown", e);
        }
    }
}
