import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {

    private static int uniqueId;
    private ArrayList<ClientThread> al;
    private SimpleDateFormat sdf;
    private int port;
    private boolean keepGoing;
    private String notif = " *** ";


    public Server(int port) {
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        al = new ArrayList<ClientThread>();
    }

    public void start() {
        keepGoing = true;
        try {

            ServerSocket serverSocket = new ServerSocket(port);

            while (keepGoing) {
                display("Server waiting for Clients.");
                Socket socket = serverSocket.accept();

                if (!keepGoing)
                    break;

                ClientThread t = new ClientThread(socket);
                al.add(t);
                t.start();
            }
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                    }
                }
            } catch (Exception e) {
               e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
        }
    }

    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    private synchronized boolean broadcast(String message) {
        String time = sdf.format(new Date());

        String[] w = message.split(" ", 3);

        boolean isPrivate = false;
        if (w[1].charAt(0) == '@')
            isPrivate = true;



        if (isPrivate == true) {
            String tocheck = w[1].substring(1, w[1].length());

            message = w[0] + w[2];
            String messageLf = time + " " + message + "\n";
            boolean found = false;
            for (int y = al.size(); --y >= 0; ) {
                ClientThread ct1 = al.get(y);
                String check = ct1.getUsername();
                if (check.equals(tocheck)) {
                    if (!ct1.writeMsg(messageLf)) {
                        al.remove(y);
                    }
                    found = true;
                    break;
                }
                if(found!=true)
                {
                    return false;
                }

            }
        }

        else {
            String messageLf = time + " " + message + "\n";
            System.out.print(messageLf);
            for (int i = al.size(); --i >= 0; ) {
                ClientThread ct = al.get(i);
                if(!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            }
        }
        return true;


    }


    synchronized void remove(int id) {

        String disconnectedClient = "";
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            // if found remove it
            if (ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " has left the chat room." + notif);
    }


    public static void main(String[] args) {

        int portNumber = 1500;

        Server server = new Server(portNumber);
        server.start();
    }


    class ClientThread extends Thread {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;

        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            System.out.println("Thread trying to create Object Input/Output Streams");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                broadcast(notif + username + " has joined the chat room." + notif);
            } catch (IOException e) {
                return;
            } catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }


        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                // read a String (which is an object)
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                // get the message from the ChatMessage object received
                String message = cm.getMessage();

                // different actions based on type message
                switch (cm.getType()) {

                    case ChatMessage.MESSAGE:
                        boolean confirmation = broadcast(username + ": " + message);
                        if (confirmation == false) {
                            String msg = notif + "Sorry. No such user exists." + notif;
                            writeMsg(msg);
                        }
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a quit message.");
                        keepGoing = false;
                        break;

                }
            }
            remove(id);
            close();
        }


        private void close() {
            try {
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {
            }
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {
            }
            ;
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
            }
        }


        private boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            }
            catch (IOException e) {
                display(notif + "Error sending message to " + username + notif);
                display(e.toString());
            }
            return true;
        }
    }
}