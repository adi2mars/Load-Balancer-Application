import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.json.JSONObject;

import java.time.Instant;
import java.time.Duration;

public class OrderService {
    static HttpClient UserClient = HttpClient.newBuilder().build();
    static HttpClient ProductClient = HttpClient.newBuilder().build();
    static HttpClient OrderClient = HttpClient.newBuilder().build();
    static String ICISURI = "http://";

    static Connection conn;
    static Connection UserConnection;

    static int count;
    static List<HttpClient> clients = new ArrayList<>();
    static int numWorkers;
    static ExecutorService executor;


    public static String GetUrl() {
        return "jdbc:sqlite:" + System.getProperty("user.dir") + "/compiled/OrderService/OrderService.db";
    }

    public static Connection Connect() {
        //CreateFile(GetUrl());
        Connection conn = null;
        String url = GetUrl();
        try {
            conn = DriverManager.getConnection(url);
            UserConnection = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.dir") + "/compiled/UserService/UserService.db");
        } catch (SQLException e) {
            //CreateFile(GetUrl());
            //conn = Connect();
        }
        return conn;
    }

    public static void CreateFile(String database_name) {
        File new_file = new File(database_name + ".db");
    }

    public static void CreateTable(Connection conn) {
        String sqlcmd = "CREATE TABLE IF NOT EXISTS Orders (\n user_id varchar NOT NULL,\n product_id varchar NOT NULL,\n quantity INTEGER NOT NULL\n);";
        try {
            Statement statement = conn.createStatement();
            statement.execute(sqlcmd);
        } catch (SQLException e) {
        }
    }

    public static void InsertInfo(String user_id, String product_id, int quantity, Connection conn) throws IOException{
        String sqlcmd = "INSERT INTO Orders(user_id, product_id, quantity) VALUES(?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
            statement.setString(1, user_id);
            statement.setString(2, product_id);
            statement.setInt(3, quantity);
            statement.executeUpdate();
        } catch (SQLException e) {
        }
    }

    public static void DeleteInfo(String user_id, String product_id,int quantity, Connection conn) throws IOException{
        String sqlcmd = "SELECT id FROM Orders WHERE user_id = ? AND product_id = ? AND quantity = ?";

        try (PreparedStatement statement = conn.prepareStatement("DELETE FROM Product WHERE user_id = ? AND product_id = ? AND quantity = ?")) {
            statement.setString(1, user_id);
            statement.setString(2, product_id);
            statement.setInt(3, quantity);
            statement.execute();
        } catch (SQLException e) {
        }
    }

    public static void DeleteAll(Connection conn)throws IOException{
        try(PreparedStatement statement = conn.prepareStatement("DELETE FROM Orders")){
            statement.execute();
        } catch(SQLException e){
        }
    }


    public static void CheckIDExists(String id, Connection conn, HttpExchange exchange) throws IOException{
        
        try{
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ICISURI + "/user/" + id)).GET().build();
            HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
            if(response.statusCode() != 200){
                sendResponse(exchange, response.body(), response.statusCode());
            }
        }
        catch(Exception e){
        }
        
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(args[0]), Integer.parseInt(args[1])), 0);
        System.out.println(args[0]);
        System.out.println(args[1]);
        System.out.println(args[2]);
        System.out.println(args[3]);
        
        CreateFile(GetUrl());
        conn = Connect();
        CreateTable(conn);
        

        ICISURI += args[2];
        ICISURI += ":";
        ICISURI += args[3];
        server.setExecutor(null); 
        System.out.println(ICISURI);
        numWorkers = 80;
        count = 0;

        executor = Executors.newFixedThreadPool(numWorkers);

        for (int i = 0; i < numWorkers; i++) {
            clients.add(HttpClient.newHttpClient());
        }

        server.createContext("/poweroff", new PowerOffHandler());
        server.createContext("/user", new UserHandler());
        server.createContext("/product", new ProductHandler());
        server.createContext("/restart", new RestartHandler());
        server.createContext("/order", new OrderHandler());
        server.start();
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
                    catch (Exception e){

                    }
                });
            }
            catch (Exception e){
                System.out.println(e);
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
                    try{
                        ProductHTTPRequest(clients.get(count % numWorkers), exchange);
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
                    catch(Exception e){

                    }
                });
            }
            catch (Exception e){
                System.out.println("Uh Oh");
            }
            
        }
    }

    static class PowerOffHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ICISURI + "/poweroff")).GET().build();
            try {
                HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }
            sendResponse(exchange, "{}", 200);
            System.exit(1);
            
        }
    }

    static class RestartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DeleteAll(conn);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ICISURI + "/restart")).GET().build();
            try {
                HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
            }
            catch (Exception e) {
            }
            sendResponse(exchange, "{}", 200);
            
        }
    }

    private static void UserHTTPRequest(HttpClient client, HttpExchange exchange) throws IOException{
        if ("GET".equals(exchange.getRequestMethod())) {
            String[] PathParts = exchange.getRequestURI().toString().split("/");
            if(PathParts.length == 3){
                if(!isInt(exchange.getRequestURI().toString().substring(6))){
                    sendResponse(exchange, "{}", 400);
                }

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ICISURI + exchange.getRequestURI().toString())).GET().build();
                try {
                    HttpResponse<String> response = UserClient.send(request, BodyHandlers.ofString());
                    sendResponse(exchange, response.body(), response.statusCode());
                }
                catch (Exception e) {
                    System.out.println(159);
                    System.out.println(e);
                    sendResponse(exchange, "{}", 500);
                    return;
                }
            }
            else if(PathParts.length == 4 && PathParts[2].equals("purchased")){
                String UserId = PathParts[3];
                Map<Integer, Integer> userPurchases = new HashMap<>();
                CheckIDExists(UserId, UserConnection, exchange);
                try (Statement statement = conn.createStatement()) {
                    String query = "SELECT product_id, quantity FROM orders WHERE user_id = " + UserId;

                    try (ResultSet resultSet = statement.executeQuery(query)) {
                        while (resultSet.next()) {
                            int productId = Integer.parseInt(resultSet.getString("product_id"));
                            int quantity = resultSet.getInt("quantity");
                            if (userPurchases.containsKey(productId)) {
                                userPurchases.put(productId, userPurchases.get(productId) + quantity);
                            } else {
                                userPurchases.put(productId, quantity);
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                if (userPurchases.isEmpty()){
                    sendResponse(exchange, "{}", 200);
                } else {
                    String jsonResponse = "{\n";
                    for (Map.Entry<Integer, Integer> entry : userPurchases.entrySet()) {
                        jsonResponse += "    " + entry.getKey() + ": " + entry.getValue() + ",\n";
                    }
                    jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 2) + "\n}";
                    sendResponse(exchange, jsonResponse, 200);
                }
            }               
        }

        else if ("POST".equals(exchange.getRequestMethod())) {
            String body = getRequestBody(exchange);
            JSONObject request;

            // error handling early
            try {
                request = new JSONObject(body);
                String command = request.getString("command");
                if (command.equals("create")) {
                    request.getInt("id");
                    if (request.getString("username").isEmpty()) {
                        throw new Exception();
                    } else if (request.getString("password").isEmpty()) {
                        throw new Exception();
                    } else if (request.getString("email").isEmpty()) {
                        throw new Exception();
                    }
                } else if (command.equals("update")) {
                    request.getInt("id");
                    if (request.has("username") && request.getString("username").isEmpty()) {
                        throw new Exception();
                    } else if (request.has("password") && request.getString("password").isEmpty()) {
                        throw new Exception();
                    } else if (request.has("email") && request.getString("email").isEmpty()) {
                        throw new Exception();
                    }
                } else if (command.equals("delete")) {
                    request.getInt("id");
                    if (!request.has("username")) {
                        throw new Exception();
                    } else if (!request.has("password")) {
                        throw new Exception();
                    } else if (!request.has("email")) {
                        throw new Exception();
                    }
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                sendResponse(exchange, "{}", 400);
                return;
            }

            HttpRequest requestSend = HttpRequest.newBuilder().uri(URI.create(ICISURI + exchange.getRequestURI().toString())).POST(HttpRequest.BodyPublishers.ofString(body)).build();

            try {
                HttpResponse<String> response = UserClient.send(requestSend, BodyHandlers.ofString());
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                System.out.println(208);
                System.out.println(e);
                sendResponse(exchange, "{}", 500);
            }
        }

        else {
            // Send a 405 Method Not Allowed response for non-POST/non-GET requests
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
        }
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

    private static void OrderHTTPRequest(HttpClient client, HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String reqbody = getRequestBody(exchange);
            JSONObject request;
            try {
                request = new JSONObject(reqbody);
            }
            catch (Exception e) {
                sendResponse(exchange, "{}", 400);
                return;
            }
            try {
                if (!request.getString("command").equals("place order")) {
                    throw new Exception();
                }
                request.getInt("user_id");
                request.getInt("product_id");
                int quantity = request.getInt("quantity");
                if (quantity < 0 || quantity != request.getFloat("quantity")) {
                    throw new Exception();
                }
            } catch (Exception e) {
                sendResponse(exchange, "{\"status\": \"Invalid Request\"}", 400);
                return;
            }
            try {
                HttpRequest requestSend = HttpRequest.newBuilder().uri(URI.create(ICISURI + exchange.getRequestURI().toString())).POST(HttpRequest.BodyPublishers.ofString(reqbody)).build();
                HttpResponse<String> response = UserClient.send(requestSend, BodyHandlers.ofString());
                if(response.statusCode() == 200){
                    InsertInfo(Integer.toString(request.getInt("user_id")), Integer.toString(request.getInt("product_id")), request.getInt("quantity"), conn);
                }
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                System.out.println(346);
                System.out.println(e);
                sendResponse(exchange, "{}", 500);
            }
        }

        else {
            // Send a 405 Method Not Allowed response for non-POST/non-GET requests
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
        }
    }

    private static void ProductHTTPRequest(HttpClient client, HttpExchange exchange) throws IOException {
        System.out.println("Here");
        Instant i1 = Instant.now();
        if ("GET".equals(exchange.getRequestMethod())) {
            if(!isInt(exchange.getRequestURI().toString().substring(9))){
                sendResponse(exchange, "{}", 400);
                return;
            }

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ICISURI + exchange.getRequestURI().toString())).GET().build();
            
            try {
                HttpResponse<String> response = ProductClient.send(request, BodyHandlers.ofString());
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                System.out.println(233);
                System.out.println(e);
                sendResponse(exchange, "{}", 500);
            }
        }

    else if ("POST".equals(exchange.getRequestMethod())) {
        String body = getRequestBody(exchange);
        JSONObject request;

        // error handling early
        try {
            request = new JSONObject(body);
            String command = request.getString("command");
            if (command.equals("create")) {
            request.getInt("id");
            int quantity = request.getInt("quantity");
            if (request.getString("name").isEmpty()) {
                throw new Exception();
            } else if (request.getString("description").isEmpty()) {
                throw new Exception();
            } else if (quantity < 0 || quantity != request.getFloat("quantity")) {
                throw new Exception();
            } else if (request.getFloat("price") < 0) {
                throw new Exception();
            }
                    } else if (command.equals("update")) {
            request.getInt("id");
            if (request.has("name") && request.getString("name").isEmpty()) {
                throw new Exception();
            } else if (request.has("description") && request.getString("description").isEmpty()) {
                throw new Exception();
            } else if (request.has("price") && request.getFloat("price") < 0) {
                throw new Exception();
            } else if (request.has("quantity")) {
                int quantity = request.getInt("quantity");
                if (quantity < 0 || quantity != request.getFloat("quantity")) {
                    throw new Exception();
                }
            }
                    } else if (command.equals("delete")) {
            request.getInt("id");
            if (!request.has("name")) {
                throw new Exception();
            } else if (!request.has("price")) {
                throw new Exception();
            } else if (!request.has("quantity")) {
                throw new Exception();
            }
                    } else {
            throw new Exception();
                }
            } catch (Exception e) {
                sendResponse(exchange, "{}", 400);
                return;
            }

            HttpRequest requestSend = HttpRequest.newBuilder().uri(URI.create(ICISURI + exchange.getRequestURI().toString())).POST(HttpRequest.BodyPublishers.ofString(body)).build();

            try {
                HttpResponse<String> response = UserClient.send(requestSend, BodyHandlers.ofString());
                sendResponse(exchange, response.body(), response.statusCode());
            }
            catch (Exception e) {
                System.out.println(249);
                System.out.println(e);
                sendResponse(exchange, "{}", 500);
            }
        }

        else {
            // Send a 405 Method Not Allowed response for non-POST/non-GET requests
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
        }
        System.out.println(Duration.between(i1, Instant.now()));
    }
    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
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
}