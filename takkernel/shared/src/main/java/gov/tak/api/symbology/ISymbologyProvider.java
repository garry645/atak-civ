package gov.tak.api.symbology;

import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.math.PointD;

import java.util.Collection;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.util.AttributeSet;

public interface ISymbologyProvider {
    class RendererHints {
        /**
         * Specifies an optional control point (only applicable to select symbols). If {@code null}
         * and a control point is used, the value will be populated on return. If non-{@code null}
         * and a control point is not used for the symbol, the value will be cleared on return.
         */
        @Nullable public IGeoPoint controlPoint = null;
        /**
         * The specialized type of the points to be rendered.
         */
        @Nullable public ShapeType shapeType = null;

        /**
         * The desired stroke color for the symbol. If {@code 0}, a default color will be applied
         * based on the symbol code.
         */
        public int strokeColor = 0;
        /**
         * The desired stroke width for the symbol. Defaults to {@code 1.0f}.
         */
        public float strokeWidth = 1f;

        @NonNull public Feature.AltitudeMode altitudeMode = Feature.AltitudeMode.ClampToGround;
        /**
         * The resolution (meters-per-pixel) of the viewport that the symbol is being rendered to.
         * If {@code Double.NaN}, a default resolution will be used based on the extent of the
         * input points.
         */
        public double resolution = Double.NaN;
        /**
         * The bounding box for the viewport that the symbol is being rendered to. If
         * non-{@code null}, clippping may be applied to the rendered features. Defaults
         * to {@code null}
         */
        @Nullable public Envelope boundingBox = null;

        /**
         * Specifies the desired icon size for single-point symbologies. If {@code 0}, a default
         * size will be selected. Defaults to {@link 0}
         */
        public int iconSize = 0;

        /**
         * Specifies the desired font size for labels without explicitly defined font sizes.
         * If {@code 0}, a default size will be selected. Defaults to {@link 0}
         */
        public int fontSize = 0;

        /**
         * If non-{@code null} returns the symbol's icon center point offset for rendered single
         * point symbologies.
         */
        public PointD iconCenterOffset;
    }

    /**
     * @return  {@code true} if the symbology set includes single point symbols
     */
    boolean hasSinglePoint();
    /**
     * @return  {@code true} if the symbology set includes multi-point symbols
     */
    boolean hasMultiPoint();

    /**
     * @return  the name of the symbology set.
     */
    String getName();

    @NonNull ShapeType getDefaultSourceShape(String symbolCode);

    /**
     * Returns a {@link Collection} of {@link Modifier} instances that are applicable for the given
     * symbol code.
     *
     * @param symbolCode
     * @return
     */
    Collection<Modifier> getModifiers(String symbolCode);

    /**
     *
     * @param code
     * @param points
     * @param attrs     User specified attributes for the symbol. Currently supported attributes
     *                  include:
     *                  <UL>
     *                    <LI>{@code "milsym.mod.<modifier-id>"}, where {@code <modifier-id>} is the
     *                    modifier ID per {@link Modifier#getId()} and the value is a string. For
     *                    modifiers supporting multiple fields, the field values shall be comma
     *                    delimited.</LI
     *                  </UL>
     * @param hints     Hints for the symbol renderer
     * @return
     */
    Collection<Feature> renderMultipointSymbol(@NonNull String code, @NonNull  IGeoPoint[] points, @Nullable AttributeSet attrs, @Nullable  RendererHints hints);
    /**
     *
     * @param code
     * @param attrs     User specified attributes for the symbol. Currently supported attributes
     *                  include:
     *                  <UL>
     *                    <LI>{@code "milsym.mod.<modifier-id>"}, where {@code <modifier-id>} is the
     *                    modifier ID per {@link Modifier#getId()} and the value is a string. For
     *                    modifiers supporting multiple fields, the field values shall be comma
     *                    delimited.</LI
     *                  </UL>
     * @param hints     Hints for the symbol renderer
     * @return
     */
    Bitmap renderSinglePointIcon(@NonNull  String code, @Nullable AttributeSet attrs, @Nullable RendererHints hints);

    ISymbolTable getSymbolTable();

    Affiliation getAffiliation(String symbolCode);

    /**
     *
     * @param symbolCode    The current symbol code
     * @param affiliation   The new affiliation
     *
     * @return The symbol code updated for the new affiliation
     */
    String setAffiliation(String symbolCode, Affiliation affiliation);

}
