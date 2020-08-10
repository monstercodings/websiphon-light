package top.codings.websiphon.light.test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;

public class TestJavaNet {
    public static void main(String[] args) throws IOException, InterruptedException {
        createHttpServer();
        HttpClient client = HttpClient.newBuilder().build();
        while (true) {
            CountDownLatch latch = new CountDownLatch(1);
            client.sendAsync(
                    HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:8080/"))
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray())
                    .whenCompleteAsync((httpResponse, throwable) -> latch.countDown())
            ;
            latch.await();
        }
    }

    private static void createHttpServer() throws IOException {
        byte[] content = new byte[1024 * 1024 * 10];
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            try {
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, content.length);
                OutputStream os = exchange.getResponseBody();
                os.write(content);
                os.flush();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }
}
