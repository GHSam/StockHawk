package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by sam on 13/05/16.
 */
public class StockRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Cursor mCursor;
    private Context mContext;

    private static final String[] COLUMNS_PROJECTION = new String[] {
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.CHANGE,
            QuoteColumns.BIDPRICE,
            QuoteColumns.ISUP
    };
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_SYMBOL = 1;
    private static final int COLUMN_CHANGE = 2;
    private static final int COLUMN_BIDPRICE = 3;
    private static final int COLUMN_ISUP = 4;

    public StockRemoteViewsFactory(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }

        mCursor = mContext.getContentResolver().query(
                QuoteProvider.Quotes.CONTENT_URI,
                COLUMNS_PROJECTION,
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{ "1" },
                null
        );
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        mCursor.moveToPosition(i);

        RemoteViews view = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);

        view.setTextViewText(R.id.stock_symbol, mCursor.getString(COLUMN_SYMBOL));
        view.setTextViewText(R.id.bid_price, mCursor.getString(COLUMN_BIDPRICE));
        view.setTextViewText(R.id.change, mCursor.getString(COLUMN_CHANGE));

        int background = R.drawable.percent_change_pill_red;
        if (mCursor.getInt(COLUMN_ISUP) == 1) {
            background = R.drawable.percent_change_pill_green;
        }

        view.setInt(R.id.change, "setBackgroundResource", background);

        Intent intent = new Intent();
        intent.putExtra(QuoteColumns.SYMBOL, mCursor.getString(COLUMN_SYMBOL));
        view.setOnClickFillInIntent(R.id.list_item, intent);

        return view;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        mCursor.moveToPosition(i);

        return mCursor.getLong(COLUMN_ID);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}