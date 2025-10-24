package gov.tak.api.symbology;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.commons.graphics.Drawable;

public interface ISymbolTable {

    /** Content mask for any _line_ type shape */
    EnumSet<ShapeType> MASK_LINE = EnumSet.of(ShapeType.LineString);
    /** Content mask for any _area_ type shape */
    EnumSet<ShapeType> MASK_AREA = EnumSet.of(ShapeType.Circle, ShapeType.Ellipse, ShapeType.Rectangle, ShapeType.Polygon);
    /** Content mask for all _area_ and _line_ type shapes, excluding only _point_ shapes */
    EnumSet<ShapeType> MASK_AREAS_AND_LINES = EnumSet.of(ShapeType.Circle, ShapeType.Ellipse, ShapeType.Rectangle, ShapeType.Polygon, ShapeType.LineString);

    interface Entry {
        /**
         * Returns the name of the entry.
         *
         * @return
         */
        @NonNull String getName();

        /**
         * Returns a mask indicating all symbol shape types. Available on this {@link Entry}, and
         * any descendent entries for nested derivatives.
         *
         * @return
         */
        @NonNull EnumSet<ShapeType> getContentMask();
    }
    interface Entry2 extends Entry {
        /**
         * Returns the parent {@link Folder} of the current {@link Entry}
         * @return The parent {code Folder}.
         */
        Folder getParent();
    }
    interface Folder extends Entry {

        /**
         * Returns the nested folders for the current node. Children are ordered alphabetically.
         * @return
         */
        @NonNull Collection<Folder> getChildren();

        /**
         * Returns all symbols contained in the current node. Children are ordered alphabetically.
         * @return
         */
        @NonNull Collection<Symbol> getSymbols();
    }
    interface Symbol extends Entry {
        /**
         * Returns the full name of the symbol. This value may contain extended information as
         * compared to {@link Entry#getName()}.
         * @return
         */
        @NonNull String getFullName();

        /**
         * Returns the code associated with the symbol.
         * @return
         */
        @NonNull String getCode();

        /**
         * Returns a summary for the symbol. If no summary is available {@code null} is returned.
         * @return
         */
        @Nullable String getSummary();

        /**
         * Generates a preview {@link Drawable} of the symbol. This method shall produce a preview
         * for both single-point and multi-point symbols.
         *
         * @param color If {@code 0}, the default color for the symbol shall be used. If non-zero,
         *              the value os a ARGB 32-bit packed color that shall be used to replace the
         *              non-transparent pixels in the preview.
         * @return  The preview icon. May be {@code null} if no preview icon can be generated.
         */
        @Nullable Drawable getPreviewDrawable(int color);
    }

    /**
     * Returns the root folder for a hierarchical view of the symbology set.
     *
     * @return
     */
    Folder getRoot();

    /**
     * Retrieves a {@link List} of all {@link Symbol}s matching the specified search criteria.
     *
     * @param searchString  The search string
     * @param contentMask   If non-{@code null}, only those symbols sharing one or more of the
     *                      specified {@link ShapeType}s will be returned. If {@code null} all
     *                      {@link Symbol}s matching {@code searchString} will be returned,
     *                      regardless of {@link ShapeType}
     * @return
     */
    @NonNull List<Symbol> find(String searchString, @Nullable EnumSet<ShapeType> contentMask);

    /**
     * Returns the {@link Symbol} matching the specified symbol code.
     *
     * @param code
     * @return  The {@link Symbol} matching the specified symbol code or {@link null} if no such
     *          symbol exists in the symbology set.
     */
    @Nullable Symbol getSymbol(String code);
}

