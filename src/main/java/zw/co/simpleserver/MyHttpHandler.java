package zw.co.simpleserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
//import sun.misc.IOUtils;
import sun.security.util.IOUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MyHttpHandler implements HttpHandler {
    static final Logger log = Logger.getLogger(String.valueOf(MyHttpHandler.class));

    String url = "jdbc:mysql://localhost:3306/Account?useSSL=false";
    String userName = "root";
    String psw = "B@0783369391m1";

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        String requestParamValue = null;
        log.info(httpExchange.getRequestMethod());
        if ("GET".equals(httpExchange.getRequestMethod())) {

            requestParamValue = handleGetRequest();

        } else if ("POST".equals(httpExchange.getRequestMethod())) {

            requestParamValue = handlePostRequest(httpExchange);
        }

        handleResponse(httpExchange, requestParamValue);

    }

    public String handlePostRequest(HttpExchange httpExchange) throws IOException {
        InputStream inputStream = httpExchange.getRequestBody();
        byte[] result = IOUtils.readAllBytes(inputStream);
        String jsonString = new String(result, StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        User user = mapper.readValue(jsonString, User.class);
        if (user.getDateOfBirth() != null) {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("clientDetailsPU");
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            emf.close();
            return mapper.writeValueAsString(user) + "added";
        } else {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection con = DriverManager.getConnection(url, userName, psw);
                Statement st = con.createStatement();
                String sql = "SELECT PASSWORD FROM details WHERE email='" + user.getEmail() + "';";
                ResultSet rs = st.executeQuery(sql);
                if (!rs.next()) {
//                    log.info("account not found");
                } else {
                    if (rs.getString("PASSWORD").equals(user.getPassword())) {
                        return "\n" +
                                "{\n" +
                                "    \"login\":\"successful\"\n" +
                                "}\n" +
                                "  ";
//                        log.info("everything is working");
                    } else {
                        log.info(user.getPassword());
                        return "{\"message\":\"invalid username or password\"}";
//                        log.info(rs.getString("PASSWORD"));
//                        log.info("there is a problem");
                    }
                }
            } catch (Exception ignored) {

            }

        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, userName, psw);
            Statement st = con.createStatement();
            String sql = "SELECT * FROM details WHERE email='" + user.getEmail() + "';";
            ResultSet rs = st.executeQuery(sql);
            rs.next();
            user.setUserid(rs.getLong("Userid"));
            con.close();
            return mapper.writeValueAsString(user);

        } catch (Exception e) {

            log.severe(e.toString());

        }

        return " {" + "\"error\":\"error adding user\"\n" + "}";
    }

    private String handleGetRequest() throws IOException {
//get users from db

        List<User> users = new ArrayList<>();

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, userName, psw);
            Statement st = con.createStatement();
            String query = "SELECT * FROM details";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                User user = new User();
                user.setEmail(rs.getString("email"));
                user.setUserid(rs.getLong("Userid"));
                user.setSurname(rs.getString("surname"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setAddress(rs.getString("address"));
                user.setFirstname(rs.getString("Firstname"));
                user.setDateOfBirth(rs.getString("dateOfBirth"));
                users.add(user);
            }
        } catch (Exception e) {
            log.severe("got exception");
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, users);
        return out.toString();
    }

    private void handleResponse(HttpExchange httpExchange, String requestParamValue) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        httpExchange.getResponseHeaders().add("Content-Type", "application/json");
        httpExchange.getResponseHeaders().add("Status", "200");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS, HEAD, PUT, POST");
        httpExchange.sendResponseHeaders(200, requestParamValue.length());

        outputStream.write(requestParamValue.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}