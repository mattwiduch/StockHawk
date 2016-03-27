package com.sam_chordas.android.stockhawk.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates configuration activity for quote widget.
 */
public class QuoteWidgetConfigurationActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private int mSymbolPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_quote_config);
        setResult(RESULT_CANCELED);

        int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            Cursor data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME, QuoteColumns.IS_CURRENT},
                    QuoteColumns.IS_CURRENT + " = ?",
                    new String[]{"1"},
                    QuoteColumns.NAME + " ASC");

            final List<String> symbols = new ArrayList<>();
            final List<String> names = new ArrayList<>();
            if (data != null && data.getCount() != 0) {
                while (data.moveToNext()) {
                    symbols.add(data.getString(data.getColumnIndex(QuoteColumns.SYMBOL)));
                    names.add(data.getString(data.getColumnIndex(QuoteColumns.NAME)));
                }
                data.close();
            } else {
                Toast.makeText(QuoteWidgetConfigurationActivity.this, R.string.empty_stocks_list,
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            Spinner symbolsSpinner = (Spinner) findViewById(R.id.symbols_spinner);
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            symbolsSpinner.setAdapter(spinnerAdapter);
            symbolsSpinner.setOnItemSelectedListener(this);

            final int finalWidgetId = widgetId;
            TextView okButton = (TextView) findViewById(R.id.add_button);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor prefs = PreferenceManager.
                            getDefaultSharedPreferences(QuoteWidgetConfigurationActivity.this).edit();
                    prefs.putString(finalWidgetId + "", symbols.get(mSymbolPosition));
                    prefs.apply();

                    Intent startService = new Intent(QuoteWidgetConfigurationActivity.this,
                            QuoteWidgetIntentService.class);
                    startService.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, finalWidgetId);
                    startService.setAction("com.sam_chordas.android.stockhawk.APPWIDGET_UPDATE");
                    setResult(RESULT_OK, startService);
                    startService(startService);

                    finish();
                }
            });

            TextView cancelButton = (TextView) findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mSymbolPosition = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mSymbolPosition = 0;
    }
}
