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
package com.sam_chordas.android.stockhawk.touch_helper;

/**
 * Enables swipe to delete on RecyclerView's items.
 * <p/>
 * Created by sam_chordas on 10/6/15.
 * Credit to Paul Burke (ipaulpro)
 */
public interface ItemTouchHelperAdapter {
    /**
     * Called when an item is dismissed in the RecyclerView.
     */
    void onItemDismiss(int position);
}
