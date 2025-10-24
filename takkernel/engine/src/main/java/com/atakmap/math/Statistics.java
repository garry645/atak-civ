package com.atakmap.math;

import java.util.HashMap;
import java.util.Map;

public final class Statistics
{
    public long observations;
    public double mean;
    public double minimum;
    public double maximum;
    public double stddev;
    public double mode;
    private int modeCount;

    private Map<Double, int[]> record;

    public Statistics()
    {
        this(false);
    }

    public Statistics(boolean modeOrStddev)
    {
        if (modeOrStddev)
            this.record = new HashMap<Double, int[]>();
        this.reset();
    }

    public void observe(double v)
    {
        if (observations == 0)
        {
            minimum = v;
            maximum = v;
        } else
        {
            if (v < minimum)
                minimum = v;
            else if (v > maximum)
                maximum = v;
        }
        if(observations > 0) {
            // https://stats.stackexchange.com/questions/105773/how-to-calculate-sd-of-sample-for-one-new-observation
            double n_2 = observations-1;
            double n_1 = observations;
            double xn_X = v-mean;
            stddev = Math.sqrt(((n_2/n_1)*stddev*stddev)+((1d/(observations+1))*(xn_X*xn_X)));
        }
        mean = ((mean * observations) + v) / (observations + 1);
        observations++;

        if (record != null)
        {
            final Double key = Double.valueOf(v);
            int[] cnt = record.get(key);
            if (cnt == null)
            {
                cnt = new int[] {0};
                record.put(key, cnt);
            }

            cnt[0]++;
            if (cnt[0] > modeCount)
            {
                mode = v;
                modeCount = cnt[0];
            }
        }
    }

    public void reset()
    {
        observations = 0L;
        mean = 0d;
        minimum = 0d;
        maximum = 0d;
        stddev = 0d;
        mode = 0d;
        modeCount = 0;

        if (record != null)
            record.clear();
    }

    public String toString()
    {
        return "Statistics {mean=" + mean + ",observations=" + observations + "}";
    }
}
