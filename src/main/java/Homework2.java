import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by shelton on 2017/10/29.
 */
public class Homework2 {
    private Connection conn = null;

    public Homework2(){
        try {
            this.conn = getConn();
            creatTableAndInit();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Connection getConn() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        System.out.println("初始化成功!");
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/homework5" +
                "?user=root&password=lixiaodong1996" +
                "&useUnicode=true&useSSL=false&characterEncoding=UTF8");
        connection.setAutoCommit(false);
        return connection;
    }

    private void creatTableAndInit(){
        try {
            Statement statement = conn.createStatement();
            String usrDeleteSql = "DROP TABLE IF EXISTS user;";
            String usrCreateSql = "CREATE TABLE user" +
                    "(" +
                    "  uid INT(11) AUTO_INCREMENT," +
                    "  name CHAR(30) NOT NULL ," +
                    "  phone CHAR(20) NOT NULL ," +
                    "  address CHAR(50) DEFAULT NULL ," +
                    "  balance DECIMAL(4,2) DEFAULT 0.00," +
                    "  PRIMARY KEY (uid)" +
                    ");";
            String bikeDeleteSql = "DROP TABLE IF EXISTS bike;";
            String bikeCreateSql = "CREATE TABLE bike" +
                    "(" +
                    "  bid INT(11) AUTO_INCREMENT," +
                    "  servertime DECIMAL(4,2) DEFAULT 0.0," +
                    "  PRIMARY KEY (bid)" +
                    ");";
            String recordDeleteSql = "DROP TABLE IF EXISTS record;";
            String recordCreateSql = "CREATE TABLE record" +
                    "(" +
                    "  uid INT(11) NOT NULL ," +
                    "  bid INT(11) NOT NULL ," +
                    "  startaddr char(50) NOT NULL ," +
                    "  starttime DATETIME NOT NULL ," +
                    "  endaddr char(50) NOT NULL ," +
                    "  endtime DATETIME NOT NULL ," +
                    "  cost INT(11) DEFAULT 0," +
                    "  PRIMARY KEY (uid,bid,starttime)" +
                    ");";
            statement.addBatch(usrDeleteSql);
            statement.addBatch(usrCreateSql);
            statement.addBatch(bikeDeleteSql);
            statement.addBatch(bikeCreateSql);
            statement.addBatch(recordDeleteSql);
            statement.addBatch(recordCreateSql);
            statement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

    }

    public static void main(String[] args){
        Homework2 homework2 = new Homework2();
    }
}
