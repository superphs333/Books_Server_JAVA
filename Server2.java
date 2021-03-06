import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

public class Server{

    /*
    ServerSocekt = 서버를 가동해주는 객체
    : 클라이언트에서 들어오는 요청을 기다리는 ServerSocekt을 구현하는 클래스
    */
    private ServerSocket server;

    // 생성자
    public Server2(){
        try { 
            // 서버가동
            server = new ServerSocket(2345);

            /*
            사용자 접속 대기 스레드 가동
            -> 새로운 접속자가 들어오면, 그 이벤트를 처리하기 위한 스레드
            */
            ConnectionThread thread = new ConnectionThread();
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ServerSocekt에러-" + e.getMessage());
        }
    } // end Server()

    // 실행코드
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
        new Server2();
    }

    // 대화방의 정보표현 객체
    public class Room{
        String room_idx; //  채팅방 idx
        int count; // 방인원수
        // 같은 방에 접속한 Client 정보 저장
        Vector<UserClass> userV;

        // 생성자
        public Room(String room_idx){
            userV = new Vector<>(); // 벡터 초기화
            this.room_idx = room_idx;
        }
        
    } // end Room

    // 사용자의 접속 대기를 처리하는 스레드 클래스
    class ConnectionThread extends Thread{

        // 전체룸
        Vector<Room> roomV;

        // 생성자
        public ConnectionThread(){
            // 방 리스트 초기화
            roomV = new Vector<>();
        }

        // 실행코드 
        public void run(){
            /*
            언제 들어올지 모르는 새로운 접속자를 위해 ConnectionThread는 종료되지 않고, 
            계속 무한루프를 돌면서 살아있다가
            -> server.accept()를 통해 새로운 접속자가 소켓에 접속하면
            -> 접속자의 정보를 받고 처리하는 스레드를 하나 더 분기하여 접속자 처리 담당
            */
            while(true){
                System.out.println("클라이언트를 기다리고 있습니다.");
            
                try {
                    Socket socket = server.accept();
                        /*
                        accept()메소드는 동기화에 걸렸다고 볼 수 있다
                        -> 새로운 사용자가 들어오기 전까지 더이상 코드진행 없이 멈춰있다가,
                        새로운 접속자가 소켓에 접속하면 -> 접속자의 정보를 받고 처리하는 
                        스레드를 하나 더 분기하여 접속자 처리 담당
                        */
                    System.out.println("사용자가 접속하였습니다->" + socket.toString());

                    /*
                    사용자 닉네임을 처리하는 스레드를 가동
                    */
                    NickNameThread thread = new NickNameThread(socket,this);
                    thread.start();

                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Socket에러-" + e.getMessage());
                }
            } // end while
        } // end run()
    } // end ConnectionThread


    /*
     * 닉네임 입력처리 스레드
     */
    class NickNameThread extends Thread{
        private Socket socket; // 클라이언트 소켓
        private ConnectionThread ct; // 자신의 방 찾기 위해 
        private Room myRoom; // 나의 룸 셋팅

        // 생성자
        public NickNameThread(Socket socket, ConnectionThread ct){
            this.socket = socket;
            this.ct = ct;
        }

        // 실행코드
        public void run(){
            /*
            전달받은 소켓을 통해 I/O스트림(입력 스트림)을 생성하여, 서로 데이터를 주고 받을 준비
            */
            // 스트림 추출(소켓으로부터 얻어온다)
            try {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();

                // 클라이언트로부터 메세지를 받고, 주기 위해
                    /*
                    DataInputStream, DataOutputStream
                    => FileInput/OutputStream을 상속하고 있고, 객체 생성시에 
                    InputStream, OutputStream을 매개변수 인자로 갖는다. 
                    => 이 클래스와 입출력 장치를 대상으로 하는 입출력 클래스를 
                    같이 이용하면 자바의 기본 자료형 데이를 파일 입출력 장치로 
                    직접 출력 할 수 있다.
                    */
                DataInputStream dis = new DataInputStream(is);
                DataOutputStream dos = new DataOutputStream(os);

                // 방 idx 수신
                String room_idx = dis.readUTF();
                System.out.println("room_idx=" + room_idx);

                // 방 생성하기
                    //단, 기존의 방리스트에 동일값이 없을 때만 방 리스트를 추가한다
                Room room = new Room(room_idx);
                // 방이름 중복확인
                int duplicate_count = 0;
                for(int i=0; i<ct.roomV.size(); i++){
                    String room_idx_temp = ct.roomV.get(i).room_idx;

                    // 위에 생성한 갑소가 같은 room_idx를 가지고 있다면
                    // 중복값을 증가시킨다
                    if(room_idx_temp.equals(room.room_idx)){
                        System.out.println("중복된 방idx존재");

                        // 중복값을 증가시킨다
                        duplicate_count++;
                    }
                } // end for
                if(duplicate_count == 0){
                    System.out.println("새로운방 추가");
                    ct.roomV.add(room);
                }

                /*
                login_value 수신
                */
                String login_value = dis.readUTF();
                System.out.println("login_value=" + login_value);

                /*
                닉네임 수신
                */
                String nickName = dis.readUTF();
                System.out.println("nickName=" + nickName);


                /*
                사용자 정보를 관리하는 객체를 생성
                */
                UserClass user = new UserClass(login_value, nickName, socket);

                /*
                방에 사용자 넣기
                */
                for(int i=0; i<ct.roomV.size(); i++){
                    Room r = ct.roomV.get(i);

                    /*
                    현재 인덱스에서 가리키고 있는 방의 idx값이 
                    클라이언트로 부터 가져온 방 idx값과 
                    같다면, 그 방에 넣어준다
                    */
                    if(r.room_idx.equals(room_idx)){
                        // 나의 룸으로 설정하기
                        myRoom = r;

                        //해당 룸을 나의 룸으로 설정하기
                        user.myRoom = r;

                        // 끝내기
                        break;
                    }
                } // end for

                // 해당방에 사용자를 넣는다
                myRoom.userV.add(user);

                // UserClass 스레드 시작
                user.start();

                /*
                해당방에 있는 사용자들에게만 접속 메세지 전달하기(보류)
                */
                // 날짜
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date time = new Date();
                String time1 = format.format(time);
                


            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } // end run
    } // end NickNameThread


    /*
    사용자 정보를 관리하는 클래스(하나하나의 접속자 등록 위해)
    -> 메세지를 지속적으로 받을 수 있다
    */
    class UserClass extends Thread{
        
        Room myRoom; // 나의 룸
        String login_value; // 회원식별
        String nickname ; // 닉네임
        Socket socket; // 클라이언트 소켓
        DataInputStream dis; // 데이터 받는용
        DataOutputStream dos; // 데이터 주는 용

        // 파일전송용
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        // 생성자
        public UserClass(String login_value, String nickname, Socket socket) {
            this.login_value = login_value;
            this.nickname = nickname;
            this.socket = socket;

            try {
                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                dis = new DataInputStream(is);
                dos = new DataOutputStream(os);
            } catch (IOException e) {
                System.out.println("in UserClass 생성자"+e.getMessage());
                e.printStackTrace();
            }
        } // end 생성자

        /*
        사용자로부터 메세지를 계속 수신받는 코드
        -> 해당 접속자에게 메세지를 계속 수신받기 위해 while문 안에서 작동함
        */
        public void run(){
            /* while문 작동 조정 boolean
            -> 소켓이 끊어지면, false로 작동을 멈춰준다
            (그렇지 않으면 오류 발생)
            */
            boolean run = true;

            try{

                while(run){

                    // 날짜
                    SimpleDateFormat format
                    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date time = new Date();
                    String time1 = format.format(time);

                    // 클라이언트에게 메세지를 수신받는다
                    String str = dis.readUTF();

                    if(str.equals("message")){ // 일반 메세지
                        // 수신받은 메세지
                        String msg = dis.readUTF();
                        System.out.println(nickname+":"+msg);

                        // 사용자들에게 메세지를 전달한다
                        sendToClient("message§"+ login_value+"§"+msg+"§"+time1,myRoom);
                    }
                    
                } //end while
            }catch(Exception e){
                e.printStackTrace();

                // 에러출력
                System.out.println("in UserClass="+e.getMessage());

                // close된 소켓을 myRoom에서 뺀다
                myRoom.userV.remove(this);

                // 소켓 input, output 모두 닫기
                try{
                    dis.close();
                    dos.close();
                    socket.close();

                    // 스레드 종료하기(메세제 받는)
                    run = false;
                }catch(IOException e1){
                    e1.printStackTrace();
                    System.out.println("소켓 클로즈 에러-"+e1.getMessage());
                }

                // 같은 방에 있는 클라이언트들에게 퇴장 메세지 보내기 
                
            }
        } // end run()

    } // end UserClass


    /*
    메세지를 전달하는 메소드
    */
    public synchronized void sendToClient(String msg, Room room){
        try{
            // 같은 방의 사용자들에게만 메세지를 전달해준다
            for(UserClass user : room.userV){
                user.dos.writeUTF(msg);
                user.dos.flush();
            }
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("in sendToClient"+e.getMessage());
        }
    } // end sendToClient

}
