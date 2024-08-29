import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.File;
import java.io.IOException;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;


import org.json.JSONObject;

public class ProductService {

    static boolean firstMessage = true;
    static Connection conn;
    private static final ConcurrentHashMap<String, String[]> productMap = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 2000;
    private static Set<String> idCache = new HashSet<>();
    static int count;
    static int numWorkers;
    static ExecutorService executor;

    private static void initializeIdCache() {
        // Populate idCache with hashed IDs from the database
        String sqlcmd = "SELECT id FROM Product";
        
        try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                String id = results.getString("id");
                idCache.add(id);
            }
        } catch (SQLException e) {
            // Handle database query exception
            e.printStackTrace();
        }
    }

    public static void SelectSpecificInfo(String id, Connection conn, HttpExchange exchange) throws IOException {

        if (!idCache.contains(id)) {
            // Product not found in both cache and database, send 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }

        String[] productInfo = productMap.get(id);

        if (productInfo != null) {
            // Product found in cache, send response from cache
            sendResponse(exchange,
                    "{\"id\": " + id + ",\"name\": \"" + productInfo[0] + "\",\"description\": \"" + productInfo[1] + "\",\"price\": " + productInfo[2] + ",\"quantity\": " + productInfo[3] + "}", 200);
        } else {
            // Product not found in cache, query the database
            String sqlcmd = "SELECT id, name, description, price, quantity FROM Product WHERE id = ?";

            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, id);
                ResultSet results = statement.executeQuery();

                if (results.next()) {
                    // Product found in the database, update cache and send response
                    productInfo = new String[]{
                            results.getString("name"),
                            results.getString("description"),
                            results.getString("price"),
                            String.valueOf(results.getInt("quantity"))
                    };

                    productMap.put(id, productInfo);

                    sendResponse(exchange,
                            "{\"id\": " + id + ",\"name\": \"" + productInfo[0] + "\",\"description\": \"" + productInfo[1] + "\",\"price\": " + productInfo[2] + ",\"quantity\": " + productInfo[3] + "}", 200);
                } else {
                    // Product not found in both cache and database, send 404 response
                    sendResponse(exchange, "{}", 404);
                }
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }

    public static void UpdateName(String id, String name, Connection conn, HttpExchange exchange) throws IOException {
        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }

        // Check if the product exists in the cache
        String[] productInfo = productMap.get(id);

        if (productInfo != null) {
            // Update the name in the cache
            productInfo[0] = name;
            productMap.put(id, productInfo);
        } else {
            // Update the name in the database
            String sqlcmd = "UPDATE Product SET name = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, name);
                statement.setString(2, id);
                int rowsAffected = statement.executeUpdate();
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }

    public static void UpdateDescription(String id, String description, Connection conn, HttpExchange exchange) throws IOException{

        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }

        // Check if the product exists in the cache
        String[] productInfo = productMap.get(id);

        if (productInfo != null) {
            // Update the name in the cache
            productInfo[1] = description;
            productMap.put(id, productInfo);
        } else {
            // Update the name in the database
            String sqlcmd = "UPDATE Product SET description = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, description);
                statement.setString(2, id);
                int rowsAffected = statement.executeUpdate();
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }

    public static void UpdatePrice(String id, String price, Connection conn, HttpExchange exchange) throws IOException{

        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }

        // Check if the product exists in the cache
        String[] productInfo = productMap.get(id);

        if (productInfo != null) {
            // Update the name in the cache
            productInfo[2] = price;
            productMap.put(id, productInfo);
        } else {
            // Update the name in the database
            String sqlcmd = "UPDATE Product SET price = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, price);
                statement.setString(2, id);
                int rowsAffected = statement.executeUpdate();
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }

    public static void UpdateQuantity(String id, String quantity, Connection conn, HttpExchange exchange) throws IOException{

        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }

        // Check if the product exists in the cache
        String[] productInfo = productMap.get(id);

        if (productInfo != null) {
            // Update the name in the cache
            productInfo[3] = quantity;
            productMap.put(id, productInfo);
        } else {
            // Update the name in the database
            String sqlcmd = "UPDATE Product SET quantity = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, quantity);
                statement.setString(2, id);
                int rowsAffected = statement.executeUpdate();
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }

    public static void RemoveQuantity(String id, int quantity, Connection conn, HttpExchange exchange) throws IOException {

        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{\"status\": \"Invalid Request\"}", 404);
            return;
        }

        // Check if the product exists in the cache
        String[] productInfo = productMap.get(id);

        if (productInfo != null) {
            int currentQuantity = Integer.parseInt(productInfo[3]);

            if (currentQuantity >= quantity) {
                // Update quantity in the cache
                currentQuantity -= quantity;
                productInfo[3] = String.valueOf(currentQuantity);
                productMap.put(id, productInfo);

                // Send a success response
                sendResponse(exchange, "Success", 200);
            } else {
                // Exceeded quantity limit, send a 400 response
                sendResponse(exchange, "{\"status\": \"Exceeded quantity limit\"}", 400);
            }
        } else {
            // Update quantity in the database
            String sqlcmd = "UPDATE Product SET quantity = quantity - ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setInt(1, quantity);
                statement.setString(2, id);
                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    // Update successful, send a success response
                    sendResponse(exchange, "Success", 200);
                } else {
                    // Entry not found in the database, send a 404 response
                    sendResponse(exchange, "{\"status\": \"Invalid Request\"}", 404);
                }
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }

    public static void InsertInfo(String id, String name, String description, String price, String quantity, Connection conn, HttpExchange exchange) throws IOException{
        // Check for duplicates in the set
        if (!idCache.contains(id)) {
            // Add ID to the set
            idCache.add(id);

            // Add information to the cache
            String[] existingInfo = productMap.putIfAbsent(id, new String[]{name, description, price, quantity});

            // Check if the cache size exceeds the maximum allowed
            if (productMap.size() > MAX_CACHE_SIZE) {
                // Perform a bulk insert in the database
                bulkInsertIntoDatabase(conn);
            }

            // Send a success response
            sendResponse(exchange, "{\"id\": " + id + ",\"name\": \"" + name + "\",\"description\": \"" + description + "\",\"price\": " + price + ",\"quantity\": " + quantity + "}", 200);
        } else {
            // Duplicate ID, send a conflict response
            sendResponse(exchange, "{}", 409);
        }
    }

    protected static void bulkInsertIntoDatabase(Connection conn) throws IOException {
        try {
            conn.setAutoCommit(false);
            PreparedStatement statement = conn.prepareStatement("INSERT OR REPLACE INTO Product(id, name, description, price, quantity) VALUES(?, ?, ?, ?, ?)");
            for (Map.Entry<String, String[]> entry : productMap.entrySet()) {
                String[] productInfo = entry.getValue();

                statement.setString(1, entry.getKey());
                statement.setString(2, productInfo[0]);
                statement.setString(3, productInfo[1]);
                statement.setString(4, productInfo[2]);
                statement.setInt(5, Integer.parseInt(productInfo[3]));
                statement.addBatch();
            }

            // Execute batch insert
            statement.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            // Clear the cache after successful bulk insert
            productMap.clear();

            // No need to send a response for successful bulk insert
        } 
        catch (SQLException e) {
        }
    }

    public static void DeleteInfo(String id, String name, String price, int quantity, Connection conn, HttpExchange exchange) throws IOException {
        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }

        // Check and delete from the cache
        String[] productInfo = productMap.get(id);
        

        if (productInfo != null) {
            if (productInfo[0].equals(name) && productInfo[2].equals(price) && productInfo[3].equals(String.valueOf(quantity))) {
                String sqlcmd = "DELETE FROM Product WHERE id = ?";
                idCache.remove(id);
                productMap.remove(id);
                try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                    statement.setString(1, id);
                    int rowsAffected = statement.executeUpdate();

                    sendResponse(exchange, "{}", 200);
                }
                catch (Exception e) {
                    System.out.println("Failed to delete");
                }
            }
            else {
              
                sendResponse(exchange, "{}", 404);
            }
        } 
        else {
            // Entry not found in the cache, delete from the database
            String sqlcmd = "DELETE FROM Product WHERE id = ? AND name = ? AND price = ? AND quantity = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, id);
                statement.setString(2, name);
                statement.setString(3, String.valueOf(price));
                statement.setInt(4, quantity);
                int rowsAffected = statement.executeUpdate();

                if (rowsAffected > 0) {
                    // Deletion successful, send a success response
                    sendResponse(exchange, "{}", 200);
                } else {
                    // Entry not found in the database, send a 404 response
                    sendResponse(exchange, "{}", 404);
                }
                idCache.remove(id);
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }


    public static void DeleteAll(Connection conn, HttpExchange exchange)throws IOException{
        productMap.clear();
        try(PreparedStatement statement = conn.prepareStatement("DELETE FROM Product")){
            statement.execute();
        } catch(SQLException e){
            sendResponse(exchange, "{}", 500);
        }
    }

    public static void main(String[] args) throws IOException { 
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(args[0]), Integer.parseInt(args[1])), 0);
        CreateFile(GetUrl());
        conn = Connect();
        CreateTable(conn);

        initializeIdCache();
        server.setExecutor(null); 
        numWorkers = 80;
        count = 0;

        executor = Executors.newFixedThreadPool(numWorkers);

        server.createContext("/product", new ProductHandler());
        server.createContext("/poweroff", new PowerOffHandler());
        server.createContext("/restart", new RestartHandler());
        
        server.setExecutor(null); 
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Your cleanup/shutdown logic here
            try {
                bulkInsertIntoDatabase(conn);
            }
            catch (Exception e) {
                System.out.println("Failed shutdown");
            }
            //updateDatabaseBeforeShutdown(conn);
        }));
        System.out.println(args[1]);
    }

    static class PowerOffHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, "{}", 200);
            System.exit(1);
        }
    }

    static class RestartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            DeleteAll(conn, exchange);
            idCache.clear();
            productMap.clear();
            sendResponse(exchange, "{}", 200);
        }
    }

    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
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

    static class ProductHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            count += 1;
            try {
                executor.execute(() -> {
                    try{
                        ProductHTTPRequest(exchange);
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

     private static void ProductHTTPRequest(HttpExchange exchange) throws IOException {
        Instant i1 = Instant.now();

        if ("GET".equals(exchange.getRequestMethod())) {
            SelectSpecificInfo(exchange.getRequestURI().toString().substring(9), conn, exchange);
        }

        else if ("POST".equals(exchange.getRequestMethod())) {
            String reqbody = getRequestBody(exchange);
            JSONObject request = new JSONObject(reqbody);

            String command = request.getString("command");
            if(firstMessage){
                if(command.equals("restart")){
                    sendResponse(exchange, "{\"command\": \"restart\"}", 200);
                    firstMessage = false;
                    return;
                }
                else{
                    DeleteAll(conn, exchange);
                }
                firstMessage = false;
            }
            if(command.equals("shutdown")){
                sendResponse(exchange, "{\"command\": \"shutdown\"}", 200);
                System.exit(1);
            }
            if (command.equals("place order")) {
                RemoveQuantity(Integer.toString(request.getInt("product_id")), request.getInt("quantity"), conn, exchange);
            } else {
                String id = Integer.toString(request.getInt("id"));
                if (command.equals("create")) {
                    InsertInfo(id, request.getString("name"), request.getString("description"), String.valueOf(request.getFloat("price")), String.valueOf(request.getInt("quantity")), conn, exchange);
                    return;
                } else if (command.equals("update")) {
                    if (request.has("name")) {
                        UpdateName(id, request.getString("name"), conn, exchange);
                    }
                    if (request.has("description")) {
                        UpdateDescription(id, request.getString("description"), conn, exchange);
                    }
                    if (request.has("price")) {
                        UpdatePrice(id, String.valueOf(request.getFloat("price")), conn, exchange);
                    }
                    if (request.has("quantity")) {
                        UpdateQuantity(id, String.valueOf(request.getInt("quantity")), conn, exchange);
                    }
                    SelectSpecificInfo(id, conn, exchange);
                }
                //TODO: Finish delete
                else if (command.equals("delete")) {
                    DeleteInfo(id, request.getString("name"), String.valueOf(request.getFloat("price")), request.getInt("quantity"), conn, exchange);
                    } else {
                    sendResponse(exchange, "{}", 400);
                    return;
                }
            }
        }
        else {
            // Send a 405 Method Not Allowed response for non-POST/non-GET requests
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
        }
        System.out.println(Duration.between(i1, Instant.now()));
        return;
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.sendResponseHeaders(200, response.length());
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

    public static String GetUrl() {
        return "jdbc:sqlite:" + System.getProperty("user.dir") + "/compiled/ProductService/ProductService.db";
    }

    public static Connection Connect() {
        //CreateFile(GetUrl());
        Connection conn = null;
        String url = GetUrl();
        try {
            conn = DriverManager.getConnection(url);
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
        String sqlcmd = "CREATE TABLE IF NOT EXISTS Product (\n id varchar PRIMARY KEY,\n name varchar NOT NULL,\n description varchar NOT NULL,\n price varchar NOT NULL,\n quantity INTEGER NOT NULL\n);";
        try {
            Statement statement = conn.createStatement();
            statement.execute(sqlcmd);
        } catch (SQLException e) {
        }
    }
}