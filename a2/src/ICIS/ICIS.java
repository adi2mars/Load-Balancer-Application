import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.List;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class ICIS {
    static HttpClient UserClient = HttpClient.newBuilder().build();
    static HttpClient ProductClient = HttpClient.newBuilder().build();
    static String UserServerURI = "http://";
    static String ProductServerURI = "http://";
    static int count;
    static List<HttpClient> clients = new ArrayList<>();
    static int numWorkers;
    static ExecutorService executor;

    public static void main(String[] args) throws IOException {
        System.out.println(args[0]);
        System.out.println(args[1]);
        System.out.println(args[2]);
        System.out.println(args[3]);
        System.out.println(args[4]);
        System.out.println(args[5]);

        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(args[0]), Integer.parseInt(args[1])), 0);
        ProductServerURI += args[3];
        ProductServerURI += ":";
        ProductServerURI += args[2].substring(0, 3);
        UserServerURI += args[5];
        UserServerURI += ":";
        UserServerURI += args[4];

        System.out.println(ProductServerURI);

        numWorkers = 100; //Change this
        count = 0; //Add this as a parameter to maintain order
        
        server.setExecutor(null); 
        executor = Executors.newFixedThreadPool(numWorkers);

        for (int i = 0; i < numWorkers; i++) {
            clients.add(HttpClient.newHttpClient());
        }
        
        server.setExecutor(Executors.newFixedThreadPool(20)); 

        server.createContext("/user", new UserHandler());
        server.createContext("/poweroff", new PowerOffHandler());
        server.createContext("/product", new ProductHandler());
        server.createContext("/restart", new RestartHandler());
        server.createContext("/order", new OrderHandler());
        server.start();
    }

    private static boolean isInt(String str){
        int len = str.length();
        if(len == 0){
            return false;
        }
        int start = 0;
        if(str.charAt(0) == '-'){
            if(len == 1){
                return false;
            }
            start = 1;
        }
        while(start < len){
            char c = str.charAt(start);
            if(c < '0' || c > '9'){
                return false;
            }
            start++;
        }
        return true;
    }

    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            count += 1;
            try {
                executor.execute(() -> {
                    try{
                        UserHTTPRequest(clients.get(count % numWorkers), exchange);
                    }
                    catch(Exception e){

                    }
                });
            }
            catch (Exception e){
                System.out.println("Uh Oh");
            }
        }
    }

    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            count += 1;
            try {
                executor.execute(() -> {
                    try{
                        OrderHTTPRequest(clients.get(count % numWorkers), exchange);
                    }
                    catch (Exception e){

                    }
                });
            }
            catch (Exception e){
                System.out.println("Uh Oh");
            }
        }
    }

    static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            count += 1;
            try {
                executor.execute(() -> {
                    ProductHTTPRequest(clients.get(count % numWorkers), exchange);
                });
            }
            catch (Exception e){
                System.out.println("Uh Oh");
            }
        }
    }

    private static void UserHTTPRequest(HttpClient client, HttpExchange exchange) throws IOException{
        if ("GET".equals(exchange.getRequestMethod())) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(UserServerURI + exchange.getRequestURI().toString())).GET().build();
            try {
                HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                sendResponse(exchange, "{}", 500);
            }
        }

        else if ("POST".equals(exchange.getRequestMethod())) {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(UserServerURI + exchange.getRequestURI().toString())).POST(HttpRequest.BodyPublishers.ofString(getRequestBody(exchange))).build();
                
            try {
                HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                sendResponse(exchange, "{}", 500);
            }
        }
        else {
            // Send a 405 Method Not Allowed response for non-POST/non-GET requests
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
        }
    }

    private static void OrderHTTPRequest(HttpClient client, HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String reqbody = getRequestBody(exchange);
            JSONObject request = new JSONObject(reqbody);
                        
            HttpRequest requestSend = HttpRequest.newBuilder().uri(URI.create(UserServerURI + "/user/" + request.getInt("user_id"))).POST(HttpRequest.BodyPublishers.ofString(reqbody)).build();
            HttpResponse<String> product_response, user_response;
            try {
                user_response = UserClient.send(requestSend, BodyHandlers.ofString());
                if(user_response.statusCode() != 200){
                    sendResponse(exchange, "{\"status\": \"Invalid Request\"}", user_response.statusCode());
                    return;
                }
                requestSend = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + "/product")).POST(HttpRequest.BodyPublishers.ofString(reqbody)).build();
                try {
                    product_response = ProductClient.send(requestSend, BodyHandlers.ofString());
                    if(product_response.statusCode() != 200){
                        sendResponse(exchange, product_response.body(), product_response.statusCode());
                    }
                    else{
                        sendResponse(exchange, "{\"product_id\":" + request.getInt("product_id") + ",\"user_id\":" + request.getInt("user_id") + ",\"quantity\":" + request.getInt("quantity") + ",\"status\":\"" + product_response.body() + "\"}", 200);
                    }
                }
                catch (Exception e) {
                    sendResponse(exchange, "{}", 500);
                }
            }
            catch (Exception e) {
                sendResponse(exchange, "{}", 500);
            }
        }
        else {
            // Send a 405 Method Not Allowed response for non-POST/non-GET requests
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
        }
    }

    private static void ProductHTTPRequest(HttpClient client, HttpExchange exchange) {
        if ("GET".equals(exchange.getRequestMethod())) {
            String idHash = String.valueOf(Integer.parseInt(exchange.getRequestURI().toString().substring(8)) % 5);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + idHash + exchange.getRequestURI().toString())).GET().build();
            try {
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                System.out.println("Failed");
            }            
        }

        else if ("POST".equals(exchange.getRequestMethod())) {
            try { 
                JSONObject reqBody = new JSONObject(getRequestBody(exchange));
                String idHash = String.valueOf(Integer.parseInt(reqBody.getString("id")) % 5);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + idHash + "/product")).POST(HttpRequest.BodyPublishers.ofString(getRequestBody(exchange))).build();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                System.out.println(e);
                System.exit(1);
            }
        }
        else {
            // Send a 405 Method Not Allowed response for non-POST/non-GET requests
            try {
                exchange.sendResponseHeaders(405, 0);
                exchange.close();
            }
            catch (Exception e) {
                System.out.println(e);
            }
        }
    }
    static class PowerOffHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(UserServerURI + "/poweroff")).GET().build();
            try {
                HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }
            request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + "1/poweroff")).GET().build();
            try {
                HttpResponse<String> response = ProductClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }

            request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + "2/poweroff")).GET().build();
            try {
                HttpResponse<String> response = ProductClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }
            
            request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + "3/poweroff")).GET().build();
            try {
                HttpResponse<String> response = ProductClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }

            request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + "4/poweroff")).GET().build();
            try {
                HttpResponse<String> response = ProductClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }

            request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + "0/poweroff")).GET().build();
            try {
                HttpResponse<String> response = ProductClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }
            sendResponse(exchange, "", 200);
            System.exit(1);
        }
    }

    static class RestartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(UserServerURI + "/restart")).GET().build();
            try {
                HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }
            request = HttpRequest.newBuilder().uri(URI.create(ProductServerURI + "/restart")).GET().build();
            try {
                HttpResponse<String> response = ProductClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }
            sendResponse(exchange, "", 200);
        }
    }

    private static String getRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                requestBody.append(line);
            }
            return requestBody.toString();
        }
    }

    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}

