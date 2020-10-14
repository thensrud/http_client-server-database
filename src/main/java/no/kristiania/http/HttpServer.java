package no.kristiania.http;

import no.kristiania.database.ProductDao;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HttpServer {

    private File contentRoot;
    private ProductDao productDao;

    public HttpServer(int port, DataSource dataSource) throws IOException {
        productDao = new ProductDao(dataSource);

        ServerSocket serverSocket = new ServerSocket(port);

        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleRequest(clientSocket);
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void handleRequest(Socket clientSocket) throws IOException, SQLException {
        HttpMessage request = new HttpMessage(clientSocket);
        String requestLine = request.getStartLine();
        System.out.println(requestLine);

        String requestMethod = requestLine.split(" ")[0];
        String requestTarget = requestLine.split(" ")[1];

        int questionPos = requestTarget.indexOf('?');

        String requestPath = questionPos != -1 ? requestTarget.substring(0, questionPos) : requestTarget;

        if (requestMethod.equals("POST")) {
            QueryString requestParameter = new QueryString(request.getBody());

            String fullName = requestParameter.getParameter("full_name");
            String emailAddressURLEncoded = requestParameter.getParameter("email_address");
            String emailAddress = URLDecoder.decode(emailAddressURLEncoded, StandardCharsets.UTF_8);

            productDao.insert("Name: " + fullName + ". E-mail: " + emailAddress);
            String body = "Okay...";
            String response = "HTTP/1.1 302 REDIRECT\r\n" +
                    "Location: http://localhost:8085/showWorkers.html\r\n" +
                    "Connection: close \r\n" +
                    "Content-Length: " + body.length() + "\r\n" +
                    "\r\n" +
                    body;
            clientSocket.getOutputStream().write(response.getBytes());
        } else {
            if (requestPath.equals("/echo")) {
                handleEchoRequest(clientSocket, requestTarget, questionPos);
            } else if (requestPath.equals("/api/showWorkers")) {
                handleGetWorker(clientSocket);
            } else {
                File file = new File(contentRoot, requestPath);
                if (!file.exists()) {
                    String body = file + " does not exist";
                    String response = "HTTP/1.1 404 Not Found\r\n" +
                            "Connection: close \r\n" +
                            "Content-Length: " + body.length() + "\r\n" +
                            "\r\n" +
                            body;
                    clientSocket.getOutputStream().write(response.getBytes());
                    return;
                }
                String statusCode = "200";
                String contentType = "text/plain";
                if (file.getName().endsWith(".html")) {
                    contentType = "text/html";
                }
                if (file.getName().endsWith(".css")) {
                    contentType = "text/css";
                }
                String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Length: " + file.length() + "\r\n" +
                        "Connection: " + "close" + "\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "\r\n";
                clientSocket.getOutputStream().write(response.getBytes());

                new FileInputStream(file).transferTo(clientSocket.getOutputStream());
            }
        }
    }

    private void handleGetWorker(Socket clientSocket) throws IOException, SQLException {
        String body = "<ul>";
        for (String workerName : productDao.list()) {
            body += "<li>" + workerName + "</li>";
        }
        body += "</ul>";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }

    private void handleEchoRequest(Socket clientSocket, String requestTarget, int questionPos) throws IOException {
        String statusCode = "200";
        String body = "Hello <strong>World</strong>!";
        if (questionPos != -1) {
            QueryString queryString = new QueryString(requestTarget.substring(questionPos + 1));
            if (queryString.getParameter("status") != null) {
                statusCode = queryString.getParameter("status");
            }
            if (queryString.getParameter("body") != null) {
                body = queryString.getParameter("body");
            }
        }
        String response = "HTTP/1.1 " + statusCode + " OK\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" +
                body;

        clientSocket.getOutputStream().write(response.getBytes());
    }

    public static void main(String[] args) throws IOException {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/kristianiashop");
        dataSource.setUser("kristianiashopuser");
        // TODO: database passwords should never be checked in!
        dataSource.setPassword("123456");

        HttpServer server = new HttpServer(8085, dataSource);
        server.setContentRoot(new File("src/main/resources/"));
    }

    public void setContentRoot(File contentRoot) {
        this.contentRoot = contentRoot;
    }

    public List<String> getWorkerNames() throws SQLException {
        return productDao.list();
    }
}