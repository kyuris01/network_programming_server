
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
        import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicTimeoutServer {
    private static final int PORT = 6003;
    private static final int CLIENT_COUNT = 1;
    private static final int TIMEOUT_SECONDS = 10;
    private static PrintWriter out;

    public static void main(String[] args) {
        ExecutorService clientExecutor = Executors.newFixedThreadPool(CLIENT_COUNT);
        ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);
        AtomicBoolean isReset = new AtomicBoolean(false);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버가 시작되었습니다. 클라이언트 연결을 기다립니다...");

            List<Socket> clientSockets = new ArrayList<>();

            // 4개의 클라이언트 연결 수락
            for (int i = 0; i < CLIENT_COUNT; i++) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("클라이언트 " + (i + 1) + " 연결됨");
            }

            // 대기 시간 타이머 설정
            ScheduledFuture<?> timeoutFuture = timeoutScheduler.scheduleWithFixedDelay(() -> {
                if (!isReset.get()) {
                    System.out.println("대기 시간이 만료되었습니다. 다음 로직으로 이동합니다.");
                    out.println("대기시간 만료");
                    timeoutScheduler.shutdown();
                    clientExecutor.shutdownNow();
                } else {
                    isReset.set(false); // 타이머를 리셋할 필요가 있는 경우
                }
            }, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // 각 클라이언트의 입력을 기다리는 스레드
            for (Socket clientSocket : clientSockets) {
                clientExecutor.submit(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                        String input;
                        out = new PrintWriter(clientSocket.getOutputStream(), true);
                        while ((input = reader.readLine()) != null) {
                            System.out.println("클라이언트 입력 받음: " + input);
                            isReset.set(true); // 입력이 올 때마다 타이머를 리셋할 수 있도록 설정
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // 타이머가 종료될 때까지 대기
            clientExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!timeoutScheduler.isShutdown()) {
                timeoutScheduler.shutdownNow();
            }
            if (!clientExecutor.isShutdown()) {
                clientExecutor.shutdownNow();
            }
            System.out.println("서버 종료.");
        }
    }
}
