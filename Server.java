import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class Server {
    public static void main(String[] args) throws IOException, InterruptedException {
        String directory = args[0];

        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
            serverSocket.bind(new InetSocketAddress("localhost", 1337));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                if (selector.selectNow() == 0) continue; //verifies that client in .isAcceptable is never null.

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey currentKey = keyIterator.next();

                    if (currentKey.isAcceptable()) {
                        SocketChannel client = serverSocket.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    }

                    if (currentKey.isReadable()) {
                        SocketChannel client = (SocketChannel) currentKey.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(2048);

                        client.read(buffer); //read client's request -- whether to terminate or send file
                        String msg = new String(buffer.array()).trim();

                        if (msg.equals("terminate")) {
                            System.out.println("Terminating connection with client.");
                            client.close();
                        } else if (msg.startsWith("FILE")){
                            System.out.println("File requested: " + msg.substring(4));

                            if (Files.exists(Paths.get(directory+"/"+msg.substring(4)))) {
                                System.out.println("File found; sending file length.");
                                client.write(ByteBuffer.wrap("found".getBytes())); //tell client file found.

                                File file = new File(directory+"/"+msg.substring(4));
                                client.write(ByteBuffer.wrap(String.valueOf(file.length()).getBytes())); //send client amount of bytes in file.
                                System.out.println("File length: " + file.length() + " bytes.");

                                FileInputStream fileIS = new FileInputStream(file);
                                for (int i = 0; i < Math.max(file.length()/2048, 1); i++) {
                                    client.write(ByteBuffer.wrap((fileIS.readNBytes((int) Math.min(2048, file.length())))));
                                }
                            } else {
                                System.out.println("Could not find file: " + msg.substring(4));
                                client.write(ByteBuffer.wrap("notFound".getBytes())); //tell client could not find file.
                            }
                        }
                    }
                    keyIterator.remove();
                }
            }
        }
    }
}
