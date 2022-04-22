import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class FileServer implements Runnable {

    private ServerSocket ss;
    public static int SERVERDATAPORT = 5001;
    public static int SERVERSENDDATAPORT = 5002;
    public static int SERVERSENDDATARECEIVEPORT = 5003;
    private static final int FILE_USER_POS = 0;
    private static final int FILE_USER_IP = 1;
    private static final int FILE_NAME_POS = 3;
    private static final int FILE_SIZE_POS = 4;
    private String pathDocuments;
    private static final String IS_FILE_STRING = ";Data_Sending;";
    private static final String SEND_BACK_STRING = ";Data_Sending_Back;";

    public FileServer() {
        pathDocuments = new JFileChooser().getFileSystemView().getDefaultDirectory().toString();
        pathDocuments += "\\ChatClient\\tmp\\";
    }

    public void run() {
        try {
            ss = new ServerSocket(SERVERDATAPORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ServerSocket serverDataSocket = new ServerSocket(SERVERSENDDATAPORT);

            while (true) {
                Socket clientSocket = serverDataSocket.accept();
                Thread t = new Thread(new FileServer.ClientFileHandler(clientSocket));
                t.start();
                System.out.println("Connection to File-Server built up.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private long getFileSize(@NotNull String nachricht) {
        String[] splittedStrings = nachricht.split(";");
        return Long.parseLong(splittedStrings[FILE_SIZE_POS]);
    }

    private String getFileName(@NotNull String nachricht) {
        String[] splittedStrings = nachricht.split(";");
        return splittedStrings[FILE_NAME_POS];
    }

    private String getUsername(@NotNull String nachricht) {
        String[] splittedStrings = nachricht.split(";");
        return splittedStrings[FILE_USER_POS];
    }

    private InetAddress getIpAdress(@NotNull String nachricht) {
        String[] splittedStrings = nachricht.split(";");
        try {
            return InetAddress.getByName(splittedStrings[FILE_USER_IP]);
        } catch (UnknownHostException uhEx) {
            uhEx.printStackTrace();
        }
        return InetAddress.getLoopbackAddress();
    }

    public void processFile(String message) {
        try {
            Socket clientSocket = ss.accept();
            saveFile(clientSocket, getFileSize(message), getFileName(message));
            sendFileToAll(getUsername(message), getFileName(message), getFileSize(message), clientSocket);
            Thread deleteThread = new Thread(new DeleteRunnable(getFileName(message)));
            deleteThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFile(Socket socket, long fileSize, String fileName) {
        try {
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            FileOutputStream fos = new FileOutputStream(pathDocuments + fileName);
            byte[] buffer = new byte[8192];

            int read = 0;
            int totalRead = 0;
            int remaining = (int) fileSize;
            while ((read = dataInputStream.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                totalRead += read;
                remaining -= read;
                System.out.print("read " + totalRead + " bytes.");
                System.out.print('\n');
                fos.write(buffer, 0, read);
            }

            fos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendFileToAll(String fileUser, String fileName, long fileSize, Socket socket) {
        Map <String, InetAddress> registeredUsers = ChatServer.getRegisteredUsers();
        // senden an alle IPs die connected sind
        for (Map.Entry<String, InetAddress> userWithIP : registeredUsers.entrySet()) {
            System.out.println("Try to send File to: " + userWithIP.getKey() + "," + userWithIP.getValue().toString());
            try {
                Socket toClientSocket = new Socket(userWithIP.getValue(), SERVERSENDDATARECEIVEPORT);
                OutputStream outstream = toClientSocket.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outstream));
                String messageFile = fileUser + SEND_BACK_STRING + fileName + ";" + fileSize + "\n";
                // TODO encrypt nachricht
//                try {
//                    messageFile = ReadWriteDES.encode(messageFile.getBytes(StandardCharsets.ISO_8859_1));
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
                writer.write(messageFile);
                writer.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException intEx) {
                intEx.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException intEx) {
                intEx.printStackTrace();
            }
            try {
                int count;
                String filePathToSend = pathDocuments + fileName;
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                File sendFile = new File(filePathToSend);
                FileInputStream fis = new FileInputStream(sendFile);
                byte[] buffer = new byte[8192];

                while ((count = fis.read(buffer)) > 0) {
                    dataOutputStream.write(buffer, 0, count);
                }

                fis.close();
                dataOutputStream.flush();
                dataOutputStream.close();
                System.out.println('\n' + fileName + " sent.");
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

    public class DeleteRunnable implements Runnable {
        private String fileName;

        public DeleteRunnable(String file) {
            fileName = file;
        }

        @Override
        public void run() {
            deleteFile(fileName);
        }

        private void deleteFile(String fileName) {
            boolean deletable = false;
            File fileToDelete = new File(pathDocuments + fileName);
            do {
                deletable = fileToDelete.canRead();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException intEx) {
                    intEx.printStackTrace();
                }
            } while (!deletable);

            if (fileToDelete.delete()) { // TODO File is locked by OpenJDK Binary Platform
                System.out.println("Deleted the file: " + fileToDelete.getName());
            } else {
                System.out.println("Failed to delete the file.");
            }
        }
    }

    public class ClientFileHandler implements Runnable {
        BufferedReader reader;
        Socket socket;

        public ClientFileHandler(Socket clientSocket) {
            try {
                socket = clientSocket;
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                reader = new BufferedReader(inputStreamReader);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void run() {
            String nachricht;
            try {
                while ((nachricht = reader.readLine()) != null) {
                    if (nachricht.contains(IS_FILE_STRING)) {
                        // TODO decrypt nachricht
//                        nachricht = ReadWriteDES.decode(nachricht);
                        System.out.println("Received: " + nachricht);
                        ChatServer.addUserToList(getUsername(nachricht), getIpAdress(nachricht));
                        processFile(nachricht);
                    }
                }
            } catch (SocketException socketException) {
                System.out.println("Connection to File-Server lost...");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}