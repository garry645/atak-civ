
package com.atakmap.android.maps.graphics;


import gov.tak.test.KernelJniTest;
import com.atakmap.interop.Pointer;
import com.atakmap.interop.Interop;
import com.atakmap.map.MockRenderContext;
import com.atakmap.map.MockSurface;
import com.atakmap.map.RenderContext;

import org.junit.BeforeClass;
import org.junit.Test;

public class GLLabelManagerTest extends KernelJniTest {

    @BeforeClass
    public static void checkLibraryLoad() {
        RenderContext renderContext = new MockRenderContext(
                new MockSurface(1920, 1080, 240));
        Interop<RenderContext> RenderContext_interop = Interop
                .findInterop(RenderContext.class);
        Pointer contextPtr = RenderContext_interop.wrap(renderContext);
        //GLLabelManager.create(contextPtr.raw);
    }

    @Test
    public void test_set_color() {
        //GLLabelManager.setColor(0, 255);
    }
}
