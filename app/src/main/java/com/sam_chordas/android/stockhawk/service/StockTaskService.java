package com.sam_chordas.android.stockhawk.service;

import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
  private String LOG_TAG = StockTaskService.class.getSimpleName();

  private Context mContext;

  public StockTaskService(){}

  public StockTaskService(Context context){
    mContext = context;
  }


  private String getQuotesUrl(List<String> symbols) {
    String query = "select * from yahoo.finance.quotes " +
            "where symbol in (\"" + TextUtils.join("\",\"", symbols) + "\")";

    // Base URL for the Yahoo query
    return Uri.parse("https://query.yahooapis.com/v1/public/yql")
            .buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("diagnostics", "true")
            .appendQueryParameter("env", "store://datatables.org/alltableswithkeys")
            .appendQueryParameter("callback", "")
            .build()
            .toString();
  }

  @Override
  public int onRunTask(TaskParams params){
    if (mContext == null) {
      mContext = this;
    }

    boolean isUpdate;
    ArrayList<String> symbols = new ArrayList<>();

    if (params.getTag().equals("init") || params.getTag().equals("periodic")){
      isUpdate = true;
      Cursor initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
              new String[] { "Distinct " + QuoteColumns.SYMBOL }, null, null, null);

      if (initQueryCursor == null || initQueryCursor.getCount() == 0){
        // Init task. Populates DB with quotes for the symbols seen below
        symbols.addAll(Arrays.asList("YHOO", "AAPL", "GOOG", "MSFT"));
      } else {
        DatabaseUtils.dumpCursor(initQueryCursor);

        initQueryCursor.moveToFirst();

        do {
          symbols.add(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")));
        } while (initQueryCursor.moveToNext());
      }
    } else if (params.getTag().equals("add")){
      isUpdate = false;
      // get symbol from params.getExtra and build query
      symbols.add(params.getExtras().getString("symbol"));
    } else {
      return GcmNetworkManager.RESULT_FAILURE;
    }

    try {
      String getResponse = Utils.fetchData(getQuotesUrl(symbols));

      try {
        // update ISCURRENT to 0 (false) so new data is current
        if (isUpdate){
          ContentValues contentValues = new ContentValues();
          contentValues.put(QuoteColumns.ISCURRENT, 0);

          mContext.getContentResolver()
                  .update(QuoteProvider.Quotes.CONTENT_URI, contentValues, null, null);
        }

        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                Utils.quoteJsonToContentVals(getResponse));

        // Notify widgets of updated data
        mContext.sendBroadcast(new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE));

        return GcmNetworkManager.RESULT_SUCCESS;
      } catch (RemoteException | OperationApplicationException e) {
        Log.e(LOG_TAG, "Error applying batch insert", e);
      }
    } catch (IOException e){
      e.printStackTrace();
    }

    return GcmNetworkManager.RESULT_FAILURE;
  }
}
