import java.sql.*;
import java.util.Scanner;

public class LibraryManagementSystem {
    static Connection connection;

    public static void main(String[] args) {
        //Verifying and fulfilling prerequisites
        setupSQL();
        createTables();

        //Begin
        menuPrintLoop();
    }

    //---------------sql setup ----------------
    private static void createTables() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, "libraryRecord", null);
            if (!resultSet.next()) {
                Statement statement = connection.createStatement();
                statement.execute("CREATE TABLE libraryRecord (TransactionId int auto_increment primary key, student_Id int, BookId int, DateOfIssue date, ReturnDate date, status varchar(10))");
                System.out.println("Record Table Created");
            }

            resultSet = metaData.getTables(null, null, "libraryBooks", null);
            if (!resultSet.next()) {
                Statement statement = connection.createStatement();
                statement.execute("CREATE TABLE libraryBooks (BookId int not null primary key, NameOfBook varchar (50), Publisher varchar(50), Section varchar(50), Cost int, Quantity int, CurrAvailable int)");
                System.out.println("Books Table Created");
            }

            resultSet = metaData.getTables(null, null, "studentDetails", null);
            if (!resultSet.next()) {
                Statement statement = connection.createStatement();
                statement.execute("CREATE TABLE studentDetails (student_Id int not null primary key, Name varchar(50), Branch varchar(10), Section varchar(1), Year int, Phone_No varchar(12),totalIssued int not null)");
                System.out.println("Students Table Created");
            }
        }catch(Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void setupSQL() {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/library", "root", "griff.in");
        }catch (Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

    //-----------------------------------------
    //---------menu and functionality----------
    private static void menuPrintLoop() {
        boolean quit = false;
        while(!quit){
            System.out.println("LIBRARY MANAGEMENT SYSTEM");
            System.out.println("1. Issue Book");
            System.out.println("2. Return Book");
            System.out.println("3. Add Student");
            System.out.println("4. Add Book");
            System.out.println("5. Book List");
            System.out.println("6. Student List");
            System.out.println("7. Transaction List");
            System.out.println("8. Quit");
            int choice = menuInputVerify();

            switch (choice) {
                case 1 -> issueBook();
                case 2 -> returnBook();
                case 3 -> addStudent();
                case 4 -> addBook();
                case 5 -> listBook();
                case 6 -> listStudent();
                case 7 -> listTransaction();
                case 8 -> quit = true;
            }
        }
    }

    public static void addStudent() {
        int student_Id = intInfoInp("Student Id");
        if(studentAvailableVerify(student_Id)){
            System.out.println("Student with this id is already present in records.");
            return;
        }
        String name = strInfoInp("Name");
        String branch = strInfoInp("Branch");
        String section = strInfoInp("Section");
        int year = intInfoInp("Year");
        String phoneNo = strInfoInp("Phone number");
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO studentDetails VALUES (?, ?, ?, ?, ?, ?, 0)");
            preparedStatement.setInt(1, student_Id);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, branch);
            preparedStatement.setString(4, section);
            preparedStatement.setInt(5, year);
            preparedStatement.setString(6, phoneNo);
            preparedStatement.executeUpdate();
            System.out.println("Student added successfully!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void addBook() {
        int book_Id = intInfoInp("Book Id");

        if(bookAvailableVerify(book_Id)){
            addQtyBook(book_Id);
            return;
        }

        String nameOfBook = strInfoInp("Book Name");
        String publication = strInfoInp("Publisher");
        String section = strInfoInp("Section");
        int cost = intInfoInp("Cost");
        int quantity = intInfoInp("Quantity");

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO libraryBooks VALUES (?, ?, ?, ?, ?, ? ,?)");
            preparedStatement.setInt(1, book_Id);
            preparedStatement.setString(2, nameOfBook);
            preparedStatement.setString(3, publication);
            preparedStatement.setString(4, section);
            preparedStatement.setInt(5, cost);
            preparedStatement.setInt(6, quantity);
            preparedStatement.setInt(7, quantity);
            preparedStatement.executeUpdate();
            System.out.println("Student added successfully!");
        } catch (Exception e) {
            System.out.println("error");
            System.out.println("Error: " + e.getMessage());
        }
    }
    private static void addQtyBook(int book_Id) {
        System.out.println("Book with this id is already present in records.");
        System.out.println("1. Add Quantity to Existing Record.");
        System.out.println("2. Return.");
        Scanner sc = new Scanner(System.in);

        int value;
        do{
            System.out.println("How would you like to proceed(1,2): ");
            value = sc.nextInt();
        }while(!(value>0 && value <3));

        if(value == 1) {
            try {
                System.out.println("Quantity: ");
                int qty = sc.nextInt();
                PreparedStatement preparedStatement = connection.prepareStatement("UPDATE libraryBooks set quantity = quantity + ?, currAvailable = currAvailable + ? where bookId = ?");
                preparedStatement.setInt(1, qty);
                preparedStatement.setInt(2, qty);
                preparedStatement.setInt(3, book_Id);
                preparedStatement.executeUpdate();
                System.out.println(qty + " quantity added successfully!");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void issueBook() {
        int student_Id = intInfoInp("Student Id");
        //check student available
        if (!studentAvailableVerify(student_Id)){
            System.out.println("Student not present in records!");
            return;
        }
        //check already issued allowed no. of books
        if (studentLimitReached(student_Id)){
            System.out.println("Student have already issued the max no of books allowed.");
            return;
        }

        int book_Id = intInfoInp("Book Id");
        //check book available
        if (!bookAvailableVerify(book_Id)){
            System.out.println("Book not available to issue.");
            return;
        }

        //issue book and print Transaction ID
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO libraryRecord(student_Id, BookId, DateOfIssue, status) VALUES (?, ?, curDate(), 'issued')",Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, student_Id);
            preparedStatement.setInt(2, book_Id);
            preparedStatement.executeUpdate();
            System.out.println("Book issued successfully!");
            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            System.out.println("Transaction ID:" + rs.getString(1));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        //update Student Profile
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE studentDetails set totalIssued = totalIssued +1 where student_Id = ?;");
            preparedStatement.setInt(1, student_Id);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        //update Book Count
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE libraryBooks set currAvailable = currAvailable -1 where bookId = ?;");
            preparedStatement.setInt(1, book_Id);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void returnBook() {
        int TransactionId = intInfoInp("Transaction Id");
        //check transaction id correct i.e. not already returned
        if (!statusTransactionVerify(TransactionId)){
            System.out.println("In-valid transaction id.");
            return;
        }

        int book_Id = intInfoInp("Book Id");
        if (!correctBookVerify(book_Id,TransactionId)){
            System.out.println("Issue records doesn't match.");
            return;
        }
        //penalty
        int penalty = 0;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT dateDiff(curDate(), dateOfIssue) from libraryRecord where transactionId = ?;");
            preparedStatement.setInt(1, TransactionId);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            int days = rs.getInt(1);
            if (days>5)
                penalty = days-5;
            System.out.println("Penalty: Rs."+ penalty);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        //update transaction status
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE libraryRecord SET status = 'returned', ReturnDate = curDate() where TransactionId = ?");
            preparedStatement.setInt(1, TransactionId);
            preparedStatement.executeUpdate();
            System.out.println("Book returned successfully!");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        //update Book Count
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE libraryBooks set currAvailable = currAvailable +1 where bookId = ?;");
            preparedStatement.setInt(1, book_Id);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        int Student_Id;
        //print details of transaction
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT student_Id from libraryRecord where transactionId = ?;");
            preparedStatement.setInt(1, TransactionId);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            Student_Id = rs.getInt(1);
            System.out.println("Student Id " + Student_Id + " returned Book.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        //update Student record
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE studentDetails set totalIssued = totalIssued -1 where student_Id = ?;");
            preparedStatement.setInt(1, Student_Id);
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void listBook() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from libraryBooks;");
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnsNumber = rsMetaData.getColumnCount();
            for(int i = 1 ; i <= columnsNumber; i++){
                System.out.print(rsMetaData.getColumnName(i) + " ");
            }
            System.out.println();
            while (rs.next()) {
                for(int i = 1 ; i <= columnsNumber; i++){
                    System.out.print(rs.getString(i) + " ");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void listStudent() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from studentDetails;");
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnsNumber = rsMetaData.getColumnCount();
            for(int i = 1 ; i <= columnsNumber; i++){
                System.out.print(rsMetaData.getColumnName(i) + " ");
            }
            System.out.println();
            while (rs.next()) {
                for(int i = 1 ; i <= columnsNumber; i++){
                    System.out.print(rs.getString(i) + " ");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void listTransaction() {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * from libraryRecord;");
            ResultSet rs = preparedStatement.executeQuery();
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columnsNumber = rsMetaData.getColumnCount();
            for(int i = 1 ; i <= columnsNumber; i++){
                System.out.print(rsMetaData.getColumnName(i) + " ");
            }
            System.out.println();
            while (rs.next()) {
                for(int i = 1 ; i <= columnsNumber; i++){
                    System.out.print(rs.getString(i) + " ");
                }
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    //-----------------------------------------
    //----------Input verifications------------
    private static int menuInputVerify() {
        int value;
        Scanner sc = new Scanner(System.in);
        do{
            System.out.print("Enter number(1-8): ");
            value = sc.nextInt();
        }while(!(value>0 && value <9));
        return value;
    }

    private static int intInfoInp(String str) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter "+str+": ");
        return sc.nextInt();
    }

    private static String strInfoInp(String str) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter "+str+": ");
        return sc.next();
    }

    //-----------------------------------------
    //---transaction necessary verification----
    private static boolean studentAvailableVerify(int id) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select * from studentDetails where student_id=?");
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }

    }

    private static boolean bookAvailableVerify(int id) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select currAvailable from libraryBooks where bookId=?");
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            if(rs.next()){
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }

    }

    private static boolean studentLimitReached(int student_Id) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select totalIssued from studentDetails where student_id=?");
            preparedStatement.setInt(1, student_Id);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            return (rs.getInt(1)==6);
        }catch(Exception e){
            System.out.println(e.getMessage());
            return true;
        }
    }

    private static boolean statusTransactionVerify(int t_id) {
        //compare transaction id matches with book id
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select bookId from libraryRecord where TransactionId=?");
            preparedStatement.setInt(1, t_id);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next();
        }catch(Exception e){
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static boolean correctBookVerify(int b_id, int t_id) {
        //compare transaction id matches with book id
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("select bookId from libraryRecord where TransactionId=?");
            preparedStatement.setInt(1, t_id);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            if(rs.getInt(1)==b_id){
                return true;
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
            return false;
        }
        return false;
    }

    //-----------------------------------------
}
// TODO
// 1. Make printing look like table by properly positioning text


