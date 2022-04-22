import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ChatServer {
    ArrayList clientOutputStream;
    private static Map<String, InetAddress> registeredUsers;
    public static int SERVERPORT = 5000;
    private static final int FILE_USER_POS = 0;
    private static final int FILE_USER_IP = 1;
    private static final String OFFLINE_STRING = ";Offline_User_Logout";

    public ChatServer() {
        startServer();
    }

    private void startServer() {
        clientOutputStream = new ArrayList();
        registeredUsers = new HashMap<String, InetAddress>();

        Thread th = new Thread((Runnable) new FileServer());
        th.start();

        try {
            ServerSocket serverSocket = new ServerSocket(SERVERPORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
                clientOutputStream.add(writer);
                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();
                System.out.println("Connection built up.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Map<String, InetAddress> getRegisteredUsers() {
        return registeredUsers;
    }

    public static void addUserToList(String username, InetAddress ipAdress) {
        if (!registeredUsers.containsKey(username)) {
            registeredUsers.put(username, ipAdress);
        }
    }

    private void deleteUserFromList(String username, InetAddress ipAdress) {
        if (registeredUsers.get(username).equals(ipAdress)) {
            registeredUsers.remove(username);
        }
    }

    public void sendStringToAll(String nachricht) {
        Iterator iterator = clientOutputStream.iterator();
        while (iterator.hasNext()) {
            try {

                PrintWriter writer = (PrintWriter) iterator.next();
                // TODO encrypt nachricht
//                try {
//                    nachricht = ReadWriteDES.encode(nachricht.getBytes());
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
                writer.println(nachricht);
                writer.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getUsername(@NotNull String nachricht) {
        String[] splittedStrings = nachricht.split(";");
        return splittedStrings[FILE_USER_POS];
    }

    private String cutIPFromMessage(String nachricht) {
        String[] splittedStrings = nachricht.split(";");
        nachricht = nachricht.replace(splittedStrings[FILE_USER_IP],"");
        return nachricht.replace(";", "");
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

    public class ClientHandler implements Runnable {
        BufferedReader reader;
        Socket socket;

        public ClientHandler(Socket clientSocket) {
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
                    // TODO Decrypt Nachricht
//                    nachricht = ReadWriteDES.decode(nachricht);
                    System.out.println("Received: " + nachricht);
                    if (nachricht.contains(OFFLINE_STRING)){
                        deleteUserFromList(getUsername(nachricht), getIpAdress(nachricht));
                    } else {
                        addUserToList(getUsername(nachricht), getIpAdress(nachricht));
                        nachricht = cutIPFromMessage(nachricht);
                        sendStringToAll(nachricht);
                    }
                }
            } catch (SocketException socketException) {
                System.out.println("Connection lost...");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
