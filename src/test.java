import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class test {
    private static String flag = null;

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static BufferedReader  keyboard = new BufferedReader(new InputStreamReader(System.in));


    public static void main(String[] args) {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("scheduler running");
        }, 0, 3, TimeUnit.SECONDS);
        System.out.println("other tasks");
        Timer m_timer = new Timer();
        TimerTask m_task = new TimerTask() {
            @Override
            public void run() {
                scheduler.shutdownNow();
                System.exit(0);
            }
        };

        m_timer.schedule(m_task, 10000);
    }


}
