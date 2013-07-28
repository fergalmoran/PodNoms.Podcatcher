package com.podnoms.android.podcatcher.providers.sync.rss;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;

public class Channel implements Serializable {

    private ArrayList<Item> items;
    private String title;
    private String link;
    private String description;
    private String lastBuildDate;
    private String docs;
    private String language;
    private String image;

    public ArrayList<Item> getItems() {
        return items;
    }

    public void setItems(ArrayList<Item> items) {
        this.items = items;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLastBuildDate() {
        return lastBuildDate;
    }

    public void setLastBuildDate(String lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }

    public String getDocs() {
        return docs;
    }

    public void setDocs(String docs) {
        this.docs = docs;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getImage() {
        return this.image;
    }

    public void setImage(String image) {
        if (TextUtils.isEmpty(this.image))
            this.image = image;
    }
}