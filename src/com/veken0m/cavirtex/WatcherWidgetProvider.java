package com.veken0m.cavirtex;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;

public class WatcherWidgetProvider extends BaseWidgetProvider {

	@Override
	public void onReceive(Context ctxt, Intent intent) {

		if (REFRESH.equals(intent.getAction())) {
			// createNotification(ctxt, R.drawable.bitcoin, "Recieved Update",
			// "It equals REFRESH", "onRecieved");
			readPreferences(ctxt);
			ctxt.startService(new Intent(ctxt, UpdateService.class));

		} else if (PREFERENCES.equals(intent.getAction())) {

			readPreferences(ctxt);

		} else {
			super.onReceive(ctxt, intent);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		setAlarm(context);
		// context.startService(new Intent(context, UpdateService.class));
		// //needed to update when app widget is created

	}

	/**
	 * This class lets us refresh the widget whenever we want to
	 */
	public static class UpdateService extends IntentService {

		public UpdateService() {
			super("WatcherWidgetProvider$UpdateService");
		}

		/*
		 * Depreciated in Android Developper website; Use onStartCommand.
		 * 
		 * @Override public void onStart(Intent intent, int i) {
		 * super.onStart(intent, i); }
		 */
		@Override
		public void onCreate() {
			readPreferences(getApplicationContext());
			// getApplicationContext().startService(new
			// Intent(getApplicationContext(), UpdateService.class)); //Disabled
			// because caused widget to be updated twice (sound, vibrate)
			super.onCreate();
		}

		public int onStartCommand(Intent intent, int flags, int startId) {
			// buildUpdate();
			// return super.onStartCommand(intent, flags, startId); //previous
			// code
			super.onStartCommand(intent, flags, startId);
			return START_STICKY;
		}


		@Override
		public void onHandleIntent(Intent intent) {

			ComponentName me = new ComponentName(this,
					WatcherWidgetProvider.class);
			AppWidgetManager awm = AppWidgetManager.getInstance(this);
			awm.updateAppWidget(me, buildUpdate(this));

		}

		/**
		 * the actual update method where we perform an HTTP request to VirtEx,
		 * read the JSON, and update the text.
		 * 
		 * This displays a notfication if successful and sets the time to green,
		 * otherwise displays failure and sets text to red
		 */
		private RemoteViews buildUpdate(Context context) {

			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.watcher_appwidget);


			Intent intent = new Intent(this, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
					intent, 0);
			views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);

			HttpClient client = new DefaultHttpClient();
			HttpGet post = new HttpGet(
					"https://www.cavirtex.com/api/CAD/ticker.json");
			try {

				HttpResponse response = client.execute(post); // some response
																// object
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								response.getEntity().getContent(), "UTF-8"));
				String text = reader.readLine();
				JSONTokener tokener = new JSONTokener(text);

				NumberFormat numberFormat = DecimalFormat.getInstance();
				numberFormat.setMaximumFractionDigits(2);
				numberFormat.setMinimumFractionDigits(2);

				JSONObject jTicker = new JSONObject(tokener);
				String lastPrice = numberFormat.format(Float.valueOf(jTicker
						.getString("last")));

				views.setTextViewText(R.id.widgetVolText,
						"Volume: " + jTicker.getString("volume"));
				views.setTextViewText(
						R.id.widgetLowText,
						"$"
								+ numberFormat.format(Float.valueOf(jTicker
										.getString("low"))));

				views.setTextViewText(
						R.id.widgetHighText,
						"$"
								+ numberFormat.format(Float.valueOf(jTicker
										.getString("high"))));

				String s = "$" + lastPrice + " CAD";
				views.setTextViewText(R.id.widgetLastText, s);
				SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
				String currentTime = sdf.format(new Date());
				views.setTextViewText(R.id.label, "Refreshed @ " + currentTime);
				views.setTextColor(R.id.label, Color.GREEN);

				if (pref_DisplayUpdates == true) {
					createTicker(context, R.drawable.bitcoin, "VirtEx Updated!");
				}

				if (pref_virtexTicker) {
					createPermanentNotification(getApplicationContext(),
							R.drawable.bitcoin, "Bitcoin at $" + lastPrice
									+ " CAD", "Bitcoin value: $" + lastPrice
									+ " CAD on VirtEx", NOTIFY_ID_VIRTEX);
				}

				try {
					if (pref_PriceAlarm) {

						if (!pref_virtexLower.equalsIgnoreCase("")) {
							if (Float.valueOf(lastPrice) <= Float
									.valueOf(pref_virtexLower)) {
								createNotification(getApplicationContext(),
										R.drawable.bitcoin,
										"Bitcoin alarm value has been reached! \n"
												+ "Bitcoin valued at $"
												+ lastPrice + " CAD on VirtEx",
												"BTC @ $" + lastPrice + " CAD",
										"Bitcoin value: $" + lastPrice
												+ " CAD on VirtEx",
										NOTIFY_ID_VIRTEX);
							}
						}

						if (!pref_virtexUpper.equalsIgnoreCase("")) {
							if (Float.valueOf(lastPrice) >= Float
									.valueOf(pref_virtexUpper)) {
								createNotification(getApplicationContext(),
										R.drawable.bitcoin,
										"Bitcoin alarm value has been reached! \n"
												+ "Bitcoin valued at $"
												+ lastPrice + " CAD on VirtEx",
												"BTC @ $" + lastPrice + " CAD",
										"Bitcoin value: $" + lastPrice
												+ " CAD on VirtEx",
										NOTIFY_ID_VIRTEX);
							}

						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					views.setTextColor(R.id.label, Color.CYAN);
				}

			} catch (Exception e) {
				e.printStackTrace();
				// tv.setText(getStackTrace(e));

				if (pref_DisplayUpdates == true) {
					createTicker(context, R.drawable.bitcoin,
							"VirtEx Update failed!");
				}
				views.setTextColor(R.id.label, Color.RED);

			}

			return views;
		}

		public void widgetButtonAction(Context context) {
			RemoteViews views = new RemoteViews(context.getPackageName(),
					R.layout.watcher_appwidget);

			if (pref_widgetBehaviour.equalsIgnoreCase("mainMenu")) {
				Intent intent = new Intent(this, MainActivity.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, 0, intent, 0);
				views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);

			}

			else if (pref_widgetBehaviour.equalsIgnoreCase("refreshWidget")) {
				Intent intent = new Intent(this, WatcherWidgetProvider.class);
				intent.setAction(REFRESH);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(
						context, 0, intent, 0);
				views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);
			}

			else if (pref_widgetBehaviour.equalsIgnoreCase("openGraph")) {
				// Intent intent = new Intent(getBaseContext(),
				// MainActivity.class);
				// intent.setAction(GRAPH);

				// PendingIntent pendingIntent =
				// PendingIntent.getActivity(context, 0, intent, 0);
				// views.setOnClickPendingIntent(R.id.widgetButton,
				// pendingIntent);

				Intent intent = new Intent(this, MainActivity.class);
				intent.setAction(GRAPH);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(
						context, 0, intent, 0);
				views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);
				// startActivity(intent);

			}

			else if (pref_widgetBehaviour.equalsIgnoreCase("pref")) {
				// context.startService(new Intent(context,
				// UpdateService.class)); //find a way to set to button
				// context.startSe(new Intent(context, UpdateService.class));
				// context.startService(service).set
				Intent intent = new Intent(this, Preferences.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, 0, intent, 0);
				views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);
			}

			else if (pref_widgetBehaviour.equalsIgnoreCase("extOrder")) {
				Intent intent = new Intent(getBaseContext(), WebViewer.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, 0, intent, 0);
				views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent);
			}
		}

	}

}