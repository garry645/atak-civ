package gov.tak.platform.symbology.milstd2525;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

final class MilSymRendererDashPatterns {
    static Map<float[], Short> knownDashPatterns = new TreeMap<>(new Comparator<float[]>() {
        @Override
        public int compare(float[] a, float[] b) {
            if(a.length > b.length)
                return 1;
            else if(a.length < b.length)
                return -1;
            for(int i = 0; i < a.length; i++) {
                if(a[i] > b[i])
                    return 1;
                else if(a[i] < b[i])
                    return -1;
            }
            return 0;
        }
    });
    static {
        float width = 1f;

        float[] getLineStroke_style1_dash = new float[]{2.0F * width, 2.0F * width, 2.0F * width, 2.0F * width};
        float[] getLineStroke_style2_dot = new float[]{0.1F * width, 2.0F * width, 0.1F * width, 2.0F * width, 0.1F * width, 2.0F * width, 0.1F * width, 2.0F * width};
        float[] getLineStroke_style3_dashdot = new float[]{4.0F * width, 2.0F * width, 0.1F * width, 2.0F * width};
        float[] getLineStroke_style4_dashdotdot = new float[]{2.0F * width, 2.0F * width, 0.1F * width, 2.0F * width, 0.1F * width, 2.0F * width};
        float[] getLineStroke2_style1_dash = new float[]{2.0F * width, 2.0F * width};
        float[] getLineStroke2_style2_dot = new float[]{0.1F * width, 2.0F * width};
        float[] getLineStroke2_style3_dashdot = new float[]{4.0F * width, 2.0F * width, 0.1F * width, 2.0F * width};
        float[] getLineStroke2_style4_dashdotdot = new float[]{2.0F * width, 2.0F * width, 0.1F * width, 2.0F * width, 0.1F * width, 2.0F * width};

        final short dashPattern = (short)0x0F0F;
        final short dotPattern = (short)0x1111;
        final short dashDotPattern = (short)0x6F6F;
        final short dashDotDotPattern = (short)0xEAEA;

        knownDashPatterns.put(getLineStroke_style1_dash, Short.valueOf(dashPattern));
        knownDashPatterns.put(getLineStroke_style2_dot, Short.valueOf(dotPattern));
        knownDashPatterns.put(getLineStroke_style3_dashdot, Short.valueOf(dashDotPattern));
        knownDashPatterns.put(getLineStroke_style4_dashdotdot, Short.valueOf(dashDotDotPattern));
        knownDashPatterns.put(getLineStroke2_style1_dash, Short.valueOf(dashPattern));
        knownDashPatterns.put(getLineStroke2_style2_dot, Short.valueOf(dotPattern));
        knownDashPatterns.put(getLineStroke2_style3_dashdot, Short.valueOf(dashDotPattern));
        knownDashPatterns.put(getLineStroke2_style4_dashdotdot, Short.valueOf(dashDotDotPattern));
    }

    private MilSymRendererDashPatterns() {}

    static Short get(float[] dashPattern) {
        return knownDashPatterns.get(dashPattern);
    }
}
