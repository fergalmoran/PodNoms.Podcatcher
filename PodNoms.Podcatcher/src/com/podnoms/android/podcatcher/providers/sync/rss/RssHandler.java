package com.podnoms.android.podcatcher.providers.sync.rss;
/*
    HUGE Kudos to Octavian Damiean (http://stackoverflow.com/users/418183/octavian-damiean)
    for this hugely useful answer
    http://stackoverflow.com/questions/4827344/how-to-parse-xml-using-the-sax-parser/4828765#4828765
 */

import android.sax.*;
import android.text.TextUtils;
import android.util.Xml;
import com.podnoms.android.podcatcher.PodNomsApplication;
import com.podnoms.android.podcatcher.ui.widgets.AlertDialogs;
import com.podnoms.android.podcatcher.util.LogHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class RssHandler extends DefaultHandler {

    private class TerminatorException extends RuntimeException {

    }

    private Channel _channel;
    private ArrayList<Item> _items;
    private Item _item;
    private int _maxElements;
    private int _currentItem;
    private static final String ITUNES_NAMESPACE = "http://www.itunes.com/dtds/podcast-1.0.dtd";

    public RssHandler() {
        _items = new ArrayList<Item>();
    }

    public Channel parse(URL url, int maxElements) throws IOException, SAXException {
        try {
            _currentItem = 0;
            _maxElements = maxElements;
            return parse(url.openStream());
        } catch (Exception ex) {
            AlertDialogs.InfoDialog(PodNomsApplication.getContext(),
                    String.format("Podcast %s returns invalid xml. Please check the URL", url));
        }
        return null;
    }

    public Channel parse(InputStream is) throws SAXException {

        RootElement root = new RootElement("rss");
        Element chanElement = root.getChild("channel");
        Element chanTitle = chanElement.getChild("title");
        Element chanLink = chanElement.getChild("link");
        Element chanDescription = chanElement.getChild("description");

        Element chanImageItunes = chanElement.getChild(ITUNES_NAMESPACE, "image");
        Element chanImageUrlItunes = chanImageItunes.getChild("url");

        Element chanImage = chanElement.getChild("image");
        Element chanImageUrl = chanImage.getChild("url");

        Element chanItem = chanElement.getChild("item");
        Element itemGuid = chanItem.getChild("guid");
        Element itemTitle = chanItem.getChild("title");
        Element itemDescription = chanItem.getChild("description");
        Element itemPubDate = chanItem.getChild("pubDate");
        Element itemLink = chanItem.getChild("link");
        Element itemEnclosure = chanItem.getChild("enclosure");

        root.setEndElementListener(new EndElementListener() {
            @Override
            public void end() {
                _channel.setItems(_items);
            }
        });
        chanElement.setStartElementListener(new StartElementListener() {
            public void start(Attributes attributes) {
                _channel = new Channel();
            }
        });
        chanLink.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String s) {
                _channel.setLink(s);
            }
        });
        chanTitle.setEndTextElementListener(new EndTextElementListener() {
            public void end(String s) {
                _channel.setTitle(s);
            }
        });
        chanDescription.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String s) {
                _channel.setDescription(s);
            }
        });
        chanImageItunes.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                _channel.setImage(attributes.getValue("href"));
            }
        });
        chanImageUrlItunes.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String s) {
                _channel.setImage(s);
            }
        });
        chanImage.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                _channel.setImage(attributes.getValue("href"));
            }
        });
        chanImageUrl.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String s) {
                _channel.setImage(s);
            }
        });
        //_item tag handlers
        chanItem.setStartElementListener(new StartElementListener() {
            public void start(Attributes attributes) {
                _item = new Item();
            }
        });
        chanItem.setEndElementListener(new EndElementListener() {
            public void end() {
                _items.add(_item);
                if (_maxElements == ++_currentItem) {
                    _channel.setItems(_items);
                    throw new TerminatorException();
                }
            }
        });

        itemGuid.setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                _item.setGuid(body);
            }
        });
        itemTitle.setEndTextElementListener(new EndTextElementListener() {
            public void end(String body) {
                _item.setTitle(body);
            }
        });
        itemDescription.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String s) {
                _item.setDescription(s);
            }
        });
        itemLink.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String s) {
                _item.setLink(s);
            }
        });
        itemPubDate.setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String s) {
                _item.setItemDate(s);
            }
        });
        itemEnclosure.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String length = attributes.getValue("length");
                if (!TextUtils.isEmpty(length))
                    _item.setLength(Long.parseLong(length));
                else
                    _item.setLength(0);
                _item.setEnclosure(attributes.getValue("url"));
            }
        });
        try {
            Xml.parse(is, Xml.Encoding.UTF_8, root.getContentHandler());
            return _channel;
        } catch (java.net.SocketException e) {
            LogHandler.reportError("Cannot contact feed", e);
        } catch (TerminatorException e) {
            return _channel;
        } catch (IOException e) {
            LogHandler.reportError("Error parsing feed", e);
        }
        return null;
    }
}