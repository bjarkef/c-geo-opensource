package cgeo.geocaching.store;

import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CacheDownloader {

    private static final String LOG_TAG = "CacheDownloader";

    private Context context;
    //private DownloaderService downloader;
    private Set<String> geocodesToStore;
    private String name;

    public CacheDownloader(Context context, String name, Set<String> geocodesToStore) {
        this.context = context;
        this.geocodesToStore = geocodesToStore;
        this.name = name;
    }

    public void start() {

        ArrayList<String> caches = new ArrayList<String>();
        caches.addAll(geocodesToStore);

        Intent service = new Intent(context, DownloaderService.class);
        service.putExtra("name", name);
        service.putStringArrayListExtra("caches", caches);

        context.startService(service);
    }


    public static class DownloaderService extends IntentService
    {
        private NotificationManager nm;
        private long lastStoredTime = 0L;
        private int finishedNotificationCounter = 1;

        private BlockingQueue<String> cacheQueue = new LinkedBlockingQueue<String>();

        private Notification.Builder notificationBuilder = new Notification.Builder(this);
        private DownloaderThread downloaderThread = new DownloaderThread();

        public DownloaderService()
        {
            super("DownloaderService");
        }

        @Override
        public void onCreate()
        {
            super.onCreate();

            nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationBuilder.setOngoing(true);
            notificationBuilder.setSmallIcon(R.drawable.cgeo);
            notificationBuilder.setContentTitle("Storing caches offline"); /* TODO: I18N */
        }

        @Override
        protected void onHandleIntent(Intent intent)
        {
            if (!downloaderThread.isAlive())
            {
                notificationBuilder.setContentText("Starting..."); /* TODO: I18N */
                nm.notify(0, notificationBuilder.getNotification());
            }

            Log.i(LOG_TAG, "Adding caches queue.");
            cacheQueue.addAll(intent.getExtras().getStringArrayList("caches"));

            if (!downloaderThread.isAlive())
            {
                Log.i(LOG_TAG, "Starting downloader thread.");
                downloaderThread.start();
            }
        }

        private class DownloaderThread extends Thread
        {
            public DownloaderThread()
            {
                super("C:Geo downloader");
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            }

            @Override
            public void run() {
                int num = 0;

                String cache = cacheQueue.poll();
                while (cache != null)
                {
                    num++;
                    String statusText = "Storing " + cache + " (" + num + "/" + (num + cacheQueue.size()) + ")";
                    Log.i(LOG_TAG, statusText);
                    notificationBuilder.setContentText(statusText);
                    nm.notify(0, notificationBuilder.getNotification());

                    if ((System.currentTimeMillis() - lastStoredTime) < 1500) {

                        int delay = 1000 + (int) (Math.random() * 1000.0) - (int) (System.currentTimeMillis() - lastStoredTime);
                        if (delay < 0) {
                            delay = 500;
                        }
                        sleepInterruptable(delay);
                    }

                    cgCache.storeCache(null, cache, StoredList.STANDARD_LIST_ID, false, null);

                    lastStoredTime = System.currentTimeMillis();

                    cacheQueue.poll();
                }

                Log.i(LOG_TAG, "Finished storing caches");

                nm.cancel(0);

                notificationBuilder.setOngoing(false);
                notificationBuilder.setContentTitle("Finished storing caches");
                notificationBuilder.setContentText("");

                nm.notify(finishedNotificationCounter++, notificationBuilder.getNotification());
            }
        }

        private void sleepInterruptable(long time)
        {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                /*
                 * Intentionally do nothing if interrupted.
                 * We simply want the sleep time to be interrupted.
                 */
            }
        }

    }

}
