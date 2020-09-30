package no.kristiania.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HttpServer {

    private File contentRoot;
    private List<String> productNames = new ArrayList<>();

    public HttpServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        new Thread(() -> {
            while (true) {
                try {
                    handleRequest(serverSocket.accept());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        String requestLine = HttpClient.readLine(clientSocket);
        System.out.println(requestLine);

        String requestTarget = requestLine.split(" ")[1];
        String statusCode = "200";
        String body = "Hello <strong>World</strong>";

        int questionPos = requestTarget.indexOf('?');

        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (questionPos != -1) {
            QueryString queryString = new QueryString(requestTarget.substring(questionPos+1));
            if (queryString.getParameter("status") != null){
                statusCode = queryString.getParameter("status");
            }
            if (queryString.getParameter("body") != null){

                body = queryString.getParameter("body");

            }
        } else if (!requestPath.equals("/echo")){
            File file = new File(contentRoot, requestPath);
            if(!file.exists()){
                body = file + "Does not exist";
                String response = "HTTP/1.1 404 Not Found \r\n" +
                        "Content-Length: " + body.length() + "\r\n" +
                        "\r\n" +
                        body;

                clientSocket.getOutputStream().write(response.getBytes());
                return;
            }
            statusCode = "200";
            String contentType = "text/plain";
            if (file.getName().endsWith(".html")) {
                contentType = "text/html";
            }
            String response = "HTTP/1.1 " + statusCode + " OK\r" +
                    "\n" +
                    "Content-Length: " + file.length() + "\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "\r\n";

            clientSocket.getOutputStream().write(response.getBytes());

            new FileInputStream(file).transferTo(clientSocket.getOutputStream());
        }

        String response = "HTTP/1.1 " + statusCode + " OK\r" +
                "\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/plain\r\n"+
                "\r\n"+
                body;


        clientSocket.getOutputStream().write(response.getBytes());
    }

    public static void main (String[] args) throws IOException {
        HttpServer server = new HttpServer(8080);
        server.setContentRoot(new File("pgr203innlevering2-thensrud/src/main/resources"));
    }

    public void setContentRoot(File contentRoot) {
        this.contentRoot = contentRoot;
    }

    public List<String> getProductNames() {
        return productNames;
    }
}
