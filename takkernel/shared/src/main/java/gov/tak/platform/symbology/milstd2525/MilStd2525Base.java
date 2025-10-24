package gov.tak.platform.symbology.milstd2525;

import armyc2.c5isr.renderer.utilities.SymbolUtilities;
import com.atakmap.util.zip.IoUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.ResourceType;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Status;

public class MilStd2525Base
{
    static boolean initialized = false;

    static Map<String,String> c2d = new HashMap<>(2048);
    static Map<String,String> d2c = new HashMap<>(2048);

    final static MilStd2525cSymbologyProvider cProvider = new MilStd2525cSymbologyProvider();
    final static MilStd2525dSymbologyProvider dProvider = new MilStd2525dSymbologyProvider();

    MilStd2525Base(){}

    static String cleanupC(final String orig) {
        String upper = orig.toUpperCase();

        if(upper.startsWith("S") || upper.startsWith("E")) {
            if (upper.charAt(10) == 'H')
                upper = upper.substring(0, 11) + "****";
            else
                upper = upper.substring(0, 10) + "*****";
        }
        else if(upper.startsWith("G")) {
            upper = upper.substring(0,10) + "****X";
        }
        else if(upper.startsWith("I")) {
            upper = upper.substring(0,12) + "***";
        }
        else if(upper.startsWith("O")) {
            upper = upper.substring(0,10) + "*****";
        }

        return upper;
    }

    /**
     * Returns the 2525D code corresponding to the provided 2525C code.
     *
     * @param orig  A 2525C code
     *
     * @return  The corresponding 2525D code or {@code null} if the 2525D code could not be
     *          translated.
     */
    public static String get2525DFrom2525C(final String orig) {
        MilStd2525.init();
        String upper = cleanupC(orig);


        String d = c2d.get(upper);
        if(d != null)
            return d;

        StringBuilder code = new StringBuilder(upper);

        boolean isWeather = upper.startsWith("W");

        String affilCode = null;
        String statusCode = null;

        Affiliation affil = cProvider.getAffiliation(upper);
        Status status = cProvider.getStatus(upper);

        if(!isWeather) {
            statusCode = upper.substring(3, 4);

            if (!statusCode.equals("P")) {
                code.setCharAt(3, 'P');
            }

            d = c2d.get(code.toString());

            affilCode = upper.substring(1, 2);
            if (d == null && !affilCode.equals("*")) {
                code.setCharAt(1, '*');
                d = c2d.get(code.toString());
            }
        }

        if(d == null)
            d = c2d.get(code.toString());

        if(d != null) {
            if(affilCode != null && !affilCode.equals("*")) {
                d = dProvider.setAffiliation(d, affil);
            }
            if(statusCode != null && !statusCode.equals("*")) {
                d = dProvider.setStatus(d, status);
            }
        }

        return d;
    }

    /**
     * Returns the 2525C code corresponding to the provided 2525D+ code.
     *
     * @return  The corresponding 2525C code or {@code null} if the 2525D code could not be
     *          translated.
     *          
     * @see {@link #get2525CFrom2525D(String, boolean)}
     */
    public static String get2525CFrom2525D(final String type) {
        return get2525CFrom2525D(type, true);
    }

    private static String resetAfilliationAndStatusD(final String type, Affiliation affil, Status status){
        String c = cProvider.setStatus(type, status);
        if (affil != Affiliation.Pending)
            c = cProvider.setAffiliation(c, affil);

        return c;
    }

    /**
     * Returns the 2525C code corresponding to the provided 2525D+ code.
     * 
     * @param type  A 2525D+ code
     * @param exact If {@code true} a 2525C code will be returned only if an exact translation can
     *              be performed. If {@code false}, a best match translation is performed by
     *              checking for an exact match, then stripping off the subtype, type and finally
     *              entity.
     *              
     * @return  The corresponding 2525C code or {@code null} if the 2525D code could not be
     *          translated.
     */
    public static String get2525CFrom2525D(final String type, boolean exact) {
        MilStd2525.init();

        String typed = type;
        if(typed.length() > 20) {
            typed = typed.substring(0,20);
        }
        if(typed.startsWith("13")) {
            typed = "10" + typed.substring(2);
        }
        String c = d2c.get(typed);
        if(c != null)
            return c;

        Status status = dProvider.getStatus(typed);
        Affiliation affil = dProvider.getAffiliation(typed);

        String code = dProvider.setStatus(typed, Status.Present);
        code = dProvider.setAffiliation(code, Affiliation.Pending);

        c = d2c.get(code);
        if (c != null) {
            c = resetAfilliationAndStatusD(c, affil, status);
        }
        if(c != null || exact)
            return c;

        //remove modifiers
        code = code.substring(0,16) + "0000";
        c = get2525CFrom2525D(code);
        if(c != null)
            return resetAfilliationAndStatusD(c, affil, status);
        //remove entity subtype
        code = code.substring(0,14) + "000000";
        c = get2525CFrom2525D(code);
        if(c != null)
            return resetAfilliationAndStatusD(c, affil, status);
        //remove entity type
        code = code.substring(0,12) + "00000000";
        c = get2525CFrom2525D(code);
        if(c != null)
            return resetAfilliationAndStatusD(c, affil, status);
        //remove entity
        code = code.substring(0,10) + "0000000000";
        c = get2525CFrom2525D(code);
        if(c != null)
            return resetAfilliationAndStatusD(c, affil, status);
        return c;
    }

    /**
     * Initializes the {@code MilStd2525} mappings
     *
     * @param resourceManager   A resource manager for the application context
     * 
     * @return  {@code true} if initialization was performed, {@code false} if already initialized
     */
    static synchronized boolean init(IResourceManager resourceManager) {
        // already initialized
        if(initialized)
            return false;

        InputStream stream = null;
        try
        {
            // load from assets
            final int id = resourceManager.getResourceId("mil2525cd_csv", ResourceType.Raw);
            if (id != 0)
            {
                stream = resourceManager.openRawResource(id);
            }
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream));

            String line;
            while ((line = reader.readLine()) != null) {
                final String c;
                final String d;
                if(line.charAt(16) == ',') {
                    c = line.substring(0, 16);
                    d = line.substring(16);
                } else {
                    String[] cd = line.split(",");
                    c = cd[0];
                    d = cd[1];
                }
                c2d.put(c, d);
                d2c.put(d, c);
            }
        } catch (Throwable t)
        {
            throw new ExceptionInInitializerError(t);
        } finally
        {
            initialized = true;
            IoUtils.close(stream);
        }

        // initialization was performed
        return true;
    }

    public static String get2525DFrom2525E(String e) {
        if(e == null || e.length() != 30 || !e.startsWith("13"))
            return null;
        final String basic = SymbolUtilities.getBasicSymbolID(e);
        if(basic == null)
            return null;
        return "10" + e.substring(2, 20);
    }

    public static String get2525EFrom2525D(String d) {
        if(d == null || d.length() != 20 || !d.startsWith("10"))
            return null;
        final String basic = SymbolUtilities.getBasicSymbolID(d);
        if(basic == null)
            return null;
        return "13" + d.substring(2) + "0000000000";
    }
}
