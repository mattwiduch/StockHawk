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
package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Defines the columns of a database table.
 *
 * Created by sam_chordas on 10/5/15.
 * Modified by Mateusz Widuch.
 */
public class QuoteColumns {
    @DataType(DataType.Type.INTEGER)
    @PrimaryKey
    @AutoIncrement
    public static final String _ID = "_id";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String SYMBOL = "symbol";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String NAME = "name";

    @DataType(DataType.Type.REAL)
    @NotNull
    public static final String PERCENT_CHANGE = "percent_change";

    @DataType(DataType.Type.REAL)
    @NotNull
    public static final String CHANGE = "change";

    @DataType(DataType.Type.REAL)
    @NotNull
    public static final String BID_PRICE = "bid_price";

    @DataType(DataType.Type.TEXT)
    @NotNull
    public static final String CREATED = "created";

    @DataType(DataType.Type.INTEGER)
    @NotNull
    public static final String IS_UP = "is_up";

    @DataType(DataType.Type.INTEGER)
    @NotNull
    public static final String IS_CURRENT = "is_current";
}
