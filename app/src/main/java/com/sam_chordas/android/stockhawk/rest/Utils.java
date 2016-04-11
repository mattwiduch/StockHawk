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
package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.threeten.bp.Instant;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Provides various utility methods.
 */
public class Utils {
    /**
     * Returns true if the network is available or about to become available.
     *
     * @param c Context to use for resource localization
     * @return true if the network is available
     */
    public static boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Retrieves application status from shared preference.
     *
     * @param context Context to use for resource localization
     * @return Hawk status integer type
     */
    @SuppressWarnings("ResourceType")
    static public
    @StockTaskService.HawkStatus
    int getHawkStatus(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(context.getString(R.string.pref_hawk_status_key), StockTaskService.HAWK_STATUS_UNKNOWN);
    }

    /**
     * Resets stock hawk status to HAWK_STATUS_UNKNOWN.
     *
     * @param context Context to use for resource localization
     */
    static public void resetHawkStatus(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(context.getString(R.string.pref_hawk_status_key), StockTaskService.HAWK_STATUS_UNKNOWN);
        spe.apply();
    }

    /**
     * Formats dates that will be displayed as graph labels.
     *
     * @param date String representation of date in ISO-8601 format
     * @return Date in the form of a string formatted as DD:MM in device's locale
     */
    static public String formatGraphDateLabels(String date) {
        String currentDate = Instant.now().toString();
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT);
        // Removes year from formatted date
        String pattern = df.toLocalizedPattern().replaceAll(".?[Yy].?", "");
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        String formattedDate;

        if (currentDate.substring(0, 9).equals(date.substring(0, 9))) {
            if (Integer.parseInt(currentDate.substring(9, 10)) == Integer.parseInt(date.substring(9, 10))
                    || Integer.parseInt(currentDate.substring(9, 10)) - 1 == Integer.parseInt(date.substring(9, 10))) {
                df = new SimpleDateFormat("HH:mm", Locale.getDefault());
                formattedDate = df.format(new Date(Instant.parse(date).toEpochMilli()));
            } else {

                formattedDate = sdf.format(new Date(Instant.parse(date).toEpochMilli()));
            }
        } else {
            formattedDate = sdf.format(new Date(Instant.parse(date).toEpochMilli()));
        }

        return formattedDate;
    }

    /**
     * Returns relative time since update time.
     *
     * @param context Context to use for resource localization
     * @param updateTime String representation of date in ISO-8601 format
     * @return Relative time since last update in the form of a string formatted as "5 minutes ago"
     */
    static public String formatLastUpdateTime(Context context, String updateTime) {
        String formattedUpdateTime = DateUtils.getRelativeTimeSpanString(Instant.parse(updateTime).toEpochMilli(),
                Instant.now().toEpochMilli(), DateUtils.MINUTE_IN_MILLIS).toString();
        return formattedUpdateTime.charAt(0) == '0' ? context.getString(R.string.last_updated_minute)
                : formattedUpdateTime;
    }

    /**
     * Formats stock bid price for device's locale.
     *
     * @param context Context to use for resource localization
     * @param bidPrice Bid price value
     * @return Bid price in the form of a string formatted in device's locale
     */
    static public String formatBidPrice(Context context, double bidPrice) {
        if (bidPrice != Double.MIN_VALUE) {
            DecimalFormat decimalFormat = new DecimalFormat("'$'#.00");
            return decimalFormat.format(bidPrice);
        } else {
            return context.getString(R.string.data_not_available_label);
        }
    }

    /**
     * Formats stock change for device's locale.
     *
     * @param context Context to use for resource localization
     * @param change Change value
     * @return Change in the form of a string formatted in device's locale
     */
    static public String formatChange(Context context, double change) {
        if (change != Double.MIN_VALUE) {
            DecimalFormat decimalFormat = new DecimalFormat("+#0.00;-#");
            return decimalFormat.format(change);
        } else {
            return context.getString(R.string.data_not_available_label);
        }
    }

    /**
     * Formats stock change in percent for device's locale.
     *
     * @param context Context to use for resource localization
     * @param changeInPercent Change in percent value
     * @return Change in percent in the form of a string formatted in device's locale
     */
    static public String formatChangeInPercent(Context context, double changeInPercent) {
        if (changeInPercent != Double.MIN_VALUE) {
            DecimalFormat decimalFormat = new DecimalFormat("+#0.00%;-#%");
            decimalFormat.setMultiplier(1);
            return decimalFormat.format(changeInPercent);
        } else {
            return context.getString(R.string.data_not_available_label);
        }
    }
}
