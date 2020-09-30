package no.kristiania.http;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class HttpClient {

    private int statusCode;
    private final Map<String, String> responseHeaders = new HashMap<>();
    private String responseBody;

    public HttpClient(final String hostname, int port, final String requestTarget) throws IOException {
        this(hostname, port, requestTarget, "GET", null);
    }

    public HttpClient(final String hostname, int port, final String requestTarget, final String httpMethod, String requestBody) throws IOException {
        Socket socket = new Socket(hostname, port);

        String contentLengthHeader = requestBody != null ? "Content-Length: " + requestBody.length() + "\r\n" : "";

        String request = httpMethod + " " + requestTarget + " HTTP/1.1\r\n" +
                "Host: " + hostname + "\r\n" +
                contentLengthHeader +
                "\r\n";

        socket.getOutputStream().write(request.getBytes());

        if (requestBody != null) {
            socket.getOutputStream().write(requestBody.getBytes());
        }

        String line = readLine(socket);
        System.out.println(line);

        String[] responseLineParts = line.split(" ");

        statusCode = Integer.parseInt(responseLineParts[1]);

        String headerLine;
        while (!(headerLine = readLine(socket)).isEmpty()){
            int colonPos = headerLine.indexOf(':');
            String headeName = headerLine.substring(0, colonPos);
            String headerValue = headerLine.substring(colonPos+1).trim();
            responseHeaders.put(headeName, headerValue);
        }

        int contentLength = Integer.parseInt(getResponseHeader("Content-Length"));
        StringBuilder body  = new StringBuilder();
        for (int i = 0; i < contentLength; i++) {
            body.append((char)socket.getInputStream().read());
        }
        responseBody = body.toString();
    }

    public static String readLine(Socket socket) throws IOException {
        StringBuilder line = new StringBuilder();
        int c;
        while ((c = socket.getInputStream().read()) != -1) {
            if (c == '\r') {

                socket.getInputStream().read();
                break;
            }
            line.append((char) c);
        }
        return line.toString();
    }

    public static void main(String[] args) throws IOException {
        new HttpClient("urlecho.appspot.com", 80, "/echo?status=404&Content-Type=text%2Fhtml&body=Hei");
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseHeader(String headerName) {
        return responseHeaders.get(headerName);
    }

    public String getResponseBody() {
        return responseBody;
    }
}