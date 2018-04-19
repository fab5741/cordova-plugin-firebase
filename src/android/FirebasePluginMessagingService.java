package org.apache.cordova.firebase;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;
import android.annotation.TargetApi;
import android.app.Service;
import android.os.Build;

import junit.framework.Test;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebasePlugin";

    @TargetApi(Build.VERSION_CODES.O)
    public String decrypt(String key, String encrypted) {
        try {
            Key k = new SecretKeySpec(Base64.getDecoder().decode(key), "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, k);
            byte[] decodedValue = Base64.getDecoder().decode(encrypted);
            byte[] decValue = c.doFinal(decodedValue);
            String decryptedValue = new String(decValue);
            return decryptedValue;
        } catch (IllegalBlockSizeException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadPaddingException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidKeyException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        String title;
        String text;
        String id;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            text = remoteMessage.getNotification().getBody();
            id = remoteMessage.getMessageId();
        } else {
            title = remoteMessage.getData().get("title");
            text = remoteMessage.getData().get("text");
            id = remoteMessage.getData().get("id");
        }

        if (TextUtils.isEmpty(id)){
            Random rand = new Random();
            int  n = rand.nextInt(50) + 1;
            id = Integer.toString(n);
        }

        Log.d(TAG, "Notification Message Title: " + title);
        Log.d(TAG, "Notification Message Body/Text: " + text);

        title = decrypt( "SECURE KEY", title);
        text = decrypt( "SECURE KEY", text);

        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Notification Message id: " + id);
        Log.d(TAG, "Notification Message Title: " + title);
        Log.d(TAG, "Notification Message Body/Text: " + text);

        String removeTag = remoteMessage.getData().get("removeTag");
        if (!TextUtils.isEmpty(removeTag)) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Log.d(TAG, "Notification Message Remove TAG: " + removeTag);
            notificationManager.cancel(removeTag, 0);
        }

        sendNotificationPlugin(remoteMessage.getData());

        if ((!TextUtils.isEmpty(text) || !TextUtils.isEmpty(title)) && (FirebasePlugin.inBackground() || !FirebasePlugin.hasNotificationsCallback())) {
            sendNotification(id, title, text, remoteMessage.getData());
        }
    }

    private void sendNotification(String id, String title, String messageBody, Map<String, String> data) {
        Bundle bundle = new Bundle();
        for (String key : data.keySet()) {
            bundle.putString(key, data.get(key));
        }

        Intent intent = new Intent(this, OnNotificationOpenReceiver.class);
        intent.putExtras(bundle);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        int resID = getResources().getIdentifier("silhouette", "drawable", getPackageName());
        if (resID != 0) {
            notificationBuilder.setSmallIcon(resID);
        } else {
            notificationBuilder.setSmallIcon(getApplicationInfo().icon);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id.hashCode(), notificationBuilder.build());
    }

    private void sendNotificationPlugin(Map<String, String> data) {
        Bundle bundle = new Bundle();
        for (String key : data.keySet()) {
            bundle.putString(key, data.get(key));
        }
        bundle.putBoolean("tap", false);
        FirebasePlugin.sendNotification(bundle);
    }
}
