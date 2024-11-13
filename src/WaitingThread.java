import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class WaitingThread extends Thread{

    private static final int PORT = 6003;
    private static final int MIN_CLIENTS = 1;
    public static Queue<ClientHandler> clientQueue = new LinkedList<>();
    public static int clientNum = 0;
    private BufferedReader in;
    private PrintWriter out;

    @Override
    public void run() {
        AuctionServer auctionServer = new AuctionServer();


        try (ServerSocket listener = new ServerSocket(PORT)) {

            while (true) {

                Socket clientSocket = listener.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                in = clientHandler.getIn();
                out = clientHandler.getOut();
                String clientName = in.readLine();
                clientHandler.setClientName(clientName);
                System.out.println("클라이언트 \"" + clientHandler.getClientName() + "\" 가 연결되었습니다: " + clientHandler.getSocket());
                clientQueue.add(clientHandler);
                System.out.println("클라이언트가 접속하였습니다. 현재 접속자 수 : " + clientQueue.size());

                if (clientQueue.size() >= MIN_CLIENTS) {
                    AuctionServer.broadcastMessage("게임이 곧 시작됩니다...");
                    break;
                }
            }

            clientNum = clientQueue.size();
            auctionServer.start();

        } catch (IOException e) {
            System.out.println("서버 오류: " + e.getMessage());
        }


    }
}
