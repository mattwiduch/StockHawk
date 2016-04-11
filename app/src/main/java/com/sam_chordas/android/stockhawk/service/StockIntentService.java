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
package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * Handles asynchronous requests on demand to update stock data.
 *
 * Created by sam_chordas on 10/1/15.
 * Modified by Mateusz Widuch.
 */
public class StockIntentService extends IntentService {
    public static final String TASK_TAG = "task_type";
    public static final String TASK_SYMBOL = "symbol";
    public static final String TASK_TYPE_ADD = "add";
    public static final String TASK_TYPE_INIT = "init";
    public static final String TASK_TYPE_PERIODIC = "periodic";

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        Utils.resetHawkStatus(getApplication());
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra(TASK_TAG).equals(TASK_TYPE_ADD)) {
            args.putString(TASK_SYMBOL, intent.getStringExtra(TASK_SYMBOL));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(TASK_TAG), args));
    }
}
