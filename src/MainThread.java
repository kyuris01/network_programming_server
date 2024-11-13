

public class MainThread {

    public static void main(String[] args) {
        System.out.println("경매 서버 시작...");
        WaitingThread waitingThread = new WaitingThread();
        waitingThread.start();

    }
}
