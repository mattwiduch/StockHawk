package com.sam_chordas.android.stockhawk.widget;
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * RemoteViewsService controlling the data being shown in the scrollable today's market widget
 */
public class MarketWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplication());
                String sortOrder = sp.getString(getResources().getString(R.string.pref_sort_order),
                        "null");
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.BID_PRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.IS_UP},
                        QuoteColumns.IS_CURRENT + " = ?",
                        new String[]{"1"},
                        sortOrder);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_market_item);

                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));
                String price = Utils.formatBidPrice(getApplication(),
                        data.getString(data.getColumnIndex(QuoteColumns.BID_PRICE)));
                String change = Utils.formatChangeInPercent(getApplication(),
                        data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
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
                views.setTextColor(R.id.widget_change, ContextCompat.getColor(getApplication(), color));
                views.setContentDescription(R.id.widget_change, getString(R.string.a11y_change, change));

                // Add fill intent to item view
                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(getString(R.string.line_graph_extra), symbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_market_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
