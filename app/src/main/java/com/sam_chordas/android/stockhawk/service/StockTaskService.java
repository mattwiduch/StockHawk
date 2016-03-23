package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Instant;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private static String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private static Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    // Names of the JSON objects that need to be extracted
    private static final String YFQ_COUNT = "count";
    private static final String YFQ_QUERY = "query";
    private static final String YFQ_QUOTE = "quote";
    private static final String YFQ_RESULTS = "results";
    private static final String YFQ_STOCK_SYMBOL = "symbol";
    private static final String YFQ_STOCK_NAME = "Name";
    private static final String YFQ_STOCK_BID = "Bid";
    private static final String YFQ_STOCK_CHANGE = "Change";
    private static final String YFQ_STOCK_CHANGE_IN_PERCENT = "ChangeinPercent";
    private static final String YFQ_DATA_NOT_AVAILABLE = "null";
    
    // Define Error States
    public static final int HAWK_STATUS_OK = 100;
    public static final int HAWK_STATUS_SERVER_DOWN = 101;
    public static final int HAWK_STATUS_SERVER_INVALID = 102;
    public static final int HAWK_STATUS_DATA_CORRUPTED = 103;
    public static final int HAWK_STATUS_SYMBOL_INVALID = 104;
    public static final int HAWK_STATUS_UTF8_NOT_SUPPORTED = 105;
    public static final int HAWK_STATUS_UNKNOWN = 106;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HAWK_STATUS_OK, HAWK_STATUS_SERVER_DOWN, HAWK_STATUS_SERVER_INVALID, HAWK_STATUS_DATA_CORRUPTED,
            HAWK_STATUS_SYMBOL_INVALID, HAWK_STATUS_UTF8_NOT_SUPPORTED, HAWK_STATUS_UNKNOWN})
    public @interface HawkStatus {
    }

    public StockTaskService() {}

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("SELECT * FROM yahoo.finance.quotes WHERE symbol "
                    + "IN (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            setHawkStatus(HAWK_STATUS_UTF8_NOT_SUPPORTED);
            e.printStackTrace();
        }
        if (params.getTag().equals(StockIntentService.TASK_TYPE_INIT)
                || params.getTag().equals(StockIntentService.TASK_TYPE_PERIODIC)) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"DISTINCT " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    setHawkStatus(HAWK_STATUS_UTF8_NOT_SUPPORTED);
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex(YFQ_STOCK_SYMBOL)) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    setHawkStatus(HAWK_STATUS_UTF8_NOT_SUPPORTED);
                    e.printStackTrace();
                }
                initQueryCursor.close();
            }
        } else if (params.getTag().equals(StockIntentService.TASK_TYPE_ADD)) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString(YFQ_STOCK_SYMBOL);
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                setHawkStatus(HAWK_STATUS_UTF8_NOT_SUPPORTED);
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update IS_CURRENT to 0 (false) so new data is current
                    if (isUpdate) {
                        contentValues.put(QuoteColumns.IS_CURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            quoteJsonToContentVals(getResponse));
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                    setHawkStatus(HAWK_STATUS_DATA_CORRUPTED);
                }
            } catch (IOException e) {
                setHawkStatus(HAWK_STATUS_SERVER_DOWN);
                e.printStackTrace();
            }
        }

        return result;
    }

    private static ArrayList quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        Log.i(LOG_TAG, "GET FB: " + JSON);
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(YFQ_QUERY);
                int count = Integer.parseInt(jsonObject.getString(YFQ_COUNT));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(YFQ_RESULTS)
                            .getJSONObject(YFQ_QUOTE);
                    if (!jsonObject.getString(YFQ_STOCK_NAME).equals(YFQ_DATA_NOT_AVAILABLE)) {
                        batchOperations.add(buildBatchOperation(jsonObject));
                    } else {
                        setHawkStatus(HAWK_STATUS_SYMBOL_INVALID);
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject(YFQ_RESULTS).getJSONArray(YFQ_QUOTE);

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            if (!jsonObject.getString(YFQ_STOCK_NAME).equals(YFQ_DATA_NOT_AVAILABLE)) {
                                batchOperations.add(buildBatchOperation(jsonObject));
                            } else {
                                setHawkStatus(HAWK_STATUS_SYMBOL_INVALID);
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
            setHawkStatus(HAWK_STATUS_SERVER_INVALID);
        }

        if (!batchOperations.isEmpty()) setHawkStatus(HAWK_STATUS_OK);
        return batchOperations;
    }

    private static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(YFQ_STOCK_SYMBOL));
            builder.withValue(QuoteColumns.NAME, jsonObject.getString(YFQ_STOCK_NAME));
            String bidPrice = jsonObject.getString(YFQ_STOCK_BID).equals(YFQ_DATA_NOT_AVAILABLE)
                    ? mContext.getString(R.string.data_not_available)
                    : Utils.truncateBidPrice(jsonObject.getString(YFQ_STOCK_BID));
            builder.withValue(QuoteColumns.BID_PRICE, bidPrice);
            String change = jsonObject.getString(YFQ_STOCK_CHANGE).equals(YFQ_DATA_NOT_AVAILABLE)
                    ? mContext.getString(R.string.data_not_available)
                    : Utils.truncateChange(jsonObject.getString(YFQ_STOCK_CHANGE), false);
            builder.withValue(QuoteColumns.CHANGE, change);
            String percentChange = jsonObject.getString(YFQ_STOCK_CHANGE_IN_PERCENT).equals(YFQ_DATA_NOT_AVAILABLE)
                    ? mContext.getString(R.string.data_not_available)
                    : Utils.truncateChange(jsonObject.getString(YFQ_STOCK_CHANGE_IN_PERCENT), true);
            builder.withValue(QuoteColumns.PERCENT_CHANGE, percentChange);
            builder.withValue(QuoteColumns.CREATED, Instant.now().toString());
            builder.withValue(QuoteColumns.IS_CURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.IS_UP, 0);
            } else {
                builder.withValue(QuoteColumns.IS_UP, 1);
            }

        } catch (JSONException e) {
            setHawkStatus(HAWK_STATUS_SERVER_INVALID);
            e.printStackTrace();
        }
        return builder.build();
    }

    /**
     * Sets application status into shared preference.
     *
     * @param hawkStatus The IntDef value to set
     */
    private static void setHawkStatus(@HawkStatus int hawkStatus){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(mContext.getString(R.string.pref_hawk_status_key), hawkStatus);
        spe.apply();
    }
}
