package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

/**
 * Created by frano on 12/03/2016.
 */
public class TrackStockDialog {
    private Context mContext;
    private AlertDialog.Builder mBuilder;
    private AlertDialog mDialog;
    private EditText mEditText;
    private TextInputLayout mTextInputLayout;
    private ProgressBar mProgressBar;

    public TrackStockDialog(Context context) {
        super();
        mContext = context;
        mBuilder = new AlertDialog.Builder(context, R.style.DialogTrackStock);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View dialogView = inflater.inflate(R.layout.dialog_track_stock, null);
        mEditText = (EditText) dialogView.findViewById(R.id.dialog_track_stock_input);
        mTextInputLayout = (TextInputLayout) dialogView.findViewById(R.id.dialog_track_stock_input_layout);
        mProgressBar = (ProgressBar) dialogView.findViewById(R.id.dialog_track_stock_progress);
        mBuilder.setView(dialogView)
                .setTitle(R.string.dialog_track_title)
                .setPositiveButton(R.string.dialog_track_button_positive, null);
        mDialog = mBuilder.create();
        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        String input = getText().toUpperCase();
                        // User clicked OK button
                        Cursor c = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                new String[]{input}, null);
                        if (c.getCount() != 0) {
                            setErrorMessage(mContext.getString(R.string.error_symbol_saved));
                            return;
                        } else {
                            // Create intent
                            Intent serviceIntent = new Intent(mContext, StockIntentService.class);
                            // Add the stock to DB
                            serviceIntent.putExtra("tag", "add");
                            serviceIntent.putExtra("symbol", input);
                            mContext.startService(serviceIntent);
                        }
                        c.close();
                    }
                });
            }
        });
    }

    public void show() {
        mDialog.show();
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTextInputLayout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                int count = s.length();
                if (count > 0 && count < 8) {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    public void setErrorMessage(String errorMessage) {
        mProgressBar.setVisibility(View.GONE);
        mTextInputLayout.setError(errorMessage);
    }

    public void dismiss() {
        mDialog.dismiss();
    }
}
