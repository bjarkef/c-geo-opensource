package cgeo.geocaching.store;

import cgeo.geocaching.R;
import cgeo.geocaching.StoredList;
import cgeo.geocaching.cgCache;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Set;

public class CacheDownloader {

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


    public static class DownloaderService extends IntentService {

        private NotificationManager nm;
        private long lastStoredTime = 0L;
        private int finishedNotificationCounter = 1;

        public DownloaderService() {
            super("DownloaderService");
        }

        @Override
        public void onCreate() {
            super.onCreate();

            nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            Notification.Builder notificationBuilder = new Notification.Builder(this);
            notificationBuilder.setOngoing(true);
            notificationBuilder.setContentTitle("Storing caches offline"); /* TODO: I18N */
            notificationBuilder.setContentText("Starting..."); /* TODO: I18N */
            notificationBuilder.setSmallIcon(R.drawable.cgeo);

            nm.notify(0, notificationBuilder.getNotification());

            ArrayList<String> caches = intent.getExtras().getStringArrayList("caches");

            int num = 0;
            for (String cache : caches)
            {
                notificationBuilder.setContentText("Storing " + cache + " (" + ++num + "/" + caches.size() + ")");
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
            }

            nm.cancel(0);

            notificationBuilder.setOngoing(false);
            notificationBuilder.setContentTitle("Finished storing caches");
            notificationBuilder.setContentText("");

            nm.notify(finishedNotificationCounter++, notificationBuilder.getNotification());
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
