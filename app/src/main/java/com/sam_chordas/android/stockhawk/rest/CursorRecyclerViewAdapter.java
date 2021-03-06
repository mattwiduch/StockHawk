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

import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;

/**
 * Provides base functionality for adapter that exposes Cursor data to RecyclerView.
 * <p/>
 * Created by sam_chordas on 10/6/15.
 * Modified by Mateusz Widuch.
 * Credit to skyfishjy gist:
 * https://gist.github.com/skyfishjy/443b7448f59be978bc59
 * for the CursorRecyclerViewAdapter.java code and idea.
 */
public abstract class CursorRecyclerViewAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected Cursor mCursor;
    private boolean dataIsValid;
    private int rowIdColumn;
    private DataSetObserver mDataSetObserver;
    private View mEmptyView;

    public CursorRecyclerViewAdapter(Cursor cursor, View emptyView) {
        mCursor = cursor;
        dataIsValid = cursor != null;
        rowIdColumn = dataIsValid ? mCursor.getColumnIndex(QuoteColumns._ID) : -1;
        mDataSetObserver = new NotifyingDataSetObserver();
        if (dataIsValid) {
            mCursor.registerDataSetObserver(mDataSetObserver);
        }

        mEmptyView = emptyView;
        //mEmptyView.setVisibility(View.GONE);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public int getItemCount() {
        if (dataIsValid && mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        if (dataIsValid && mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(rowIdColumn);
        }
        return 0;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public abstract void onBindViewHolder(VH viewHolder, Cursor cursor);

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        if (!dataIsValid) {
            throw new IllegalStateException("This should only be called when Cursor is valid");
        }
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Could not move Cursor to position: " + position);
        }
        onBindViewHolder(viewHolder, mCursor);
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        if (oldCursor != null && mDataSetObserver != null) {
            oldCursor.unregisterDataSetObserver(mDataSetObserver);
        }
        mCursor = newCursor;
        if (mCursor != null) {
            if (mDataSetObserver != null) {
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
            rowIdColumn = newCursor.getColumnIndexOrThrow(QuoteColumns._ID);
            dataIsValid = true;
            notifyDataSetChanged();
        } else {
            rowIdColumn = -1;
            dataIsValid = false;
            notifyDataSetChanged();
        }
        // TODO: Find out why empty view shows between database updates
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);

        return oldCursor;
    }

    private class NotifyingDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            dataIsValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            dataIsValid = false;
            notifyDataSetChanged();
        }
    }
}
