import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler{ //게임참여자의 정보 담는 클래스
    private Boolean isBid = false; //호가 여부
    private Boolean isParticipating = false; //해당 응찰라운드 참여여부
    private Integer bidPrice;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String clientName;
    private int balance = 100;
    private int bidAmount = 0;
    ArrayList<String> items = new ArrayList<>();


    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public BufferedReader getIn() {
        return in;
    }

    public PrintWriter getOut() {
        return out;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public ArrayList<String> getItems() {
        return items;
    }

    public void setItems(ArrayList<String> items) {
        this.items = items;
    }

    public int getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(int bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Boolean getBid() {
        return isBid;
    }

    public void setBid(Boolean bid) {
        isBid = bid;
    }

    public Boolean getParticipating() {
        return isParticipating;
    }

    public void setParticipating(Boolean participating) {
        isParticipating = participating;
    }

    public Integer getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(Integer bidPrice) {
        this.bidPrice = bidPrice;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void addFunds(int amount) {
        balance += amount;
        sendMessage("잔액 추가됨: " + amount + "원. 현재 잔액: " + balance + "원");
    }

    public void sendMessage(String message) {
        out.println(message);
    }
    
    public void participationProcess() {
        
    }

    public String userInputProcessor () {
        String userMessage = "";
        String userCommand = "";
        String chatMessage = "";

        try {
            userMessage = in.readLine();


            if (userMessage.startsWith("호가")) {
                bidAmount = Integer.parseInt(userMessage.split(" ")[1]);
                userCommand = userMessage.split(" ")[0];
            } else if (userMessage.startsWith("채팅")) {
                chatMessage = userMessage.substring(3);
                userCommand = userMessage.split(" ")[0];
            } else {
                userCommand = userMessage;
            }

//            switch (userCommand) {
//                case "참가":
//                    isParticipating = true;
//                    AuctionServer.getParticipantsList().add(this);
//                    AuctionServer.broadcastMessage(clientName + " 님이 경매에 참여했습니다");
//                    break;
//                case "호가":
//                    isBid = true;
//                    if( balance >= bidAmount ) {
//                        balance -= bidAmount;
//                        AuctionServer.placeBid(this, bidAmount);
//                    } else {
//                        sendMessage("잔액이 부족합니다!");
//                    }
//                    break;
//                case "불참여":
//                    isParticipating = false;
//                    sendMessage("경매에 불참합니다");
//                    sendMessage("게임이 끝날때까지 대기합니다");
//                    break;
//                case "채팅":
//                    AuctionServer.broadcastMessage("채팅 " + clientName + ": " + chatMessage);
//                    break;
//                default:
//                    sendMessage("잘못된입력입니다");
//                    System.out.println("잘못된입력");
//            }



        } catch (Exception e) {
            e.printStackTrace();
        }
        return userCommand;
    }

    public void run() {
        userInputProcessor();
    }

}