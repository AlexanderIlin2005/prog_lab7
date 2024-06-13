package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.random;
import static server.TCPServer.currentUserOnTimeout;

public class ModifiedClientSocket {
    Socket clientSocket;
    ObjectInputStream in;
    ObjectOutputStream out;


    public ModifiedClientSocket(Socket clientSocket) throws SocketTimeoutException {
        this.clientSocket = clientSocket;
        try {
            this.clientSocket.setSoTimeout(30000);
        } catch (SocketException e) {
            throw new SocketTimeoutException();
        }
        try {
            this.in = new ObjectInputStream(clientSocket.getInputStream());
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());





        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Object readObject() throws IOException, ClassNotFoundException {


        return in.readObject();
    }

    public void writeObject(Object obj) throws IOException {
        out.writeObject(obj);
        out.flush();
    }

    public Socket getSocket(){
        return clientSocket;
    }


}
