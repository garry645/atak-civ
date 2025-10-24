package gov.tak.api.ui;

public class PaneBuilderTest {
    public static class View extends PaneBuilderBaseTest {
        @Override
        protected Class<?> getPaneSourceType() {
            return android.view.View.class;
        }

        @Override
        protected PaneBuilder newInstance() {
            return new PaneBuilder(new android.view.View(getTestContext()));
        }
    }

    public static class Fragment extends PaneBuilderBaseTest {
        @Override
        protected Class<?> getPaneSourceType() {
            return androidx.fragment.app.Fragment.class;
        }

        @Override
        protected PaneBuilder newInstance() {
            return new PaneBuilder(new androidx.fragment.app.Fragment());
        }
    }
}
