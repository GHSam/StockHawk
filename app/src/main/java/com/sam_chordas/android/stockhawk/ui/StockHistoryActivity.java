package com.sam_chordas.android.stockhawk.ui;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.Theme;
import com.db.chart.model.ChartEntry;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.util.ArrayList;


public class StockHistoryActivity extends AppCompatActivity {
    private class FetchStockHistory extends AsyncTask<String, Void, LineSet> {
        @Override
        protected LineSet doInBackground(String... strings) {
            String symbol = strings[0];

            ArrayList<Pair<String, Float>> history = Utils.fetchStockHistory(symbol);
            LineSet dataset = new LineSet();

            for (Pair<String, Float> stock : history) {
                dataset.addPoint(stock.first, stock.second);
            }

            return dataset;
        }

        @Override
        protected void onPostExecute(LineSet dataset) {
            LineChartView chart = (LineChartView) findViewById(R.id.linechart);
            View progressBar = findViewById(R.id.loading_bar);

            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (ChartEntry ce : dataset.getEntries()) {
                if (ce.getValue() < min) {
                    min = (int)ce.getValue();
                }

                if (ce.getValue() > max) {
                    max = (int)Math.ceil(ce.getValue());
                }
            }

            dataset.setColor(getResources().getColor(R.color.material_blue_500));

            chart.addData(dataset);
            chart.setXLabels(AxisController.LabelPosition.NONE);
            chart.setLabelsColor(getResources().getColor(android.R.color.secondary_text_dark));
            chart.setAxisBorderValues(min, max);
            chart.setStep(Math.max((max - min) / 10, 1));
            chart.show();

            progressBar.setVisibility(View.GONE);
            chart.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_history);

        Intent intent = getIntent();
        if (intent != null) {
            String symbol = intent.getStringExtra(QuoteColumns.SYMBOL);

            TextView stockSymbol = (TextView) findViewById(R.id.stock_symbol);
            stockSymbol.setText(symbol);

            new FetchStockHistory().execute(symbol);
        }
    }
}
