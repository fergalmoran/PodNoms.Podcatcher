package com.podnoms.android.podcatcher.providers.sync.rss;

import android.text.TextUtils;
import com.podnoms.android.podcatcher.util.LogHandler;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Item implements Serializable {

    private String guid;
    private String title;
    private String description;
    private String link;
    private String enclosure;
    private long length;
    private String dateCreated;
    private String image;

    public Item() {
        setTitle(null);
        setDescription(null);
        setLink(null);
    }
    public String getGuid() {
        return TextUtils.isEmpty(guid) ? "" : guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getEnclosure() {
        return enclosure;
    }

    public void setEnclosure(String enclosure) {
        this.enclosure = enclosure;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getLength() {
        return length;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public String getDateForSql(SimpleDateFormat dateParser, int nItem) {
        String ret = "";
        try {
            Date localDate = new Date(dateParser.parse(this.dateCreated).getTime() + (nItem * 1000));
            String str = new SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.getDefault()).format(localDate);
            ret = str;
        } catch (ParseException localParseException) {
            LogHandler.reportError("Error formatting publish date", localParseException);
        } catch (NullPointerException e) {
            LogHandler.showLog("Blank pub date");
        }
        return ret;
    }

    public void setItemDate(String s) {
        dateCreated = s;
    }
}
