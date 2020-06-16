import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;


public class Client {
    public static void main(String[] args) throws IOException {
        final String fileName = "test.txt";
        final String fileExtension = fileName.split("\\.")[1];

        try (SocketChannel channel = SocketChannel.open(new InetSocketAddress("localhost", 1337))) {
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            int fileSize;

            channel.write(ByteBuffer.wrap(("FILE" + fileName).getBytes()));
            buffer.clear();

            channel.read(buffer);
            if (new String(buffer.array()).trim().equals("notFound")) {
                buffer.clear();
                System.out.println("Server could not find requested file; terminating connection.");
                channel.write(ByteBuffer.wrap("terminate".getBytes()));
            } else if (new String(buffer.array()).trim().equals("found")) {
                buffer.clear();

                System.out.println("Server found file, ready to receive filesize.");
                channel.read(buffer); //gets file size from server
                fileSize = solveFileSize(new String(buffer.array()).trim());
                System.out.println("Received file size: " + fileSize);
                buffer.clear();


                StringBuilder fileData = new StringBuilder();
                for (int i = 0; i < Math.max(Math.ceil(fileSize/2048), 1); i++) {
                    channel.read(buffer);
                    fileData.append(new String(buffer.array()).trim());
                    buffer.clear();
                }

                System.out.println("Received file from server with contents: " + "\n" + fileData.toString());
                System.out.println("Terminating connection with server.");
                channel.write(ByteBuffer.wrap("terminate".getBytes()));

                if (!Files.isDirectory(Paths.get("received"))) Files.createDirectory(Paths.get("received"));
                File file = new File("received/" +LocalDateTime.now().getYear() +
                        LocalDateTime.now().getDayOfYear() +
                        LocalDateTime.now().getNano() + "." + fileExtension);
                if (!file.exists()) file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(fileData.toString().getBytes());
            }
        }
    }

    public static int solveFileSize(String fileSize) {      //filesize sometimes has random letters appended at the end of it
        try {                                               //that are not there on the server side; this is my solution to removing them.
            return Integer.parseInt(fileSize);
        } catch (NumberFormatException e) {
            return solveFileSize(fileSize.substring(0, fileSize.length()-1));
        }

    }
}
