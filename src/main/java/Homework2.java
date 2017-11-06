import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by shelton on 2017/10/29.
 */
public class Homework2 {
    private Connection conn = null;
    private String[] filePaths = new String[]{"resource/bike.txt","resource/record.txt","resource/user.txt"};
    private HashMap<String,Double> balance = new HashMap<String, Double>();
    private HashMap<String,Double> servertime = new HashMap<String, Double>();

    public Homework2(){
        try {
            this.conn = getConn();
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
        //建表
        try {
            Statement statement = conn.createStatement();
            String usrDeleteSql = "DROP TABLE IF EXISTS user;";
            String usrCreateSql = "CREATE TABLE user" +
                    "(" +
                    "  uid INT(11) NOT NULL," +
                    "  name CHAR(30) NOT NULL ," +
                    "  phone CHAR(20) NOT NULL ," +
                    "  address CHAR(50) DEFAULT NULL ," +
                    "  balance DECIMAL(14,2) DEFAULT 0.00," +
                    "  PRIMARY KEY (uid)" +
                    ")default charset = utf8;";
            String createUserIdIndex = "CREATE  INDEX  username ON user(uid);";

            String bikeDeleteSql = "DROP TABLE IF EXISTS bike;";
            String bikeCreateSql = "CREATE TABLE bike" +
                    "(" +
                    "  bid INT(11)," +
                    "  servertime DECIMAL(4,2) DEFAULT 0.0," +
                    "  PRIMARY KEY (bid)" +
                    ")default charset = utf8;";
            String createBikeIdIndex = "CREATE INDEX bikeid ON bike(bid);";

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
                    ")default charset = utf8;";
            statement.addBatch(usrDeleteSql);
            statement.addBatch(usrCreateSql);
            statement.addBatch(createUserIdIndex);

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

        //初始化用户余额和车辆服务时间的HashMap；
        initHashMap();


        //插入record数据
        String initRecordSql = "INSERT INTO record(uid, bid, startaddr, starttime, endaddr, endtime, cost) values (?,?,?,?,?,?,?)";
        try {
            PreparedStatement recordPrepareStatement = conn.prepareStatement(initRecordSql);
            ArrayList<String[]> recordDatalist = readFile(filePaths[1]);
            for(String[] line : recordDatalist){
                String uid = line[0];
                String bid = line[1];
                String startaddr = line[2];
                String starttime = line[3];
                String endaddr = line[4];
                String endtime = line[5];
                int cost = calculateCost(starttime,endtime);
                if(!canBeInsert(uid,bid,cost,starttime,endtime)){
                    continue;
                }
                recordPrepareStatement.setString(1,uid);
                recordPrepareStatement.setString(2,bid);
                recordPrepareStatement.setString(3,startaddr);
                recordPrepareStatement.setString(4,starttime);
                recordPrepareStatement.setString(5,endaddr);
                recordPrepareStatement.setString(6,endtime);
                recordPrepareStatement.setInt(7,cost);
                recordPrepareStatement.addBatch();
            }
            recordPrepareStatement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

        //插入user数据
        String initUserSql = "INSERT INTO user(uid, name, phone, balance) values (?,?,?,?)";
        try {
            PreparedStatement userPrestatement = conn.prepareStatement(initUserSql);
            ArrayList<String[]> usrDatalist = readFile(filePaths[2]);
            for(int numRow = 0;numRow<usrDatalist.size();numRow++){
                String[] line = usrDatalist.get(numRow);
                String id = line[0];
                String name = line[1];
                String phone = line[2];
                Double balanceNow = balance.get(id);
                userPrestatement.setString(1,id);
                userPrestatement.setString(2,name);
                userPrestatement.setString(3,phone);
                userPrestatement.setDouble(4,balanceNow);
                userPrestatement.addBatch();
            }
            userPrestatement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }


        //插入bike数据
        String initBikeSql = "INSERT INTO bike(bid,servertime) values (?,?);";
        try {
            PreparedStatement bikePreparedStatement = conn.prepareStatement(initBikeSql);
            ArrayList<String[]> bikedatalist = readFile(filePaths[0]);
            for(String[] row : bikedatalist){
                Double servertimeNow = servertime.get(row[0]);
                bikePreparedStatement.setString(1,row[0]);
                bikePreparedStatement.setDouble(2,servertimeNow);
                bikePreparedStatement.addBatch();
            }
            bikePreparedStatement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }



                //添加更新余额的触发器
        String deleteTrigger = "DROP TRIGGER IF EXISTS updateBalance";

        String updateBalanceSql = "CREATE TRIGGER updateBalance " +
                "AFTER INSERT ON record " +
                "FOR EACH ROW " +
                "  BEGIN " +
                "    UPDATE user u SET u.balance = u.balance - NEW.cost WHERE u.uid = NEW.uid; " +
                "  END;";
        try {
            Statement updateBalanceStatement = conn.createStatement();
            updateBalanceStatement.addBatch(deleteTrigger);
            updateBalanceStatement.addBatch(updateBalanceSql);
            updateBalanceStatement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }

        //更新单车使用时间的触发器
        String deleteUpdateServertimeTrigger = "DROP TRIGGER IF EXISTS updateServertime;";
        String createUpdateServertimeTrigger = "CREATE TRIGGER updateServertime " +
                "AFTER INSERT ON record " +
                "FOR EACH ROW " +
                "  BEGIN " +
                "    UPDATE bike b SET b.servertime = b.servertime + (TIMESTAMPDIFF(MINUTE,NEW.starttime,NEW.endtime)/60) WHERE b.bid = NEW.bid; " +
                "  END;";
        try {
            Statement servertimeTriggerStatement = conn.createStatement();
            servertimeTriggerStatement.addBatch(deleteUpdateServertimeTrigger);
            servertimeTriggerStatement.addBatch(createUpdateServertimeTrigger);
            servertimeTriggerStatement.executeBatch();
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

    private void initHashMap(){
        ArrayList<String[]> usrDatalist = readFile(filePaths[2]);
        for(int numRow = 0;numRow<usrDatalist.size();numRow++){
            String[] line = usrDatalist.get(numRow);
            String uid = line[0];
            Double money = Double.parseDouble(line[3]);
            balance.put(uid,money);
        }
        ArrayList<String[]> bikedatalist = readFile(filePaths[0]);
        for(String[] row : bikedatalist){
            String bid = row[0];
            servertime.put(bid,0.0);
        }

    }

    //判断record是否可插入
    private boolean canBeInsert(String uid,String bid,int cost,String starttime,String endtime){
        Double balanceNow = balance.get(uid);
        if(balanceNow>=cost){
            balanceNow = balanceNow-cost;
            balance.put(uid,balanceNow);
            Double timeCost  = calculateTime(starttime,endtime);
            Double severtimeNow = servertime.get(bid);
            severtimeNow = severtimeNow + timeCost;
            servertime.put(bid,severtimeNow);
            return true;
        }else{
            return false;
        }
    }

    private Double calculateTime(String starttime,String endtime){
        Date begin = null;
        Double hour = 0.0;
        try {
            begin = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss").parse(starttime);
            Date end = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss").parse(endtime);
            long minutes = (end.getTime() - begin.getTime())/(1000*60);
            hour = minutes/60.0;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return hour;
    }


    //计算用车费用
    private int calculateCost(String starttime,String endTime){
        int cost = 0;
        try {
            Date begin = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss").parse(starttime);
            Date end = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss").parse(endTime);
            long minutes = (end.getTime() - begin.getTime())/(1000*60);
            if(minutes>=0&&minutes<=30){
                cost = 1;
            }else if (minutes>30&&minutes<=60){
                cost = 2;
            }else if (minutes>60&&minutes<=90){
                cost = 3;
            }else if(minutes>90){
                cost = 4;
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return cost;
    }

    //设置用户的住址
    public void setAddress(){
        String setSql = "UPDATE user SET address = (SELECT r.endaddr" +
                                             "   FROM record r" +
                                             "   WHERE user.uid = r.uid AND HOUR(r.endtime) BETWEEN 18 AND 24" +
                                             "   GROUP BY r.endaddr" +
                                             "   ORDER BY COUNT(*) DESC LIMIT 0,1);";
        try {
            Statement statement = conn.createStatement();
            statement.execute(setSql);
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

//插入用车记录，尝试只用sql实现很难，只能借助java代码
    public void setRecord(int uid, int bid, String startaddr,Date starttime, String endaddr, Date endtime){
        warning(uid);//如果账户余额为负，警告不能用车


        //更新账户余额，每插入一次用车记录则更新
        String deleteTrigger = "DROP TRIGGER IF EXISTS updateBalance";

        String updateBalanceSql = "CREATE TRIGGER updateBalance " +
                "AFTER INSERT ON record " +
                "FOR EACH ROW " +
                "  BEGIN " +
                "    UPDATE user u SET u.balance = u.balance - NEW.cost WHERE u.uid = NEW.uid; " +
                "  END;";
        try {
            Statement updateStatement = conn.createStatement();
            updateStatement.addBatch(deleteTrigger);
            updateStatement.addBatch(updateBalanceSql);
            updateStatement.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }


        long diff = endtime.getTime() - starttime.getTime();
        long minuts = diff/(1000*60);
        int cost = 0;
        if(minuts>=0 && minuts <= 30){
            cost = 1;
        }else if(minuts > 30 && minuts <=60){
            cost = 2;
        }else if(minuts > 60 && minuts < 90){
            cost  = 3;
        }else if(minuts > 90){
            cost = 4;
        }
        String begintimeString = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(starttime);
        String endtimeString = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(endtime);
        String insertSql = "INSERT INTO record VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement statement = conn.prepareStatement(insertSql);
            statement.setInt(1,uid);
            statement.setInt(2,bid);
            statement.setString(3,startaddr);
            statement.setString(4,begintimeString);
            statement.setString(5,endaddr);
            statement.setString(6,endtimeString);
            statement.setInt(7,cost);
            statement.addBatch();
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

        warning(uid);

        String updateservertime = "UPDATE bike b SET b.servertime = b.servertime + ? ";
        try {
            PreparedStatement updateServertimeStat = conn.prepareStatement(updateservertime);
            updateServertimeStat.setDouble(1,minuts/30);
            updateServertimeStat.addBatch();
            updateServertimeStat.executeBatch();
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

    private void warning(int uid){
        try {
            PreparedStatement selectSql = conn.prepareStatement("SELECT balance FROM user WHERE uid = ?");
            selectSql.setInt(1,uid);
            ResultSet result = selectSql.executeQuery();
            conn.commit();
            double balance = 0;
            while (result.next()){
                balance = result.getDouble(1);
            }
            if(balance<0){
                System.out.println("金额为负，不能使用共享单车");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    //每月初检车，所有服务用时大于两百的车要维修，并把服务用时置零
    public void resetServertime(){
        String deleteEventSql = "DROP EVENT IF EXISTS evt_test";
        String createEventSql = "CREATE EVENT evt_test " +
                "ON SCHEDULE EVERY 1 MONTH STARTS date_add(curdate()-day(curdate())+1,interval 1 month) " +
                "DO " +
                "  BEGIN " +
                "    CREATE TABLE IF NOT EXISTS needRepair( " +
                "      bid INT(11) NOT NULL, " +
                "      PRIMARY KEY (bid) " +
                "    )DEFAULT CHARSET = utf8; " +
                " " +
                "    INSERT INTO needRepair (SELECT b.bid FROM bike b WHERE b.servertime > 200); " +
                " " +
                "    UPDATE bike b SET b.servertime = 0 WHERE b.servertime > 200; " +
                "  END;";
        try {
            Statement statement = conn.createStatement();
            statement.addBatch(deleteEventSql);
            statement.addBatch(createEventSql);
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


    private ArrayList<String[]> readFile(String path){
        File file = new File(path);
        ArrayList<String[]> result = new ArrayList<String[]>();
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line  = "";
            String[] line_datas = null;
            try {
                while ((line = bufferedReader.readLine())!=null){
                    line_datas = line.split(";");
                    result.add(line_datas);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return result;

    }

    public static void main(String[] args){
        long begin = 0;
        long end = 0;
        Homework2 homework2 = new Homework2();
        System.out.println("正在建表并插入所有信息，更新用户账户，设置单车使用时间......");
        begin = System.currentTimeMillis();
        homework2.creatTableAndInit();
        end = System.currentTimeMillis();
        System.out.println("完成！用时"+(end-begin)+"ms");
        System.out.println("正在设置用户住址信息......");
        begin = System.currentTimeMillis();
        homework2.setAddress();
        end = System.currentTimeMillis();
        System.out.println("完成！用时"+(end-begin)+"ms");
        Date starttime = null;
        Date endtime = null;
        try {
            starttime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2017-10-05 05:23:34");
            endtime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2017-10-05 06:23:34");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String startaddr = "南园10舍";
        String endaddr = "北大楼";
        System.out.println("正在插入一条用车记录......");
        begin = System.currentTimeMillis();
        homework2.setRecord(2,6970,startaddr,starttime,endaddr,endtime);
        end = System.currentTimeMillis();
        System.out.println("完成！用时"+(end-begin)+"ms");
        System.out.println("正在设置月初检车车辆耗损的事件......");
        begin = System.currentTimeMillis();
        homework2.resetServertime();
        end = System.currentTimeMillis();
        System.out.println("完成！用时"+(end-begin)+"ms");
    }
}
