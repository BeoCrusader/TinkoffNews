package com.crusader.tinkoffnews.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.crusader.tinkoffnews.R;
import com.crusader.tinkoffnews.models.Article;

import java.util.ArrayList;

/**
 * Created by crusader on 1/8/17.
 */

public class ArticlesAdapter extends ArrayAdapter<Article> {

    private Activity context;

    public ArticlesAdapter(Activity context, ArrayList<Article> articles){
        super(context, R.layout.article, R.id.article_text, articles);
        this.context = context;
    }

    public class ViewHolder
    {
        TextView articleText;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //using ViewHolder patter to accelerate list
        ViewHolder viewHolder;
        if(convertView == null){
            LayoutInflater inflater = context.getLayoutInflater();
            convertView = inflater.inflate(R.layout.article, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.articleText = (TextView) convertView.findViewById(R.id.article_text);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Spanned spannedText;
        Article article = getItem(position);
        //getting rid of HTML tags
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            spannedText = Html.fromHtml(article.getText(), Html.FROM_HTML_MODE_COMPACT);
        } else {
            spannedText = Html.fromHtml(article.getText());
        }
        viewHolder.articleText.setText(spannedText);

        //Highlightig unreaded (and without full text) articles
        if(!article.isDetailed()){
            viewHolder.articleText.setTypeface(null, Typeface.BOLD);
            viewHolder.articleText.setTextColor(Color.BLACK);
        } else {
            viewHolder.articleText.setTypeface(null, Typeface.NORMAL);
            viewHolder.articleText.setTextColor(Color.DKGRAY);
        }
        return convertView;
    }
}
