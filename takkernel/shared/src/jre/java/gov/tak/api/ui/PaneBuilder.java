package gov.tak.api.ui;

import java.awt.Component;

public final class PaneBuilder extends PaneBuilderBase
{
    /**
     * Creates a new <code>PaneBuilder</code> with the specified {@link Component} as the pane content.
     * @param content
     */
    public PaneBuilder(Component content)
    {
        super(content, Component.class);
    }
}
