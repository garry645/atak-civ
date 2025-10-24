
package com.atakmap.android.cot.detail;

public class UrlLinkDetailEntry {

    public static final String ASSOCIATED_URLS_KEY = "associated_urls";
    private final static String delim = "######";

    private final String uid;
    private String url;
    private String remark;
    private String mimeType;

    public UrlLinkDetailEntry(String uid, String url, String remark,
            String mimeType) {
        this.uid = fixEncoding(ensureEmptyString(uid));
        this.remark = ensureEmptyString(remark);
        this.url = ensureEmptyString(url);
        this.mimeType = ensureEmptyString(mimeType);
    }

    private String ensureEmptyString(String s) {
        if (s == null)
            return "";
        return s;
    }

    /**
     * Sets the URL for the link
     * @param url the url with a proper scheme
     */
    public void setUrl(String url) {
        this.url = fixEncoding(ensureEmptyString(url));
    }

    /**
     * Sets the remark that is to be used as the title of the url
     * @param remark the remark
     */
    public void setRemark(String remark) {
        this.remark = ensureEmptyString(remark);
    }

    /**
     * Sets the mimeType for the link
     * @param mimeType the mimetype
     */
    public void setMimeType(String mimeType) {
        this.mimeType = ensureEmptyString(mimeType);
    }

    /**
     * Gets the mimetype for the UrlLinkDetailEntry
     * @return the mimetype or empty string if not set
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Gets the remark for the UrlLinkDetailEntry
     * @return the remark or empty string if not set
     */
    public String getRemark() {
        return remark;
    }

    /**
     * Gets the uid for the UrlLinkDetailEntry
     * @return the uid or empty string if not set
     */
    public String getUid() {
        return uid;
    }

    /**
     * Gets the url for the UrlLinkDetailEntry
     * @return the url or empty string if not set
     */
    public String getUrl() {
        return url;
    }

    private String sanitize(String s) {
        return s.replaceAll(delim, "");
    }

    public String toStringRepresentation() {
        return sanitize(uid) + delim + sanitize(url) + delim +
                sanitize(remark) + delim + sanitize(mimeType);
    }

    public static UrlLinkDetailEntry fromStringRepresentation(String s) {
        try {
            if (s == null)
                return null;
            String[] split = s.split(delim, -1);
            if (split.length != 4)
                return null;
            return new UrlLinkDetailEntry(split[0], split[1], split[2],
                    split[3]);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Simple encoding fix for kml httpQuery (note this is very simple)
     * @param orig the original string
     * @return the fixed encoding
     */
    private static String fixEncoding(String orig) {
        return orig.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&apos;", "'").replaceAll("&quot;", "\"")
                .replaceAll("&amp;", "&");
    }

}
