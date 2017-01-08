package com.crusader.tinkoffnews;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.crusader.tinkoffnews.models.Article;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by crusader on 1/8/17.
 * Second view to download, show and return full article text
 */

public class DetailActivity extends AppCompatActivity {

    //JSON field names
    private final String JSON_CONTENT = "content";
    private final String JSON_PAYLOAD = "payload";

    private final String LOG_TAG = "TinkoffDetail";

    //Add ID at the end of this string:
    private final String API_DETAIL_PARTIAL_URL = "https://api.tinkoff.ru/v1/news_content?id=";

    private RequestQueue requestQueue;
    //Will show data as WebView
    private WebView webView;
    Article article;
    Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        webView = (WebView) findViewById(R.id.article_detail);

        requestQueue = Volley.newRequestQueue(this);
        intent = getIntent();

        article = (Article) intent.getSerializableExtra(NewsActivity.ARTICLE);

        if(article == null)            //exit in case nothing to show.
            finish();

        if(!article.isDetailed()){
            //if no fullText cached yet - get it from server
            makeDetailRequest(article.getId());
        } else {
            //setting full text as html for webView
            webView.loadDataWithBaseURL(null, article.getFullText(), "text/html", "UTF-8", null);
        }
    }

    //Make http request for detailed article data and set it for article
    private void makeDetailRequest(Integer id){
        requestQueue.add(new JsonObjectRequest(Request.Method.GET, API_DETAIL_PARTIAL_URL + id.toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject payload = response.getJSONObject(JSON_PAYLOAD);
                            String text = payload.getString(JSON_CONTENT);
                            article.setFullText(text);
                            webView.loadDataWithBaseURL(null, text, "text/html", "UTF-8", null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            //in case error show this text:
                            webView.loadDataWithBaseURL(null, getString(R.string.error_no_internet), "text/html", "UTF-8", null);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //in case error show this text:
                        webView.loadDataWithBaseURL(null, getString(R.string.error_no_internet), "text/html", "UTF-8", null);
                    }
                }
        ));
    }

    //have to override back button cos it's not returning activity result by default
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                Intent resultIntent = new Intent();
                resultIntent.putExtra(NewsActivity.ARTICLE, article);
                setResult(NewsActivity.ARTICLE_DETAIL_RESULT, resultIntent);
                Log.i(LOG_TAG, "Result setted!");
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

}
