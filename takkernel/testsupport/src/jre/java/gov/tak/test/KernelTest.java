package gov.tak.test;

import android.content.Context;

import java.util.Locale;

import javax.swing.SwingUtilities;

public abstract class KernelTest
{
    protected KernelTest()
    {

    }

    protected Context getTestContext()
    {
        return new Context();
    }

    protected void setLocale(Locale locale)
    {
        Locale.setDefault(locale);
    }

    protected void runOnMainThread(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }
}
