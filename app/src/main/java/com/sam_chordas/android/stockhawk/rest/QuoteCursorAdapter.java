package com.sam_chordas.android.stockhawk.rest;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperAdapter;
import com.sam_chordas.android.stockhawk.touch_helper.ItemTouchHelperViewHolder;

/**
 * Created by sam_chordas on 10/6/15.
 * Credit to skyfishjy gist:
 * https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the code structure
 */
public class QuoteCursorAdapter extends CursorRecyclerViewAdapter<QuoteCursorAdapter.ViewHolder>
        implements ItemTouchHelperAdapter {
    private static final int FOOTER_VIEW = 1;
    private static Context mContext;
    private static Typeface robotoLight;
    //private final OnStartDragListener mDragListener;
    private boolean isPercent;
    private int mFocusedItem;
    private static int mSelectedItem;

    public QuoteCursorAdapter(Context context, Cursor cursor, View emptyView) {
        super(context, cursor, emptyView);
        //mDragListener = dragListener;
        mFocusedItem = -1;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        robotoLight = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Light.ttf");

        if (viewType == FOOTER_VIEW) {
            View footerView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_footer, parent, false);
            FooterViewHolder vh = new FooterViewHolder(footerView);
            // Prevent footer from being selected
            vh.itemView.setEnabled(false);
            return vh;
        }
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_quote, parent, false);
        return new ListItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if (viewHolder instanceof ListItemViewHolder) {
            super.onBindViewHolder(viewHolder, position);
            // Request focus
            if (mFocusedItem == position) {
                viewHolder.itemView.requestFocus();
            }
        }
        if (viewHolder instanceof FooterViewHolder) {
            // Request focus
            if (mFocusedItem == position) {
                viewHolder.itemView.requestFocus();
            }
            // Skips footer view during talk back
            ViewCompat.setImportantForAccessibility(
                    ((FooterViewHolder) viewHolder).itemView,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final Cursor cursor) {
        // TODO: Move to utils
        @ColorInt
        int color;
        @DrawableRes
        Drawable icon;
        String trending;
        if (cursor.getInt(cursor.getColumnIndex(QuoteColumns.IS_UP)) == -1) {
            icon = ContextCompat.getDrawable(mContext, R.drawable.ic_trending_down_white_18dp);
            color = ContextCompat.getColor(mContext, R.color.red_low);
            trending = mContext.getString(R.string.a11y_trending_down);
        } else if (cursor.getInt(cursor.getColumnIndex(QuoteColumns.IS_UP)) == 0) {
            icon = ContextCompat.getDrawable(mContext, R.drawable.ic_trending_flat_white_18dp);
            color = ContextCompat.getColor(mContext, R.color.blue_flat);
            trending = mContext.getString(R.string.a11y_trending_flat);
        } else {
            icon = ContextCompat.getDrawable(mContext, R.drawable.ic_trending_up_white_18dp);
            color = ContextCompat.getColor(mContext, R.color.green_high);
            trending = mContext.getString(R.string.a11y_trending_up);
        }

        viewHolder.icon.setImageDrawable(icon);
        viewHolder.icon.setColorFilter(color);
        viewHolder.symbol.setText(cursor.getString(cursor.getColumnIndex(QuoteColumns.SYMBOL)));
        String name = cursor.getString(cursor.getColumnIndex(QuoteColumns.NAME));
        viewHolder.name.setText(name);
        viewHolder.symbol.setContentDescription(name);
        viewHolder.name.setContentDescription(trending);
        String bidPrice = Utils.formatBidPrice(mContext, cursor.getDouble(cursor.getColumnIndex(QuoteColumns.BID_PRICE)));
        viewHolder.bidPrice.setText(bidPrice);
        viewHolder.bidPrice.setContentDescription(mContext.getString(R.string.a11y_price, bidPrice));

        viewHolder.change.setTextColor(color);
        String change;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (sp.getBoolean(mContext.getString(R.string.pref_units_key), true)) {
            change = Utils.formatChangeInPercent(mContext, cursor.getDouble(cursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE)));
            viewHolder.change.setText(change);
        } else {
            change = Utils.formatChange(mContext, cursor.getDouble(cursor.getColumnIndex(QuoteColumns.CHANGE)));
            viewHolder.change.setText(change);
        }
        viewHolder.change.setContentDescription(mContext.getString(R.string.a11y_change,
                change));
    }

    /**
     * Sets position of currently focused item
     */
    public void setFocusedItem(int position) {
        mFocusedItem = position;
    }

    public int getSelectedItem() { return mSelectedItem; }

    @Override
    public void onItemDismiss(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        String symbol = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
        mContext.getContentResolver().delete(QuoteProvider.Quotes.withSymbol(symbol), null, null);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        if (mCursor.getCount() == 0) {
            // Nothing to show
            return 0;
        }
        // Add extra view to show the footer view
        return mCursor.getCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mCursor.getCount()) {
            return FOOTER_VIEW;
        }
        return super.getItemViewType(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements ItemTouchHelperViewHolder, View.OnClickListener {
        public final ImageView icon;
        public final TextView symbol;
        public final TextView name;
        public final TextView bidPrice;
        public final TextView change;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.list_item_icon);
            symbol = (TextView) itemView.findViewById(R.id.stock_symbol);
            name = (TextView) itemView.findViewById(R.id.stock_name);
            bidPrice = (TextView) itemView.findViewById(R.id.bid_price);
            change = (TextView) itemView.findViewById(R.id.change);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.control_highlight));
            mSelectedItem = this.getAdapterPosition();
        }

        @Override
        public void onItemClear() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                // If we're running on Honeycomb or newer, then we can use the Theme's
                // selectableItemBackground to ensure that the View has a pressed state
                TypedValue outValue = new TypedValue();
                mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                itemView.setBackgroundResource(outValue.resourceId);
            }
        }

        @Override
        public void onClick(View v) {

        }
    }

    // Footer
    public class FooterViewHolder extends ViewHolder {
        public FooterViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onItemSelected() {
        }
    }

    // List Item
    public class ListItemViewHolder extends ViewHolder {
        public ListItemViewHolder(View itemView) {
            super(itemView);
            symbol.setTypeface(robotoLight);
            // Emulates item touch when touch pad center button is pressed
            itemView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                        RecyclerView rv = (RecyclerView) v.getParent();
                        int[] touchCoordinates = new int[2];
                        // Get view's coordinates
                        v.getLocationInWindow(touchCoordinates);
                        // Translate to get coordinates within the view
                        touchCoordinates[0] = touchCoordinates[0] + 21;
                        touchCoordinates[1] = touchCoordinates[1] - 165;

                        // Fire touch down event
                        MotionEvent e = MotionEvent.obtain(SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_DOWN,
                                touchCoordinates[0], touchCoordinates[1], 0);
                        rv.dispatchTouchEvent(e);
                        // Fire touch up event
                        e = MotionEvent.obtain(SystemClock.uptimeMillis(),
                                SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_UP,
                                touchCoordinates[0], touchCoordinates[1], 0);
                        rv = (RecyclerView) v.getParent();
                        rv.dispatchTouchEvent(e);

                        return true;
                    }
                    return false;
                }
            });
        }
    }
}
