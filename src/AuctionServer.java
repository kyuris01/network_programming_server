import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionServer extends Thread {
    private static final int PORT = 6003;
//    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final ArrayList<ClientHandler> participantsList = new ArrayList<>(WaitingThread.clientNum); //플레이어 객체 담는 배열
    //경매품목 - 굿즈(쿠,건덕이,건구스,건붕이)
    private static final List<String> goods = Arrays.asList("쿠", "건구스", "건덕이", "건붕이");
    //경매품목 - 아이템(건구스의지원금, 황소의분노, 일감호의기적, 스턴건)
    private static final List<String> items = Arrays.asList("건구스의 지원금", "황소의 분노", "일감호의 기적", "스턴건");
    //경매품목 - 아이템
    private static String currentItem;
    private static int currentBid = 0;
    private static ClientHandler highestBidder = null;
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(WaitingThread.clientNum);
    private Boolean bidUpdated = false;
    private static Boolean endGame = false;
    private static Boolean endRound = false;
    private static final int TIMEOUT = 7;



    public static ArrayList<ClientHandler> getParticipantsList() {
        return participantsList;
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
        //broadcastMessageToPlayer를 쓰면 응찰참여 클라이언트에게만 메시지 전달됨. 근데 아직 응찰여부 설정안했으므로 false로 되어있어서 클라이언트쪽에 이 메시지 전달안되었던듯
    }






    //모든 클라이언트에게 브로드캐스트
    public static void broadcastMessage(String message) {
        for (ClientHandler client : WaitingThread.clientQueue) {
            client.sendMessage(message);
        }
    }
    
    //응찰참여자에게만 브로드캐스트
    public static void broadcastMessageToPlayers(String message) {
        for (ClientHandler client : WaitingThread.clientQueue) {
            if (client.getParticipating()) {
                client.sendMessage(message);
            }
        }
    }

    public static void endAuctionRound() {
        System.out.println("낙찰자와 승리자를 판정합니다");
        broadcastMessage("낙찰자와 승리자를 판정합니다");
        if (highestBidder != null) {
            broadcastMessage("낙찰자: " + highestBidder.getClientName() + " - 금액: " + currentBid);
            System.out.println("낙찰자: " + highestBidder.getClientName() + " - 금액: " + currentBid);
        } else {
            broadcastMessage("낙찰자 없음");
            System.out.println("낙찰자 없음");
        }
        for (ClientHandler client : WaitingThread.clientQueue) {


            if (!client.getParticipating()) {
                client.addFunds(5);
            }
            client.setBid(false);
            client.setParticipating(false);
            client.setBidPrice(0);
            client.setBidAmount(0);
            participantsList.remove(client);
        }

        if(checkWinner()) {
            System.out.println("게임종료");
            broadcastMessage("게임종료");
            endGame = true;
        }

    }

    public static boolean allElementAreSame(List<String> items) { //승리조건 판정 중 같은굿즈 조건판정

        if (items.size() != 4) {
            return false; // 비어 있는 리스트는 모든 원소가 같다고 가정
        }
        
        String firstElement = items.get(0);

        for (String item : items) {
            if (!item.equals(firstElement)) {
                return false;
            }
        }
        return true;

    }

    public static boolean allElementAreDifferent(List<String> items) {
        if (items.size() != 4) {
            return false;
        }
        if (items.size() == new HashSet<>(items).size()) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean checkWinner() {

        for (ClientHandler participant : participantsList) {
            ArrayList<String>items = participant.getItems();
            if (allElementAreDifferent(items) || allElementAreSame(items)) {
                System.out.println("승리자는 " + participant.getClientName());
                broadcastMessage("승리자는 " + participant.getClientName());
                return true;
            }
        }
        return false;
    }

    public void checkParticipation() throws InterruptedException {

        broadcastMessage("응찰하시겠습니까?");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        for (ClientHandler client : WaitingThread.clientQueue) {
            Future<String> future = executor.submit(() -> client.userInputProcessor());

            try {
                future.get(TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                client.sendMessage("응찰 대기시간이 지났습니다");
                executor.shutdownNow();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }


        for (ClientHandler client : WaitingThread.clientQueue) {
            String userCmd = client.getUserCommand();
            if (!client.getParticipating()) {
                continue;
            }
            if (!(userCmd.equals("참가") || userCmd.equals("불참여"))) {
                client.sendMessage("잘못된 입력값입니다");
            } else {
                if (userCmd.equals("참가")) {
                    client.setParticipating(true);
                    getParticipantsList().add(client);
                    broadcastMessage(client.getClientName() + " 님이 경매에 참여했습니다");
                } else {
                    client.setParticipating(false);
                    client.sendMessage("경매에 불참합니다");
                    client.sendMessage("게임이 끝날때까지 대기합니다");
                }

            }
        }


    }

    public static synchronized void placeBid(ClientHandler client, int bidAmount) {
        currentBid +=bidAmount;
        highestBidder = client;
        broadcastMessageToPlayers("새로운 최고 입찰: " + currentBid + "원 - " + client.getClientName());
    }


    public Boolean clientBidReceiver(ClientHandler participant) throws InterruptedException {
        String userMsg = participant.userInputProcessor();
        participant.setBid(true);
        if( participant.getBalance() >= participant.getBidAmount() ) {
            participant.setBalance(participant.getBalance() - participant.getBidAmount());
            placeBid(participant, participant.getBidAmount());
        } else {
            participant.sendMessage("잔액이 부족합니다!");
        }
        return participant.getBid();
    }



    public void roundRefresher() {

        ExecutorService clientExecutor = Executors.newCachedThreadPool();
        ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(WaitingThread.clientNum);
        AtomicBoolean isReset = new AtomicBoolean(false);
        AtomicBoolean clientExecutorDone = new AtomicBoolean(false);


//        try {
//            isReset.set(false);
//
//            broadcastMessageToPlayers("호가를 시작하세요");
//            ScheduledFuture<?> timeoutFuture = timeoutScheduler.scheduleWithFixedDelay(() -> {
//                if (!isReset.get()) {
//                    System.out.println("대기 시간이 만료되었습니다.");
//                    broadcastMessageToPlayers("대기시간이 만료되었습니다.");
//                    timeoutScheduler.shutdown();
//                    clientExecutor.shutdownNow();
//                    clientExecutorDone.set(true);
//                } else {
//                    isReset.set(false); // 타이머를 리셋할 필요가 있는 경우
//                }
//            }, TIMEOUT, TIMEOUT, TimeUnit.SECONDS);
//
//
//
//            for (ClientHandler participant : participantsList) {
//                clientExecutor.submit(() -> {
//                    try {
//
//                        while (clientBidReceiver(participant) && !clientExecutorDone.get()) {
//                            System.out.println(participant.getClientName() + "클라이언트 입력 받음: " + participant.getBidAmount());
//                            isReset.set(true); // 입력이 올 때마다 타이머를 리셋할 수 있도록 설정
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                });
//            }
//
//            // 타이머가 종료될 때까지 대기
////            clientExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
//            try {
//                timeoutFuture.get();
//            } catch (CancellationException e) {
//                System.out.println("타이머가 취소되었습니다.");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (!timeoutScheduler.isShutdown()) {
//                timeoutScheduler.shutdownNow();
//            }
//            if (!clientExecutor.isShutdown()) {
//                clientExecutor.shutdownNow();
//            }
//            System.out.println("Refresher 종료");
//        }
        try {
            isReset.set(false);
            broadcastMessageToPlayers("호가를 시작하세요");

            Runnable timeoutTask = new Runnable() {
                @Override
                public void run() {
                    if (!isReset.get()) {
                        System.out.println("대기 시간이 만료되었습니다.");
                        broadcastMessageToPlayers("대기시간이 만료되었습니다.");
                        timeoutScheduler.shutdown();
                        clientExecutor.shutdownNow();
                        clientExecutorDone.set(true);
                    } else {
                        isReset.set(false);  // 타이머를 리셋
                        timeoutScheduler.schedule(this, TIMEOUT, TimeUnit.SECONDS);  // 새로운 타이머 예약
                    }
                }
            };

            timeoutScheduler.schedule(timeoutTask, TIMEOUT, TimeUnit.SECONDS);

            for (ClientHandler participant : participantsList) {
                clientExecutor.submit(() -> {
                    try {
                        while (clientBidReceiver(participant) && !clientExecutorDone.get()) {
                            System.out.println(participant.getClientName() + "클라이언트 입력 받음: " + participant.getBidAmount());
                            isReset.set(true); // 입력 시 타이머 리셋 플래그 설정
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            timeoutScheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!timeoutScheduler.isShutdown()) {
                timeoutScheduler.shutdownNow();
            }
            if (!clientExecutor.isShutdown()) {
                clientExecutor.shutdownNow();
            }
            System.out.println("Refresher 종료");
        }


    }

    @Override
    public void run() {
        try {
            while(!endGame) {
                startNewAuctionRound();
                checkParticipation(); //해당 라운드 참여여부 결정

                roundRefresher();
                endAuctionRound();
            }


            System.out.println("게임끝");






        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (ClientHandler client : WaitingThread.clientQueue) {
                try {
                    client.getSocket().close();
                } catch (IOException e) {}
            }
            System.exit(0);
        }

    }
}
