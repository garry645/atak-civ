
package gov.tak.platform.widgets.opengl;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.widgets.IParentWidget;
import gov.tak.api.widgets.opengl.IGLWidget;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.commons.opengl.Matrix;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class GLAbstractParentWidget extends GLWidget implements
        IParentWidget.OnWidgetListChangedListener
{

    private final static String TAG = "GLAbstractParentWidget";

    public GLAbstractParentWidget(IParentWidget subject,
                                  MapRenderer orthoView)
    {
        super(subject, orthoView);
        rebuildChildren();
    }

    @Override
    public void start()
    {
        super.start();
        if (subject instanceof IParentWidget)
            ((IParentWidget) subject)
                    .addOnWidgetListChangedListener(this);
    }

    @Override
    public void stop()
    {
        super.stop();
        if (subject instanceof IParentWidget)
            ((IParentWidget) subject)
                    .removeOnWidgetListChangedListener(this);
    }

    @Override
    public void drawWidgetContent(DrawState drawState)
    {

        DrawState localDrawState = drawState.clone();
        Matrix.translateM(localDrawState.modelMatrix, 0, _padding[LEFT], -_padding[TOP], 0f);
        synchronized (_children)
        {
            for (IGLWidget c : _children.values())
                c.drawWidget(localDrawState);
        }
        localDrawState.recycle();
    }

    @Override
    public void releaseWidget()
    {
        synchronized (_children)
        {
            for (IGLWidget w : _childRenderers.values()) {
                w.stop();
                w.releaseWidget();
            }
            _childRenderers.clear();
            _children.clear();
        }
    }

    @Override
    public void onWidgetAdded(IParentWidget parent, final int index,
                              IMapWidget child)
    {
        synchronized (_children)
        {
            rebuildChildren();
        }
    }

    @Override
    public void onWidgetRemoved(IParentWidget parent, final int index,
                                IMapWidget child)
    {
        synchronized (_children)
        {
            final IGLWidget glWidget = _childRenderers.remove(child);
            if(glWidget == null)
                return;
            glWidget.stop();
            glWidget.releaseWidget();

            rebuildChildren();
        }
    }

    private void rebuildChildren() {
        List<IMapWidget> children = ((IParentWidget)subject).getChildren();
        _children.clear();
        for(int i = 0; i < children.size(); i++)
        {
            final IMapWidget c = children.get(i);
            IGLWidget g = _childRenderers.get(c);
            if(g == null) {
                g = GLWidgetFactory.create(this.getMapRenderer(), c);
                if(g != null) {
                    g.start();
                    _childRenderers.put(c, g);
                }
            }
            if(g != null)
                _children.put(Integer.valueOf(i), g);
        }
    }

    protected List<IGLWidget> getChildren()
    {
        List<IGLWidget> ret = new ArrayList<>();
        synchronized (_children)
        {
            ret.addAll(_children.values());
        }
        return ret;
    }

    protected final SortedMap<Integer, IGLWidget> _children = new TreeMap<>();
    private final Map<IMapWidget, IGLWidget> _childRenderers = new IdentityHashMap<>();
}
