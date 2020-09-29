package no.kristinia.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    public static void main (String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);

        Socket clientSocket = serverSocket.accept();

        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 14\r\n" +
                "Content-Type: text/plain\r\n"+
                "\r\n"+
                "Hei Thorstein!";


        clientSocket.getOutputStream().write(response.getBytes());

    }
}
