public class test{
    public static void main(String[] args) {

        // JDBC 드라이버 가동
        try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("jdbc성공");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("jdbc에러=>"+e.getMessage());
        }

    
        // 간단히 다른 스레드만 호출하고, 메인스레드는 종료
    }	}
