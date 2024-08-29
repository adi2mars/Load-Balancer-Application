import java.sql.Connection;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.InetAddress;
import java.security.MessageDigest;
import org.json.JSONObject;
 
public class UserService {
 
    static boolean firstMessage = true;
    static Connection conn;
    private static final ConcurrentHashMap<String, String[]> userMap = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 5000;
    private static Set<String> idCache = new HashSet<>();
    static int count;
    static int numWorkers;
    static ExecutorService executor;
 
    public static String GetUrl() {
        return "jdbc:sqlite:" + System.getProperty("user.dir") + "/compiled/UserService/UserService.db";
    }
 
    private static void initializeIdCache() {
        // Populate idCache with hashed IDs from the database
        String sqlcmd = "SELECT id FROM User";
        
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
        String sqlcmd = "CREATE TABLE IF NOT EXISTS User (\n id varchar PRIMARY KEY,\n username varchar NOT NULL,\n email varchar NOT NULL,\n password varchar NOT NULL\n);";
        try {
            Statement statement = conn.createStatement();
            statement.execute(sqlcmd);
        } catch (SQLException e) {
        }
 
    }
 
    
    public static void SelectSpecificInfo(String id, Connection conn, HttpExchange exchange) throws IOException {
 
        if (!idCache.contains(id)) {
            // Product not found in both cache and database, send 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }
 
        String[] productInfo = userMap.get(id);
 
        if (productInfo != null) {
            // Product found in cache, send response from cache
        
            sendResponse(exchange,
                    "{\"id\": " + id + ",\"username\": \"" + productInfo[0] + "\",\"email\": \"" + productInfo[1] + "\",\"password\": \"" + productInfo[2] + "\"}", 200);
            return;
        } 
        else {
            // Product not found in cache, query the database
            String sqlcmd = "SELECT id, username, email, password FROM User WHERE id = ?";
 
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, id);
                ResultSet results = statement.executeQuery();
 
                if (results.next()) {
                    // Product found in the database, update cache and send response
                    productInfo = new String[]{
                            results.getString("username"),
                            results.getString("email"),
                            results.getString("password"),
                    };
 
                    userMap.put(id, productInfo);
 
                    sendResponse(exchange,
                    "{\"id\": " + id + ",\"username\": \"" + productInfo[0] + "\",\"email\": \"" + productInfo[1] + "\",\"password\": " + productInfo[2] + "}", 200);
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
 
     public static void CheckIDExists(String id, Connection conn, HttpExchange exchange) throws IOException{
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            System.out.println("cant find the id");
            sendResponse(exchange, "{}", 404);
            return;
        }
        else{
            sendResponse(exchange, "{}", 200);
        }
     }
 
    public static void UpdateName(String id, String username, Connection conn, HttpExchange exchange) throws IOException {
        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }
 
        // Check if the product exists in the cache
        String[] productInfo = userMap.get(id);
 
        if (productInfo != null) {
            // Update the name in the cache
            productInfo[0] = username;
            userMap.put(id, productInfo);
        } else {
            // Update the name in the database
            String sqlcmd = "UPDATE User SET username = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, username);
                statement.setString(2, id);
                int rowsAffected = statement.executeUpdate();
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }
 
    public static void UpdateEmail(String id, String email, Connection conn, HttpExchange exchange) throws IOException {
        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }
 
        // Check if the product exists in the cache
        String[] productInfo = userMap.get(id);
 
        if (productInfo != null) {
            // Update the name in the cache
            productInfo[1] = email;
            userMap.put(id, productInfo);
        } else {
            // Update the name in the database
            String sqlcmd = "UPDATE User SET email = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, email);
                statement.setString(2, id);
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }
 
    public static void UpdatePassword(String id, String password, Connection conn, HttpExchange exchange) throws IOException {
        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }
 
        // Check if the product exists in the cache
        String[] productInfo = userMap.get(id);
 
        if (productInfo != null) {
            // Update the name in the cache
            productInfo[2] = password;
            userMap.put(id, productInfo);
        } else {
            // Update the name in the database
            String sqlcmd = "UPDATE User SET password = ? WHERE id = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, password);
                statement.setString(2, id);
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }
 
 
    public static void InsertInfo(String id, String username, String email, String password, Connection conn, HttpExchange exchange) throws IOException{
        if (userMap.get(id) == null) {
            idCache.add(id);
 
            //Check cache first -> No need to insert randomly into cache
            if (userMap.size() >= MAX_CACHE_SIZE) {
                bulkInsertIntoDatabase(conn);
            }
            userMap.put(id, new String[]{username, email, password});
            //This should always happen, we always want to send a response
            sendResponse(exchange, "{\"id\": " + id + ",\"username\": \"" + username + "\",\"email\": \"" + email + "\",\"password\": \"" + password + "\"}", 200);
        } else {
            sendResponse(exchange, "{}", 409);
        }
    }
 
    private static void bulkInsertIntoDatabase(Connection conn) throws IOException {
        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO USER(id, username, email, password) VALUES(?, ?, ?, ?)")){
            
            
            conn.setAutoCommit(false);
            for (Map.Entry<String, String[]> entry : userMap.entrySet()) {
                String id = entry.getKey();
                String[] productInfo = entry.getValue();
 
                statement.setString(1, id);
                statement.setString(2, productInfo[0]);
                statement.setString(3, productInfo[1]);
                statement.setString(4, productInfo[2]);
                statement.addBatch();
            }
 
            statement.executeBatch();
 
            // Clear the cache after bulk insert
            userMap.clear();
            conn.commit();
            conn.setAutoCommit(true);
        }
 
        catch (Exception e) {
            System.out.println("I hope I never see this line of code");
        }
    }
 
    public static void DeleteInfo(String id, String username, String email, String password, Connection conn, HttpExchange exchange) throws IOException {
        // Check if the ID exists in the set
        if (!idCache.contains(id)) {
            // ID not found, send a 404 response
            sendResponse(exchange, "{}", 404);
            return;
        }
 
        // Check and delete from the cache
        String[] productInfo = userMap.get(id);
 
        if (productInfo != null) {
            if (productInfo[0].equals(username) && productInfo[1].equals(email) && productInfo[2].equals(password)) {
                String sqlcmd = "DELETE FROM User WHERE id = ?";
                idCache.remove(id);
                userMap.remove(id);
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
        } else {
            // Entry not found in the cache, delete from the database
            String sqlcmd = "DELETE FROM User WHERE id = ? AND username = ? AND email = ? AND password = ?";
            try (PreparedStatement statement = conn.prepareStatement(sqlcmd)) {
                statement.setString(1, id);
                statement.setString(2, username);
                statement.setString(3, email);
                int rowsAffected = statement.executeUpdate();
 
                if (rowsAffected > 0) {
                    // Deletion successful, send a success response
                    sendResponse(exchange, "{}", 200);
                } else {
                    // Entry not found in the database, send a 404 response
                    sendResponse(exchange, "{}", 404);
                }
            } catch (SQLException e) {
                // Handle database query exception
                sendResponse(exchange, "{}", 500);
            }
        }
    }
 
    public static void DeleteAll(Connection conn, HttpExchange exchange)throws IOException{
        try(PreparedStatement statement = conn.prepareStatement("DELETE FROM User")){
            statement.execute();
        } catch(SQLException e){
            sendResponse(exchange, "{}", 500);
        }
    }
 
    /**
     * Convert byte array to hex string
     * @param hash byte array representing hashed string
     * @return String represented by the byte array
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    /**
     * function to perform SHA-256 hashing of input string
     * @param input input string to hash
     * @return String which is hashed
     * @throws Exception
     */
    public static String hashString(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash).toUpperCase();
    }
 
    public static void main(String[] args) throws IOException { 
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName(args[0]), Integer.parseInt(args[1])), 0);
 
        CreateFile(GetUrl());
        conn = Connect();
        CreateTable(conn);
        initializeIdCache();
        int threadPoolSize = 300;
        int maxRequests = 5000;
        // Create a ThreadPoolExecutor with a bounded queue to control resource consumption
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadPoolSize, // Core pool size
                threadPoolSize, // Maximum pool size
                60, // Keep-alive time for excess threads
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(maxRequests)); // Bounded queue
 
        server.setExecutor(executor);
 
 
        server.createContext("/user", new UserHandler());
        server.createContext("/poweroff", new PowerOffHandler());
        server.createContext("/restart", new RestartHandler());
        
        server.setExecutor(null); 
        server.start(); 
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Your cleanup/shutdown logic here
            //System.out.println(userMap);
            try {
                bulkInsertIntoDatabase(conn);
            }
            catch (Exception e) {
                System.out.println("It is over if we see this");
            }
        }));       
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
            userMap.clear();
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
 
    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try{
                    Integer.parseInt(exchange.getRequestURI().toString().substring(6));
                }
                catch (Exception e){
                    sendResponse(exchange, "{}", 400);
                }
                SelectSpecificInfo(exchange.getRequestURI().toString().substring(6), conn, exchange);
 
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
                    CheckIDExists(Integer.toString(request.getInt("user_id")), conn, exchange);
                }
                else {
                    String id = Integer.toString(request.getInt("id"));
                    if (command.equals("create")) {
                        try {
                            InsertInfo(id, request.getString("username"), request.getString("email"), hashString(request.getString("password")), conn, exchange);
                        } catch (Exception e) {
 
                        }
                    } else if (command.equals("update")) {
 
                        if (request.has("username")) {
                            UpdateName(id, request.getString("username"), conn, exchange);
                        }
 
                        if (request.has("email")) {
                            UpdateEmail(id, request.getString("email"), conn, exchange);
                        }
 
                        if (request.has("password")) {
                            try {
                                UpdatePassword(id, hashString(request.getString("password")), conn, exchange);
                            } catch (Exception e) {
 
                            }
                        }
 
                        SelectSpecificInfo(id, conn, exchange);
                    }
                    //TODO: Finish delete
                    else if (command.equals("delete")) {
                        if(request.has("username") && request.has("email") && request.has("password")){
                            try {
                                DeleteInfo(id, request.getString("username"), request.getString("email"), hashString(request.getString("password")), conn, exchange);
                            } catch (Exception e) {
 
                            }
                        }
                        else{
                            sendResponse(exchange, "{}", 400);
                        }
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
 
            //TODO: Remove me
            sendResponse(exchange, "{}", 500);
        }
    }

    private static void UserHTTPRequest(HttpClient client, HttpExchange exchange) throws IOException{
        if ("GET".equals(exchange.getRequestMethod())) {
            try{
                Integer.parseInt(exchange.getRequestURI().toString().substring(6));
            }
            catch (Exception e){
                sendResponse(exchange, "{}", 400);
            }
            SelectSpecificInfo(exchange.getRequestURI().toString().substring(6), conn, exchange);
 
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
                CheckIDExists(Integer.toString(request.getInt("user_id")), conn, exchange);
            }
            else {
                String id = Integer.toString(request.getInt("id"));
                if (command.equals("create")) {
                    try {
                        InsertInfo(id, request.getString("username"), request.getString("email"), hashString(request.getString("password")), conn, exchange);
                    } catch (Exception e) {
                    }
                } else if (command.equals("update")) {
                    if (request.has("username")) {
                        UpdateName(id, request.getString("username"), conn, exchange);
                    }
                    if (request.has("email")) {
                        UpdateEmail(id, request.getString("email"), conn, exchange);
                    }
                    if (request.has("password")) {
                        try {
                            UpdatePassword(id, hashString(request.getString("password")), conn, exchange);
                        } catch (Exception e) {
                        }
                    }

                    SelectSpecificInfo(id, conn, exchange);
                }
                //TODO: Finish delete
                else if (command.equals("delete")) {
                    if(request.has("username") && request.has("email") && request.has("password")){
                        try {
                            DeleteInfo(id, request.getString("username"), request.getString("email"), hashString(request.getString("password")), conn, exchange);
                        } catch (Exception e) {

                        }
                    }
                    else{
                        sendResponse(exchange, "{}", 400);
                    }
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
 
            //TODO: Remove me
            sendResponse(exchange, "{}", 500);
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
}