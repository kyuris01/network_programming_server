import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AuctionServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = new ArrayList<>();

    //경매품목 - 굿즈(쿠,건덕이,건구스,건붕이)
    private static final List<String> goods = Arrays.asList("쿠", "건구스", "건덕이", "건붕이");
    //경매품목 - 아이템(건구스의지원금, 황소의분노, 일감호의기적, 스턴건)
    private static final List<String> items = Arrays.asList("건구스의 지원금", "황소의 분노", "일감호의 기적", "스턴건");
    //경매품목 - 아이템
    private static String currentItem;
    private static int currentBid = 0;
    private static ClientHandler highestBidder = null;

    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(PORT);
        System.out.println("서버가 실행 중입니다");

        // 경매 라운드를 자동 시작 (예시로 10초마다 새로운 경매 시작)
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(AuctionServer::startNewAuctionRound, 0, 10, TimeUnit.SECONDS);

        try {
            while (true) {
                Socket clientSocket = listener.accept();

                // 클라이언트를 관리하는 핸들러 생성 및 추가
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);

                // 새로운 스레드로 클라이언트 핸들러 실행
                new Thread(clientHandler).start();
            }
        } finally {
            listener.close();
        }
    }

    //주기적으로 새로운 경매품목을 랜덤으로 선택하고 클라이언트에게 알림
    private static void startNewAuctionRound() {
        Random random = new Random();

        // 60% 확률로 굿즈, 40% 확률로 아이템 선택
        if (random.nextInt(100) < 60) {
            // 굿즈 선택: 4개 중 하나를 동일한 확률로 선택
            currentItem = goods.get(random.nextInt(goods.size()));
        } else {
            // 아이템 선택(지원금10%, 외 나머지3개 30%동일)
            int itemChance = random.nextInt(100);
            if (itemChance < 10) {
                currentItem = "건구스의 지원금"; // 10% 확률
            } else if (itemChance < 40) {
                currentItem = "황소의 분노"; // 30% 확률
            } else if (itemChance < 70) {
                currentItem = "일감호의 기적"; // 30% 확률
            } else {
                currentItem = "스턴건"; // 30% 확률
            }
        }

        currentBid = 0;
        highestBidder = null;
        broadcastMessage("경매를 시작합니다. 경매품목: " + currentItem);
    }

    public static synchronized void placeBid(ClientHandler client, int bidAmount) {
        if (bidAmount > currentBid) {
            currentBid = bidAmount;
            highestBidder = client;
            broadcastMessage("새로운 최고 입찰: " + bidAmount + "원 - " + client.getClientName());
        }
    }

    //브로드캐스트
    public static void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public static void endAuctionRound() {
        if (highestBidder != null) {
            broadcastMessage("낙찰자: " + highestBidder.getClientName() + " - 금액: " + currentBid);
        } else {
            broadcastMessage("낙찰자 없음");
        }
        for (ClientHandler client : clients) {
            if (!client.isParticipating()) {
                client.addFunds(5);
            }
        }
        startNewAuctionRound();
    }
}
