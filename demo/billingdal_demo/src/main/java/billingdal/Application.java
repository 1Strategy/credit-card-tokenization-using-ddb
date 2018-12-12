//   Copyright 2018 1Strategy, LLC

//     Licensed under the Apache License, Version 2.0 (the "License");
//     you may not use this file except in compliance with the License.
//     You may obtain a copy of the License at

//         http://www.apache.org/licenses/LICENSE-2.0

//     Unless required by applicable law or agreed to in writing, software
//     distributed under the License is distributed on an "AS IS" BASIS,
//     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//     See the License for the specific language governing permissions and
//     limitations under the License.

package billingdal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.onestrategy.Crypto;
import com.onestrategy.AwsDynamodbKmsEncryptionProvider;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.*;
import java.util.*;
@SpringBootApplication
@RestController
public class Application {
    public static final String MYSQL_DB_URL = "jdbc:mysql://your_rds_mysql_DB_URL.rds.amazonaws.com/yourmysqldbname";
    public static final String MYSQL_DB_USER = "username";
    public static final String MYSQL_DB_PASSWORD = "password";

    @RequestMapping("/")
    public String home() {
        return "Billingdal Demo";
    }

    @RequestMapping("/add")
    public String addPANumber(@RequestParam(value="number") String number) {
        Crypto encryptor = new AwsDynamodbKmsEncryptionProvider();
        return encryptor.encrypt(number);
    }

    @RequestMapping("/get")
    public String getPANumber(@RequestParam(value="token") String token) {
        Crypto encryptor = new AwsDynamodbKmsEncryptionProvider();
        return encryptor.decrypt(token);
    }

    @GetMapping("/orders")
    public List<Order> getAllOrders() throws SQLException, Exception {
        List<Order> result = new ArrayList<Order>();
        Crypto encryptor = new AwsDynamodbKmsEncryptionProvider();

        Connection conn;
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_DB_USER, MYSQL_DB_PASSWORD);
        String selectQuery = "SELECT UserPaymentAccountID, OrderName, Description, AWS_PA_Token from OrderHistory";
        PreparedStatement select = conn.prepareStatement(selectQuery);

        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            String name = rs.getString("OrderName");
            String description = rs.getString("Description");
            String token = rs.getString("AWS_PA_Token");
            int userPaymentAccountID = rs.getInt("UserPaymentAccountID");
            String paNumber = encryptor.decrypt(token);
            Order o = new Order();
            o.setId(userPaymentAccountID);
            o.setName(name);
            o.setDetail(description);
            String last4 = "";
            if (paNumber.length() <= 4) {
                last4 = paNumber;
            } else {
                last4 = paNumber.substring(paNumber.length() - 4);
            }
            o.setPaymentMethod("Credit Card .... " + last4);

            result.add(o);
        }

        rs.close();

        return result;
    }

    @GetMapping("/order")
    public List<Order> getOrder(@RequestParam(value="id") int id) throws SQLException, Exception {
        List<Order> result = new ArrayList<Order>();
        Crypto encryptor = new AwsDynamodbKmsEncryptionProvider();

        Connection conn;
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_DB_USER, MYSQL_DB_PASSWORD);
        String selectQuery = "SELECT UserPaymentAccountID, OrderName, Description, AWS_PA_Token from OrderHistory WHERE UserPaymentAccountID = ?";
        PreparedStatement select = conn.prepareStatement(selectQuery);
        select.setInt(1, id);
        ResultSet rs = select.executeQuery();
        while (rs.next()) {
            String name = rs.getString("OrderName");
            String description = rs.getString("Description");
            String token = rs.getString("AWS_PA_Token");
            int userPaymentAccountID = rs.getInt("UserPaymentAccountID");
            String paNumber = encryptor.decrypt(token);
            Order o = new Order();
            o.setId(userPaymentAccountID);
            o.setName(name);
            o.setDetail(description);
            String last4 = "";
            if (paNumber.length() <= 4) {
                last4 = paNumber;
            } else {
                last4 = paNumber.substring(paNumber.length() - 4);
            }
            o.setPaymentMethod("Credit Card .... " + last4);

            result.add(o);
        }

        rs.close();

        return result;
    }

    @PostMapping("/order")
    public ResponseEntity newOrder(@ModelAttribute Order order) throws SQLException, Exception {
        List<Order> result = new ArrayList<Order>();
        Crypto encryptor = new AwsDynamodbKmsEncryptionProvider();

        Connection conn;
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(MYSQL_DB_URL, MYSQL_DB_USER, MYSQL_DB_PASSWORD);
        String insertQuery = "INSERT INTO OrderHistory (OrderName, Description, AWS_PA_Token) VALUES (?, ?, ?)";
        PreparedStatement insert = conn.prepareStatement(insertQuery);
        insert.setString(1, order.getName());
        insert.setString(2, order.getDetail());
        String token = encryptor.encrypt(order.getPaymentMethod());
        insert.setString(3, token);
        insert.executeUpdate();
        return new ResponseEntity(HttpStatus.CREATED);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
