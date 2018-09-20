import java.net.*;
import java.io.*;
import java.util.*;



public class Client  {

    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private String server, username;
    private int port;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }



    Client(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }


    public boolean start() {

        try {
            socket = new Socket(server, port);
        }
        catch(Exception ec) {
            ec.printStackTrace();
            return false;
        }


        try
        {
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
           eIO.printStackTrace();
            return false;
        }

        new ListenFromServer().start();

        try
        {
            sOutput.writeObject(username);
        }
        catch (IOException eIO) {
            eIO.printStackTrace();
            return false;
        }
        return true;
    }


    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
        }
    }
    private void disconnect() {
        try {
            if(sInput != null) sInput.close();
        }
        catch(Exception e) {}
        try {
            if(sOutput != null) sOutput.close();
        }
        catch(Exception e) {}
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {}

    }

    public static void main(String[] args) {
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Client";
        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the username: ");
        userName = scan.nextLine();


        Client client = new Client(serverAddress, portNumber, userName);

        if(!client.start())
            return;

        while(true) {
            System.out.print("> ");
            String msg = scan.nextLine();
            if(msg.equalsIgnoreCase("quit")) {
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
                break;
            }else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        scan.close();
        client.disconnect();
    }

    class ListenFromServer extends Thread {

        public void run() {
            while(true) {
                try {
                    String msg = (String) sInput.readObject();
                    System.out.println(msg);
                    System.out.print("> ");
                }
                catch(IOException e) {
                    e.printStackTrace();
                    break;
                }
                catch(ClassNotFoundException e2) {
                }
            }
        }
    }
}