package com.greenlemonmedia.feeghe.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.greenlemonmedia.feeghe.MainActivity;
import com.greenlemonmedia.feeghe.R;

/**
 * Created by tontonskie on 5/11/15.
 */
public class GcmIntentService extends IntentService {

  public static final int NOTIFICATION_ID = 1;

  private NotificationManager mNotificationManager;
  public NotificationCompat.Builder builder;

  public GcmIntentService() {
    super("GcmIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Bundle extras = intent.getExtras();
    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
    // The getMessageType() intent parameter must be the intent you received
    // in your BroadcastReceiver.
    String messageType = gcm.getMessageType(intent);

    if (!extras.isEmpty()) {

      /*
       * Filter messages based on message type. Since it is likely that GCM
       * will be extended in the future with new message types, just ignore
       * any message types you're not interested in, or that you don't
       * recognize.
       */
      if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {

//        sendNotification("Send error: " + extras.toString());

      } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {

//        sendNotification("Deleted messages on server: " + extras.toString());

      } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

        // Post notification of received message.
        sendNotification(extras.getString("room"), extras.getString("message"));
      }
    }

    // Release the wake lock provided by the WakefulBroadcastReceiver.
    GcmBroadcastReceiver.completeWakefulIntent(intent);
  }

  // Put the message into a notification and post it.
  // This is just one simple example of what you might choose to do with
  // a GCM message.
  private void sendNotification(String roomId, String message) {
    mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra("room", roomId);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
      .setSmallIcon(R.drawable.messages_white)
      .setContentTitle(getResources().getString(R.string.app_name))
      .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
      .setContentText(message)
      .setAutoCancel(true);

    mBuilder.setContentIntent(contentIntent);
    mBuilder.setDefaults(Notification.DEFAULT_SOUND);
    mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
  }
}