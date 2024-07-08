package com.cassens.autotran.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

public class LoggingCursorFactory implements SQLiteDatabase.CursorFactory {
        private boolean debugQueries = false;

        public LoggingCursorFactory() {
            this.debugQueries = false;
        }

        public LoggingCursorFactory(boolean debugQueries) {
            this.debugQueries = debugQueries;
        }

        @Override
        public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery,
                                String editTable, SQLiteQuery query) {
            if (debugQueries) {
                Log.d("SQL", query.toString());
            }
            return new SQLiteCursor(masterQuery, editTable, query);
        }
}
