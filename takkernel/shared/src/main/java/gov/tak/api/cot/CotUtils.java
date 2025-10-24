package gov.tak.api.cot;

import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.util.ReadWriteLock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.AttributeSetUtils;
import gov.tak.platform.symbology.SymbologyProvider;

/**
 * @since 6.0.0
 */
public class CotUtils {

    private final static String TAG = "CotUtils";
    final private static Map<String, String> cot2c = new ConcurrentHashMap<>();

    static com.atakmap.util.ReadWriteLock rw = new ReadWriteLock();

    static {
        SymbologyProvider.addSymbologyProvidersChangedListener(new SymbologyProvider.SymbologyProvidersChangedListener() {
            @Override
            public void onSymbologyProvidersChanged() {

                try {
                    rw.acquireWrite();
                    cot2c.clear();
                }
                finally {
                    rw.releaseWrite();
                }
            }
        });
    }

    /**
     * The ability to set a time  given an attribute in the {@link CotDetail} provided a
     * {@link AttributeSet} and the metaKey
     * @param editor the {@link CotDetail} to be modified
     * @param attrs the {@link AttributeSet}
     * @param metaKey the key used from the {@link AttributeSet}
     * @param attrKey the attribute to set in the CotDetail.
     */
    public static void checkSetTime(CotDetail editor, AttributeSet attrs, String metaKey,
                                    String attrKey) {
        Long time = AttributeSetUtils.getLongAttribute(attrs, metaKey, null);
        if (time != null && time > 0) {
            editor.setAttribute(attrKey, (new CoordinatedTime(time)).toString());
        }
    }

    /**
     * Given a {@link CotDetail}, checks to see if the {@link AttributeSet} contains the long and
     * if so converts it to the desired attribute tag within the element defined by the
     * {@link CotDetail}. If the item does not exist, then no attribute is created within the
     * {@link CotDetail}.
     * @param editor the {@link CotDetail}
     * @param attrs the {@link AttributeSet}
     * @param metaKey the metadata key for the {@link AttributeSet}
     * @param attrKey the attribute for the corresponding detail.
     */
    public static void checkSetLong(CotDetail editor, AttributeSet attrs, String metaKey,
                                    String attrKey) {
        Long attrsVal = AttributeSetUtils.getLongAttribute(attrs, metaKey, null);
        if (attrsVal != null) {
            editor.setAttribute(attrKey, String.valueOf(attrsVal));
        }
    }

    /**
     * Given a {@link CotDetail}, checks to see if the {@link AttributeSet} contains the int and if
     * so converts it to the desired attribute tag within the element defined by the
     * {@link CotDetail}. If the item does not exist, then no attribute is created within the
     * {@link CotDetail}.
     * @param editor the {@link CotDetail}
     * @param attrs the {@link AttributeSet}
     * @param metaKey the metadata key for the {@link AttributeSet}
     * @param attrKey the attribute for the corresponding detail.
     */
    public static void checkSetInt(CotDetail editor, AttributeSet attrs,
                                   String metaKey,
                                   String attrKey) {
        Integer attrsVal = AttributeSetUtils.getIntAttribute(attrs, metaKey, null);
        if (attrsVal != null) {
            editor.setAttribute(attrKey, String.valueOf(attrsVal));
        }
    }

    /**
     * Given a {@link CotDetail}, checks to see if the {@link AttributeSet} contains the double and
     * if so converts it to the desired attribute tag within the element defined by the
     * @link CotDetail}. If the item does not exist, then no attribute is created within the
     * {@link CotDetail}.
     * @param editor the {@link CotDetail}
     * @param attrs the {@link AttributeSet}
     * @param metaKey the metadata key for the {@link AttributeSet}
     * @param attrKey the attribute for the corresponding {@link CotDetail}.
     */
    public static void checkSetDouble(CotDetail editor, AttributeSet attrs,
                                      String metaKey,
                                      String attrKey) {
        Double attrsVal = AttributeSetUtils.getDoubleAttribute(attrs, metaKey, null);
        if (attrsVal != null) {
            editor.setAttribute(attrKey, String.valueOf(attrsVal));
        }
    }

    /**
     * Given a {@link CotDetail}, checks to see if the {@link AttributeSet} contains the String and
     * if so converts it to the desired attribute tag within the element defined by the
     * {@link CotDetail}. If the item does not exist, then no attribute is created within the
     * {@link CotDetail}.
     * @param editor the {@link CotDetail}
     * @param attrs the {@link AttributeSet}
     * @param metaKey the metadata key for the {@link AttributeSet}
     * @param attrKey the attribute for the corresponding detail.
     */
    public static void checkSetString(CotDetail editor, AttributeSet attrs, String metaKey,
                                      String attrKey) {
        editor.setAttribute(attrKey, attrs.getStringAttribute(metaKey, null));
    }

    /**
     * Given a {@link CotDetail}, checks to see if the {@link AttributeSet} contains the boolean and
     * if so converts it to the desired attribute tag within the element defined by the
     * {@link CotDetail}. If the item does not exist, then no attribute is created within the
     * {@link CotDetail}.
     * @param editor the {@link CotDetail}
     * @param attrs the {@link AttributeSet}
     * @param metaKey the metadata key for the {@link AttributeSet}
     * @param attrKey the attribute for the corresponding {@link CotDetail}.
     */
    public static void checkSetBoolean(CotDetail editor, AttributeSet attrs, String metaKey,
                                       String attrKey) {
        Boolean attrsVal = AttributeSetUtils.getBooleanAttribute(attrs, metaKey, null);
        if (attrsVal != null) {
            editor.setAttribute(attrKey, String.valueOf(attrsVal));
        }
    }

    /**
     * Provided a {@link AttributeSet} and a key, convert the string value to a double and set the
     * value in the {@link AttributeSet}
     * @param attrs the {@link AttributeSet}
     * @param key the corresponding key to set
     * @param val the value in string form.
     */
    public static void setDouble(final AttributeSet attrs, String key, String val) {
        try {
            if (val != null) {
                attrs.setAttribute(key, Double.parseDouble(val));
            }
        } catch (Exception e) {
            Log.d(TAG, "error setting: " + key + "with " + val);
        }
    }

    /**
     * Provided a {@link AttributeSet} and a key, convert the string value to a integer and set the
     * value in the {@link AttributeSet}
     * @param attrs the {@link AttributeSet}
     * @param key the corresponding key to set
     * @param val the value in string form.
     */
    public static void setInteger(final AttributeSet attrs, String key, String val) {
        try {
            if (val != null) {
                attrs.setAttribute(key, (int) Double.parseDouble(val));
            }
        } catch (Exception e) {
            Log.d(TAG, "error setting: " + key + "with " + val);
        }
    }

    /**
     * Provided a {@link AttributeSet} and a key, convert the string value to a long and set the
     * value in the {@link AttributeSet}
     * @param attrs the {@link AttributeSet}
     * @param key the corresponding key to set
     * @param val the value in string form.
     */
    public static void setLong(final AttributeSet attrs, String key, String val) {
        try {
            if (val != null) {
                attrs.setAttribute(key, Long.parseLong(val));
            }
        } catch (Exception e) {
            Log.d(TAG, "error setting: " + key + "with " + val);
        }
    }

    /**
     * Provided a {@link AttributeSet} and a key, convert the string value to a boolean and set the
     * value in the {@link AttributeSet}
     * @param attrs the {@link AttributeSet}
     * @param key the corresponding key to set
     * @param val the value in string form.
     */
    public static void setBoolean(final AttributeSet attrs, String key, String val) {
        try {
            if (val != null) {
                attrs.setAttribute(key, Boolean.parseBoolean(val));
            }
        } catch (Exception e) {
            Log.d(TAG, "error setting: " + key + "with " + val);
        }
    }

    /**
     * Provided a {@link AttributeSet} and a key, checks to make sure the string cannot be null.
     * @param attrs the {@link AttributeSet}
     * @param key the corresponding key to set
     * @param val the value in string form.
     */
    public static void setString(final AttributeSet attrs, String key, String val) {
        if (val != null) {
            attrs.setAttribute(key, val);
        }
    }

    /**
     * Retrieve the callsign for this event
     * @param event the {@link CotEvent} to retrieve the callsign from.
     * @return The callsign of this event or null if it doesn't exist
     */
    public static String getCallsign(CotEvent event) {
        String callsign = null;
        CotDetail detail = event.getDetail();
        if (detail != null) {
            CotDetail contact = detail.getFirstChildByName(0, "contact");
            if (contact != null) {
                callsign = contact.getAttribute("callsign");
            }
        }
        return callsign;
    }
    public static String mil2525cFromCotType(String type) {
        try {
            rw.acquireRead();
            String s2525C = cot2c.get(type);
            if (s2525C != null)
                return s2525C;

            if (type != null && type.indexOf("a") == 0 && type.length() > 2) {

                switch (type.charAt(2)) {
                    case 'f':
                    case 'a':
                        s2525C = "sf";
                        break;
                    case 'n':
                        s2525C = "sn";
                        break;
                    case 's':
                    case 'j':
                    case 'k':
                    case 'h':
                        s2525C = "sh";
                        break;
                    case 'u':
                    default:
                        s2525C = "su";
                        break;
                }
                StringBuilder s2525CSB = new StringBuilder(s2525C);
                for (int x = 4; x < type.length(); x += 2) {
                    char[] t = {
                            type.charAt(x)
                    };
                    String s = new String(t);
                    s2525CSB.append(s.toLowerCase(LocaleUtil.getCurrent()));
                    if (x == 4) {
                        s2525CSB.append("p");
                    }
                }
                for (int x = s2525CSB.length(); x < 15; x++) {
                    if (x == 10 && s2525CSB.charAt(2) == 'g'
                            && s2525CSB.charAt(4) == 'i') {
                        s2525CSB.append("h");
                    } else {
                        s2525CSB.append("-");
                    }
                }

                Bitmap bmp = null;
                try {
                    bmp = SymbologyProvider.renderSinglePointIcon(
                            s2525CSB.toString().toUpperCase(LocaleUtil.US),
                            new AttributeSet(), null);
                } catch (Throwable ignored) {
                }

                // if there is a provider that can be used to check validity, then use the provider
                // otherwise assume that the conversion is correct based on the standard CoT to
                // 2525c rules.
                if (bmp == null && !SymbologyProvider.getProviders().isEmpty()) {
                    int lastDashIdx = type.lastIndexOf('-');
                    if (lastDashIdx != -1) {
                        String val = mil2525cFromCotType(
                                type.substring(0, lastDashIdx));
                        cot2c.put(type, val);
                        return val;
                    }
                } else {
                    cot2c.put(type, s2525CSB.toString());
                }

                return s2525CSB.toString();
            }

            return "";
        }
        finally {
            rw.releaseRead();
        }
    }

    /**
     * Obtain a CoT type from a given 2525C symbol identifier
     * @param type the 2525C symbol identifier
     * @return the cot type
     */
    public static String cotTypeFromMil2525C(final String typeOrig) {
        String type = null;
        if(typeOrig != null)
            type = typeOrig.toLowerCase();
        if (type != null && type.indexOf("s") == 0 && type.length() > 2) {
            String cotType;
            switch (type.charAt(1)) {
                case 'f':
                    cotType = "a-f";
                    break;
                case 'n':
                    cotType = "a-n";
                    break;
                case 'h':
                    cotType = "a-h";
                    break;
                case 'u':
                default:
                    cotType = "a-u";
            }
            StringBuilder cotTypeBuilder = new StringBuilder(cotType);
            for (int i = 2; i < type.length(); ++i) {
                if (i == 3)
                    continue;
                if (type.charAt(i) == '-')
                    break;

                cotTypeBuilder.append(("-" + type.charAt(i)).toUpperCase());
            }
            cotType = cotTypeBuilder.toString();
            return cotType;

        } else if (type != null && type.length() > 2) {
            String cotType;
            switch (type.charAt(1)) {
                case 'f':
                    cotType = "a-f-G";
                    break;
                case 'n':
                    cotType = "a-n-G";
                    break;
                case 'h':
                    cotType = "a-h-G";
                    break;
                case 'u':
                default:
                    cotType = "a-u-G";
            }
            return cotType;

        }

        return "";
    }

}
