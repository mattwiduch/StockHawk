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
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {
    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format(Locale.ENGLISH, "%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format(Locale.ENGLISH, "%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    /**
     * Returns true if the network is available or about to become available.
     *
     * @param c Context used to get the ConnectivityManager
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
     * @param context Context used to get the SharedPreferences
     * @return hawk status integer type
     */
    @SuppressWarnings("ResourceType")
    static public
    @StockTaskService.HawkStatus
    int getHawkStatus(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(context.getString(R.string.pref_hawk_status_key), StockTaskService.HAWK_STATUS_UNKNOWN);
    }

    /**
     * Resets stock hawk status to HAWK_STATUS_UNKNOWN
     *
     * @param context Context used to get the SharedPreferences
     */
    static public void resetHawkStatus(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(context.getString(R.string.pref_hawk_status_key), StockTaskService.HAWK_STATUS_UNKNOWN);
        spe.apply();
    }

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
     * Returns relative time since update time
     */
    static public String formatLastUpdateTime(Context context, String updateTime) {
        String formattedUpdateTime = DateUtils.getRelativeTimeSpanString(Instant.parse(updateTime).toEpochMilli(),
                Instant.now().toEpochMilli(), DateUtils.MINUTE_IN_MILLIS).toString();
        return formattedUpdateTime.charAt(0) == '0' ? context.getString(R.string.last_updated_minute)
                : formattedUpdateTime;
    }

    /**
     * Formats stock bid price for device's locale
     */
    static public String formatBidPrice(Context context, String bidPrice) {
        if (!bidPrice.equals(context.getResources().getString(R.string.data_not_available))) {
            DecimalFormat decimalFormat = new DecimalFormat("'$'#.00");
            bidPrice = decimalFormat.format(Double.parseDouble(bidPrice));
        } else {
            bidPrice = context.getString(R.string.data_not_available_label);
        }
        return bidPrice;
    }

    /**
     * Formats stock change for device's locale
     */
    static public String formatChange(Context context, String change) {
        if (!change.equals(context.getResources().getString(R.string.data_not_available))) {
            DecimalFormat decimalFormat = new DecimalFormat("+#0.00;-#");
            change = decimalFormat.format(Double.parseDouble(change));
        } else {
            context.getString(R.string.data_not_available_label);
        }
        return change;
    }

    /**
     * Formats stock change in percent for device's locale
     */
    static public String formatChangeInPercent(Context context, Double changeInPercent) {
        if (changeInPercent != Double.MIN_VALUE) {
            DecimalFormat decimalFormat = new DecimalFormat("+#0.00%;-#%");
            decimalFormat.setMultiplier(1);
            return decimalFormat.format(changeInPercent);
        } else {
            return context.getString(R.string.data_not_available_label);
        }
    }
}
