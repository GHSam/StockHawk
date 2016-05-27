package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.util.Log;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static String fetchData(String url) throws IOException {
    OkHttpClient client = new OkHttpClient();

    Request request = new Request.Builder()
            .url(url)
            .build();

    Response response = client.newCall(request).execute();
    return response.body().string();
  }

  public static ArrayList<Pair<String, Float>> fetchStockHistory(String symbol) {
    ArrayList<Pair<String, Float>> history = new ArrayList<>();

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);

    String startDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
    String endDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

    String query = "select * from yahoo.finance.historicaldata " +
            "where  symbol    = \"" + symbol + "\"" +
            "and    startDate = \"" + startDate + "\"" +
            "and    endDate   = \"" + endDate + "\"";

    String url = Uri.parse("https://query.yahooapis.com/v1/public/yql")
            .buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("diagnostics", "true")
            .appendQueryParameter("env", "store://datatables.org/alltableswithkeys")
            .appendQueryParameter("callback", "")
            .build()
            .toString();

    try {
      JSONObject jsonObject = new JSONObject(fetchData(url));
      JSONArray quotes = jsonObject.getJSONObject("query")
              .getJSONObject("results")
              .getJSONArray("quote");

      for (int i = 0; i < quotes.length(); i++) {
        JSONObject quote = quotes.getJSONObject(i);

        String date = quote.getString("Date");
        Float value = Float.parseFloat(quote.getString("Close"));

        history.add(new Pair<>(date, value));
      }

    } catch (JSONException e) {
      Log.e(LOG_TAG, "Parsing JSON failed: " + e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "Fetching URL failed: " + e);
    }

    return history;
  }

  public static ArrayList quoteJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");

          ContentProviderOperation operation = buildBatchOperation(jsonObject);
          if (operation != null) {
            batchOperations.add(operation);
          }
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);

              ContentProviderOperation operation = buildBatchOperation(jsonObject);
              if (operation != null) {
                batchOperations.add(operation);
              }
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuffer changeBuffer = new StringBuffer(change);
    changeBuffer.insert(0, weight);
    changeBuffer.append(ampersand);
    change = changeBuffer.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
    try {
      ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
              QuoteProvider.Quotes.CONTENT_URI);

      String change = jsonObject.getString("Change");
      String changeInPercent = jsonObject.getString("ChangeinPercent");
      String bid = jsonObject.getString("Bid");

      // Skip invalid stock results
      if (change.equals("null") || changeInPercent.equals("null") || bid.equals("null")) {
        return null;
      }

      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(bid));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(changeInPercent, true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);

      if (change.charAt(0) == '-') {
        builder.withValue(QuoteColumns.ISUP, 0);
      } else {
        builder.withValue(QuoteColumns.ISUP, 1);
      }

      return builder.build();
    } catch (JSONException e) {
      e.printStackTrace();
      return null;
    }
  }
}
