package gov.tak.platform.engine;

import gov.tak.api.engine.map.RenderSurface;
import com.atakmap.coremap.log.Log;

import com.jogamp.opengl.GLAutoDrawable;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Implementation of the takkernel RenderSurface for JOGL
 *
 * @since 3.0
 */
public abstract class JOGLRenderSurface implements RenderSurface
{
    private JOGLRenderSurface()
    {
    }

    /**
     * return dots per inch of the surface
     *
     * @return
     */
    @Override
    public final double getDpi()
    {
        return 128.0;
    }

    /**
     * create a RenderSurface for the given drawable
     *
     * @param drawable drawable to construct with
     * @return new RenderSurface
     */
    public static RenderSurface get(GLAutoDrawable drawable)
    {
        if (drawable instanceof Component)
        {
            return new ComponentImpl((Component) drawable);
        } else if (drawable instanceof com.jogamp.newt.Window)
        {
            return new WindowImpl((com.jogamp.newt.Window) drawable);
        } else
        {
            return null;
        }
    }

    /**
     * Component implementation of the RenderSurface
     *
     * @since 3.0
     */
    private static class ComponentImpl extends JOGLRenderSurface
    {

        final Component impl;
        final Map<OnSizeChangedListener, ComponentListener> listeners = new IdentityHashMap<>();

        private ComponentImpl(Component impl)
        {
            this.impl = impl;
        }

        /**
         * @return width of the surface
         */
        @Override
        public int getWidth()
        {
            return impl.getWidth();
        }

        /**
         * @return height of the surface
         */
        @Override
        public int getHeight()
        {
            return impl.getHeight();
        }

        /**
         * Add Listener for size changes
         */
        @Override
        public void addOnSizeChangedListener(OnSizeChangedListener l)
        {
            ComponentListener cl;
            synchronized (listeners)
            {
                if (listeners.containsKey(l))
                {
                    return;
                }
                cl = new ComponentAdapter()
                {
                    @Override
                    public void componentResized(ComponentEvent e)
                    {
                        l.onSizeChanged(ComponentImpl.this, impl.getWidth(), impl.getHeight());
                    }
                };
                listeners.put(l, cl);
            }
            impl.addComponentListener(cl);
        }

        /**
         * Remove listener for size changes
         */
        @Override
        public void removeOnSizeChangedListener(OnSizeChangedListener l)
        {
            ComponentListener cl;
            synchronized (listeners)
            {
                cl = listeners.remove(l);
                if (cl == null)
                {
                    return;
                }
            }
            impl.removeComponentListener(cl);
        }
    }

    /**
     * window implementation of the RenderSurface
     *
     * @since 3.0
     */
    private static class WindowImpl extends JOGLRenderSurface
    {

        final com.jogamp.newt.Window impl;
        final Map<OnSizeChangedListener, com.jogamp.newt.event.WindowListener> listeners = new IdentityHashMap<>();

        private WindowImpl(com.jogamp.newt.Window impl)
        {
            this.impl = impl;
        }

        /**
         * @return width of the window
         */
        @Override
        public int getWidth()
        {
            return impl.getWidth();
        }

        /**
         * @return height of the window
         */
        @Override
        public int getHeight()
        {
            return impl.getHeight();
        }

        /**
         * Add Listener for size changes
         */
        @Override
        public void addOnSizeChangedListener(OnSizeChangedListener l)
        {
            com.jogamp.newt.event.WindowListener cl;
            synchronized (listeners)
            {
                if (listeners.containsKey(l))
                {
                    return;
                }
                cl = new com.jogamp.newt.event.WindowAdapter()
                {
                    @Override
                    public void windowResized(com.jogamp.newt.event.WindowEvent e)
                    {
                        l.onSizeChanged(WindowImpl.this, impl.getSurfaceWidth(), impl.getSurfaceHeight());
                    }
                };
                listeners.put(l, cl);
            }
            impl.addWindowListener(cl);
        }

        /**
         * Remove Listener for size changes
         */
        @Override
        public void removeOnSizeChangedListener(OnSizeChangedListener l)
        {
            com.jogamp.newt.event.WindowListener cl;
            synchronized (listeners)
            {
                cl = listeners.remove(l);
                if (cl == null)
                {
                    return;
                }
            }
            impl.removeWindowListener(cl);
        }
    }
}
