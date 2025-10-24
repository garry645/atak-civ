package gov.tak.api.ui;

import javax.swing.JPanel;
import java.awt.Component;

public class PaneBuilderTest extends PaneBuilderBaseTest {
    @Override
    protected Class<?> getPaneSourceType() {
        return Component.class;
    }

    @Override
    protected PaneBuilder newInstance() {
        return new PaneBuilder(new JPanel());
    }
}
