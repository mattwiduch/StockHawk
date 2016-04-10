package com.sam_chordas.android.stockhawk.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

/**
 * Creates Dialog that allows user to track new stock symbol.
 */
public class DialogTrackStock extends DialogFragment {
    private AlertDialog mDialog;
    private EditText mEditText;
    private TextInputLayout mTextInputLayout;
    private ProgressBar mProgressBar;
    private Button mPositiveButton;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Alert);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View dialogView = inflater.inflate(R.layout.dialog_track_stock, null);
        mEditText = (EditText) dialogView.findViewById(R.id.dialog_track_stock_input);
        mTextInputLayout = (TextInputLayout) dialogView.findViewById(R.id.dialog_track_stock_input_layout);
        mProgressBar = (ProgressBar) dialogView.findViewById(R.id.dialog_track_stock_progress);
        builder.setView(dialogView)
                .setTitle(R.string.dialog_track_title)
                .setNegativeButton(R.string.dialog_button_negative, null)
                .setPositiveButton(R.string.dialog_track_button_positive, null);
        mDialog = builder.create();
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
                        Cursor c = getActivity().getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                new String[]{input}, null);
                        if (c.getCount() != 0) {
                            setErrorMessage(getActivity().getString(R.string.dialog_track_error_symbol_saved));
                            return;
                        } else {
                            // Create intent
                            Intent serviceIntent = new Intent(getActivity(), StockIntentService.class);
                            // Add the stock to DB
                            serviceIntent.putExtra(StockIntentService.TASK_TAG, StockIntentService.TASK_TYPE_ADD);
                            serviceIntent.putExtra(StockIntentService.TASK_SYMBOL, input);
                            getActivity().startService(serviceIntent);
                        }
                        c.close();
                    }
                });
            }
        });
        return mDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            mPositiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
            if (!(mEditText.getText().length() > 0)) mPositiveButton.setEnabled(false);
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
                    if (s.length() > 0 && s.length() <= 8) {
                        mPositiveButton.setEnabled(true);
                    } else {
                        mPositiveButton.setEnabled(false);
                    }
                }
            });
        }
    }

    private String getText() {
        return mEditText.getText().toString();
    }

    public void setErrorMessage(String errorMessage) {
        mProgressBar.setVisibility(View.GONE);
        mTextInputLayout.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake));
        mTextInputLayout.setError(errorMessage);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mTextInputLayout.setError(null);
    }
}
