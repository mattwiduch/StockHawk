package com.sam_chordas.android.stockhawk.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by frano on 27/03/2016.
 */
public class QuoteWidgetIntentService extends IntentService {
    public QuoteWidgetIntentService() {
        super("QuoteWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Retrieve all of the Today widget ids: these are the widgets we need to update
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this,
                QuoteWidgetProvider.class));

        int trendingIcon = R.drawable.ic_trending_up_white_18dp;
        String symbol = "APPL";
        String name = "Apple Inc.";
        String price = "98.20";
        String change = "+3.14%";

        // Perform this loop procedure for each Today widget
        for (int appWidgetId : appWidgetIds) {
            int layoutId = R.layout.widget_large;
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Add the data to the RemoteViews
            views.setImageViewResource(R.id.widget_icon, trendingIcon);
            views.setTextViewText(R.id.widget_stock_symbol, symbol);
            views.setTextViewText(R.id.widget_stock_name, name);
            views.setTextViewText(R.id.widget_bid_price, price);
            views.setTextViewText(R.id.widget_change, change);
            views.setContentDescription(R.id.widget_icon, symbol);

            // Create an Intent to launch MainActivity
            Intent launchIntent = new Intent(this, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_large, pendingIntent);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
