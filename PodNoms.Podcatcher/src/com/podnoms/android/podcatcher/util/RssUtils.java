package com.podnoms.android.podcatcher.util;

import com.podnoms.android.podcatcher.providers.sync.api.ApiHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;

public class RssUtils extends HttpUtils {

    public static final String VALIDATOR_URL = "http://validator.w3.org/feed/check.cgi?output=soap12&url=";

    public static boolean isValidFeed(String url) {
        boolean ret = true;
        if (!isValidUrl(url))
            ret = false;
        else {
            ret = new ApiHandler().validatePodcastFeed(url);
        }
        return ret;
    }

    private static boolean validateRssFeed(String feedUrl) {
        String outputXMLString = new String();

        try {
            InputStream is = new URL(VALIDATOR_URL + feedUrl).openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                outputXMLString += line.trim() + "\n";
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (Exception e) {
            LogHandler.reportError("Error validating podcast", e, true);
        }

        String xml = outputXMLString;

        ValidatorHandler handler = new ValidatorHandler();
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException e) {
            return handler.isValid();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static class ValidatorHandler extends DefaultHandler {
        private boolean _valid = false;
        private boolean _inValid = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (localName.equals("validity")) {
                _inValid = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (_inValid) {
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    s.append(ch[i]);
                }
                _valid = s.toString().equals("true");
                throw new SAXException("Finished parsing");
            } else
                super.characters(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("validity")) {

            }
        }

        public boolean isValid() {
            return _valid;
        }
    }
}
