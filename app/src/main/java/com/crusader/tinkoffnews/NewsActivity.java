package com.crusader.tinkoffnews;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.crusader.tinkoffnews.adapters.ArticlesAdapter;
import com.crusader.tinkoffnews.models.Article;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

public class NewsActivity extends AppCompatActivity {

    //constants block
    //for getting activity result
    public static final String ARTICLE = "article";
    public static final int ARTICLE_DETAIL_REQUEST = 2;
    public static final int ARTICLE_DETAIL_RESULT = 4;
    //filename to store articles
    private final String FILE_NAME_LIST = "articles.obj";
    //API URL for list of news
    private final String API_LIST_URL = "https://api.tinkoff.ru/v1/news";

    //JSON field names
    private final String JSON_PAYLOAD = "payload";
    private final String JSON_ID = "id";
    private final String JSON_TEXT = "text";
    private final String JSON_DATE = "publicationDate";
    private final String JSON_MILLS = "milliseconds";

    //Main List of articles
    private ArrayList<Article>  articles = new ArrayList<Article>();
    //Suppirting swipe & refresh
    private SwipeRefreshLayout swipeContainer;
    //supporting volley
    private RequestQueue requestQueue;
    private ArticlesAdapter adapter;

    private final String LOG_TAG = "TinkoffNews";
    private ListView articleList;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        context = this;
        articleList = (ListView) findViewById(R.id.articles_list);
        adapter = new ArticlesAdapter(this, articles);
        articleList.setAdapter(adapter);

        //if item clicked - move to another activity for details
        articleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Article itemClicked = adapter.getItem(position);
                if(itemClicked != null){
                    Intent intent = new Intent(context, DetailActivity.class);
                    intent.putExtra(ARTICLE, itemClicked);
                    Log.i(LOG_TAG, itemClicked.getText());
                    startActivityForResult(intent, ARTICLE_DETAIL_REQUEST);
                }
            }
        });

        //Setting up the request queue
        requestQueue = Volley.newRequestQueue(this);

        //request json
        makeListRequest();

        //local file load
        FileInputStream listFileInput;
        ObjectInput listObjectInput;
        ArrayList<Article> readedArticlesList = new ArrayList<>();
        try {
            listFileInput = openFileInput(FILE_NAME_LIST);
            listObjectInput = new ObjectInputStream(listFileInput);

            Object readedObject = listObjectInput.readObject();
            if (readedObject instanceof ArrayList) {
                readedArticlesList = (ArrayList<Article>) readedObject;
                Log.i(LOG_TAG, "File readed successfully");
            }

            listObjectInput.close();
            listFileInput.close();

            scanForNewArticles(readedArticlesList);

        } catch (Exception e){
            Log.w(LOG_TAG, "Can't restore file due to unknown reason!");
            e.printStackTrace();
        }
        //End of local file load

        //setting up swipe container
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.activity_news);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                makeListRequest();
            }
        });
        //End of setting swipe container

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ARTICLE_DETAIL_REQUEST){
            if(resultCode == ARTICLE_DETAIL_RESULT){
                //add detailed information from second activity to main list
                Article articleWithDetail = (Article) data.getSerializableExtra(ARTICLE);
                int index = articles.indexOf(articleWithDetail);
                if(index > -1){
                    articles.get(index).setFullText(articleWithDetail.getFullText());
                    scanForNewArticles(null);
                    Log.i(LOG_TAG, "Article detail GET");
                }
            }
        }
    }

    //Scan for new articles in list and add them to "articles" list. Sort after.
    protected void scanForNewArticles(ArrayList<Article> newArticleList){
        if (newArticleList != null) {
            for (Article article : newArticleList) {
                if (!articles.contains(article)) {
                    articles.add(article);
                }
            }
            //Sort articles
            Collections.sort(articles, new articlesDateComparator());
        }
        articleList.invalidate();
        adapter.notifyDataSetChanged();

        //Save to local file
        try {
            new WriteListToFile().doInBackground();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //Do HTTP request and parse returned JSON object. Scan for new articles.
    protected void makeListRequest(){
        requestQueue.add(new JsonObjectRequest(Request.Method.GET, API_LIST_URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ArrayList<Article> apiArticles = new ArrayList<Article>();
                            JSONArray mJsonArray = response.getJSONArray(JSON_PAYLOAD);
                            for (int i = 0; i < mJsonArray.length(); i++){
                                JSONObject obj = mJsonArray.getJSONObject(i);
                                Integer id = Integer.parseInt(obj.getString(JSON_ID));
                                String text = obj.getString(JSON_TEXT);
                                //convert special HTML codes to displayable symbols
                                text =  unescapeHtml4(text);
                                JSONObject date = obj.getJSONObject(JSON_DATE);
                                //store date in milliseconds
                                long millis = Long.parseLong(date.getString(JSON_MILLS));
                                apiArticles.add(new Article(id, text, millis));
                            }
                            scanForNewArticles(apiArticles);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(LOG_TAG, "Error parsing JSON");
                            //Error in JSON parsing
                        }
                        //in case request was made from swipe container
                        swipeContainer.setRefreshing(false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Error from server or no connection
                        //in case request was made from swipe container
                        swipeContainer.setRefreshing(false);
                    }
                }
        ));
    }

    //Comparator to sort main list
    public class articlesDateComparator implements Comparator<Article>{
        @Override
        public int compare(Article firstArticle, Article secondArticle) {
            return firstArticle.compareTo(secondArticle);
        }
    }

    //Async write main list to file
    class WriteListToFile extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... objects) {
            try {
                FileOutputStream fileOutput = openFileOutput(FILE_NAME_LIST, Context.MODE_PRIVATE);
                ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput);
                //Write list as object
                objectOutput.writeObject(articles);
                objectOutput.close();
                fileOutput.close();
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
    }
}
