/*
 * Taken from the 'Learning Android' project,
 * released as Public Domain software at
 * http://github.com/digitalspaghetti/learning-android
 * and modified heavily for Transdroid
 */
package org.ifies.android.sax;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.transdroid.util.HttpHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class RssParser extends DefaultHandler {
    private String urlString;
    private Channel channel;
    private StringBuilder text;
    private Item item;
    private boolean imgStatus;

    public RssParser(String url) {
        this.urlString = url;
        this.text = new StringBuilder();
    }

    /**
     * Returns the feed as a RssFeed, which is a ListArray
     *
     * @return RssFeed rssFeed
     */
    public Channel getChannel() {
        return (this.channel);
    }

    public void parse() throws ParserConfigurationException, SAXException, IOException {

        HttpClient httpclient = initialise();
        HttpResponse result = httpclient.execute(new HttpGet(urlString));
        //FileInputStream urlInputStream = new FileInputStream("/sdcard/rsstest2.txt");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        if (spf != null) {
            SAXParser sp = spf.newSAXParser();
            sp.parse(result.getEntity().getContent(), this);
        }

    }

    /**
     * Instantiates an HTTP client that can be used for all requests.
     */
    protected HttpClient initialise() {
        return HttpHelper.buildDefaultSearchHttpClient(false);
    }

    /**
     * By default creates a standard Item (with title, description and links), which may to overriden to add more data.
     *
     * @return A possibly decorated Item instance
     */
    protected Item createNewItem() {
        return new Item();
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {

        /** First lets check for the channel */
        if (localName.equalsIgnoreCase("channel")) {
            this.channel = new Channel();
        }

        /** Now lets check for an item */
        if (localName.equalsIgnoreCase("item") && (this.channel != null)) {
            this.item = createNewItem();
            this.channel.addItem(this.item);
        }

        /** Now lets check for an image */
        if (localName.equalsIgnoreCase("image") && (this.channel != null)) {
            this.imgStatus = true;
        }

        /** Checking for a enclosure */
        if (localName.equalsIgnoreCase("enclosure")) {
            /** Lets check we are in an item */
            if (this.item != null && attributes != null && attributes.getLength() > 0) {
                if (attributes.getValue("url") != null) {
                    this.item.setEnclosureUrl(parseLink(attributes.getValue("url")));
                }
                if (attributes.getValue("type") != null) {
                    this.item.setEnclosureType(attributes.getValue("type"));
                }
                if (attributes.getValue("length") != null) {
                    this.item.setEnclosureLength(Long.parseLong(attributes.getValue("length")));
                }
            }
        }

        if (this.item != null)
            addAdditionalData(localName, qName, attributes, this.item);

    }

    /**
     * This is where we actually parse for the elements contents
     */
    public void endElement(String uri, String localName, String qName) {
        /** Check we have an RSS Feed */
        if (this.channel == null) {
            return;
        }

        /** Check are at the end of an item */
        if (localName.equalsIgnoreCase("item")) {
            this.item = null;
        }

        /** Check we are at the end of an image */
        if (localName.equalsIgnoreCase("image")) {
            this.imgStatus = false;
        }

        /** Now we need to parse which title we are in */
        if (localName.equalsIgnoreCase("title")) {
            /** We are an item, so we set the item title */
            if (this.item != null) {
                this.item.setTitle(this.text.toString().trim());
                /** We are in an image */
            } else {
                this.channel.setTitle(this.text.toString().trim());
            }
        }

        /** Now we are checking for a link */
        if (localName.equalsIgnoreCase("link")) {
            /** Check we are in an item **/
            if (this.item != null) {
                this.item.setLink(parseLink(this.text.toString()));
                /** Check we are in an image */
            } else if (this.imgStatus) {
                this.channel.setImage(parseLink(this.text.toString()));
                /** Check we are in a channel */
            } else {
                this.channel.setLink(parseLink(this.text.toString()));
            }
        }

        /** Checking for a description */
        if (localName.equalsIgnoreCase("description")) {
            /** Lets check we are in an item */
            if (this.item != null) {
                this.item.setDescription(this.text.toString().trim());
                /** Lets check we are in the channel */
            } else {
                this.channel.setDescription(this.text.toString().trim());
            }
        }

        /** Checking for a pubdate */
        if (localName.equalsIgnoreCase("pubDate")) {
            /** Lets check we are in an item */
            if (this.item != null) {
                try {
                    this.item.setPubdate(new Date(Date.parse(this.text.toString().trim())));
                } catch (Exception e) {
                    // Date is malformed (not parsable by Date.parse)
                }
                /** Lets check we are in the channel */
            } else {
                try {
                    this.channel.setPubDate(new Date(Date.parse(this.text.toString().trim())));
                } catch (Exception e) {
                    // Date is malformed (not parsable by Date.parse)
                }
            }
        }

        /** Check for the category */
        if (localName.equalsIgnoreCase("category") && (this.item != null)) {
            this.channel.addCategory(this.text.toString().trim());
        }

        if (this.item != null)
            addAdditionalData(localName, this.item, this.text.toString());

        this.text.setLength(0);
    }

    /**
     * May be overridden to add additional data from tags that are not standard in RSS. Not used by this default RSS style parser.
     * Executed on start element
     *
     * @param localName  The tag name
     * @param qName      The tag name
     * @param attributes The attributes attached to the element
     * @param item       The Item we are currently parsing
     */
    protected void addAdditionalData(String localName, String qName, Attributes attributes, Item item) {
    }

    /**
     * May be overridden to add additional data from tags that are not standard in RSS. Not used by this default RSS style parser.
     * Executed on end element
     *
     * @param localName The tag name
     * @param item      The Item we are currently parsing
     * @param text      The new text content
     */
    protected void addAdditionalData(String localName, Item item, String text) {
    }

    public void characters(char[] ch, int start, int length) {
        this.text.append(ch, start, length);
    }

    private String parseLink(String string) {
        return string.trim();
    }

}
