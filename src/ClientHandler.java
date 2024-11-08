import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;

    //경매 참여 여부 플래그
    private boolean participating = false;

    //소지금
    private int balance = 100;

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String getClientName() {
        return clientName;
    }

    public boolean isParticipating() {
        return participating;
    }

    public void addFunds(int amount) {
        balance += amount;
        sendMessage("잔액 추가됨: " + amount + "원. 현재 잔액: " + balance + "원");
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            clientName = in.readLine();

            System.out.println("클라이언트 \"" + clientName+ "\" 가 연결되었습니다: " + socket);
            AuctionServer.broadcastMessage(clientName + " 님이 참가했습니다.");

            while (true) {
                String command = in.readLine();
                if (command == null) break;

                if (command.startsWith("참가")) {
                    participating = true;
                    AuctionServer.broadcastMessage(clientName + " 님이 경매에 참가했습니다.");
                } else if (command.startsWith("호가")) {
                    int bidAmount = Integer.parseInt(command.split(" ")[1]);
                    if (balance >= bidAmount) {
                        balance -= bidAmount;
                        AuctionServer.placeBid(this, bidAmount);
                    } else {
                        sendMessage("잔액 부족으로 호가 실패.");
                    }
                } else if (command.startsWith("불참여")) {
                    participating = false;
                    sendMessage("경매에 불참했습니다.");
                } else if (command.startsWith("채팅")) {
                    String chatMessage = command.substring(3); // "채팅 " 부분을 제거
                    AuctionServer.broadcastMessage("채팅 " + clientName + ": " + chatMessage);
                }
            }
        } catch (IOException e) {
            System.out.println("연결 종료: " + clientName);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("소켓 종료 오류");
            }
        }
    }
}
