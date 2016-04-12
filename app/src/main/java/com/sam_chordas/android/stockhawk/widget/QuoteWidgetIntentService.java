/*
 * Copyright (C) 2016 Mateusz Widuch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sam_chordas.android.stockhawk.widget;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Handles updating all Quote widgets with the latest data.
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

        // Get handle to shared prefs so we can retrieve widget's stock symbol
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        // Perform this loop procedure for each Today widget
        for (int appWidgetId : appWidgetIds) {
            int layoutId = R.layout.widget_quote;
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);

            // Load data from the database
            String symbol = sp.getString(appWidgetId + "", getString(R.string.widget_default_symbol));
            Cursor data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BID_PRICE,
                            QuoteColumns.CHANGE, QuoteColumns.PERCENT_CHANGE, QuoteColumns.IS_UP, QuoteColumns.IS_CURRENT},
                    QuoteColumns.SYMBOL + " = ? AND " + QuoteColumns.IS_CURRENT + " = ?",
                    new String[]{symbol, "1"},
                    null);

            if (data == null) {
                return;
            }
            if (!data.moveToFirst()) {
                views.setTextViewText(R.id.widget_stock_symbol, getString(R.string.widget_quote_empty_label));
                views.setTextViewText(R.id.widget_stock_name, getString(R.string.widget_quote_empty, symbol));
                views.setImageViewResource(R.id.widget_icon, -1);
                views.setTextViewText(R.id.widget_change, "");
                views.setTextViewText(R.id.widget_bid_price, "");
                launchMainActivity(views);
                appWidgetManager.updateAppWidget(appWidgetId, views);
                data.close();
                return;
            }

            //String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
            String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));
            String price = Utils.formatBidPrice(this, data.getDouble(data.getColumnIndex(QuoteColumns.BID_PRICE)));
            String change = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_widget_units_key), true)
                    ? Utils.formatChangeInPercent(this, data.getDouble(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE)))
                    : Utils.formatChange(this, data.getDouble(data.getColumnIndex(QuoteColumns.CHANGE)));
            int isUp = data.getInt(data.getColumnIndex(QuoteColumns.IS_UP));

            // Get correct color & icon
            int color = R.color.blue_flat;
            int icon = R.drawable.ic_trending_flat_18dp;
            String trending = getString(R.string.a11y_trending_flat);
            if (isUp == -1) {
                icon = R.drawable.ic_trending_down_18dp;
                color = R.color.red_low;
                trending = getString(R.string.a11y_trending_down);
            } else if (isUp == 1) {
                icon = R.drawable.ic_trending_up_18dp;
                color = R.color.green_high;
                trending = getString(R.string.a11y_trending_up);
            }

            // Add the data to the RemoteViews
            views.setImageViewResource(R.id.widget_icon, icon);
            views.setTextViewText(R.id.widget_stock_symbol, symbol);
            views.setContentDescription(R.id.widget_stock_symbol, name);
            views.setTextViewText(R.id.widget_stock_name, name);
            views.setContentDescription(R.id.widget_stock_name, trending);
            views.setTextViewText(R.id.widget_bid_price, price);
            views.setContentDescription(R.id.widget_bid_price, getString(R.string.a11y_price, price));
            views.setTextViewText(R.id.widget_change, change);
            views.setTextColor(R.id.widget_change, ContextCompat.getColor(this, color));
            views.setContentDescription(R.id.widget_change, getString(R.string.a11y_change, change));

            // Create an Intent to launch MainActivity
            launchMainActivity(views);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

            // Close cursor
            data.close();
        }
    }

    private void launchMainActivity(RemoteViews views) {
        Intent launchIntent = new Intent(this, MyStocksActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_large, pendingIntent);
    }
}
