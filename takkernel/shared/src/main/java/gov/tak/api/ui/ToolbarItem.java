package gov.tak.api.ui;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.commons.graphics.Drawable;

import java.util.Objects;
import java.util.UUID;

/**
 * Definition for an item that will be added to the toolbar.
 */
@SuppressWarnings("unused") // API
public class ToolbarItem
{
    // The name of the item.
    private final String title;

    // The category of the plugin creating this item.
    private final String category;

    // The unique identifier that will be passed to the listener.
    private final String identifier;

    // The icon to use for the item.
    private final Drawable icon;

    // The listener that will be notified when the item is clicked.
    private final IToolbarItemListener listener;

    // Flag that indicates that the icon should cover the entire button.
    private final boolean fullButtonIcon;

    /**
     * Constructs an item to be added to the toolbar. Used only by the {@link Builder}.
     *
     * @param builder The builder that was used to construct this item.
     */
    private ToolbarItem(Builder builder)
    {
        this.title = builder.title;
        this.icon = builder.icon;
        this.category = builder.category;
        this.identifier = builder.identifier;
        this.listener = builder.listener;
        this.fullButtonIcon = builder.fullButtonIcon;
    }

    /**
     * Returns the toolbar item's name.
     *
     * @return The item's name.
     */
    @NonNull
    public String getTitle()
    {
        return title;
    }

    /**
     * Returns the toolbar item's icon.
     *
     * @return The item's icon.
     */
    @NonNull
    public Drawable getIcon()
    {
        return icon;
    }

    /**
     * Returns the toolbar item owner plugin's category. If <code>null</code>, the toolbar item should be associated
     * with the _general_ or _default_ category.
     *
     * <P>Note that not all applications may not provide a user experience for toolbar groupings.
     *
     * @return The item owner's family.
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Returns the toolbar item's unique identifier.
     *
     * @return The item's identifier.
     */
    @NonNull
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * Returns the flag that indicates if the icon the icon should be stretched to fill the entire
     * extent of the button. If {@code false}, the icon will be displayed at the nominal size.
     *
     * @return The flag that indicates if the icon should be used for the entire button with no
     * background.
     */
    public boolean isFullButtonIcon()
    {
        return fullButtonIcon;
    }

    /**
     * Returns the toolbar item's listener.
     *
     * @return The item's listener.
     */
    @Nullable
    public IToolbarItemListener getListener()
    {
        return listener;
    }

    /**
     * The builder used to construct new toolbar items.
     */
    public static class Builder
    {
        // The name of the item.
        private final String title;

        // The icon to use for the item.
        private final Drawable icon;

        // The category of the plugin creating this item.
        private String category;

        // The unique identifier that will be passed to the listener.
        private String identifier;

        // The listener that will be notified when the item is clicked.
        private IToolbarItemListener listener;

        // Flag that indicates that the icon should cover the entire button.
        private boolean fullButtonIcon;

        /**
         * Constructs a new {@link ToolbarItem} builder. A title and icon are always required for
         * toolbar items.
         *
         * @param title      The toolbar item's title.
         * @param icon       The toolbar item's icon.
         */
        public Builder(@NonNull String title, @NonNull Bitmap icon)
        {
            this(title, new BitmapDrawable(icon));
        }

        public Builder(@NonNull String title, @NonNull Drawable icon)
        {
            this.title = title;
            this.icon = icon;
            this.category = null;
            this.identifier = UUID.randomUUID().toString();
        }

        /**
         * Sets the listener that will be notified when the item is clicked.
         *
         * @param listener The listener that will be notified when the item is clicked.
         * @return This builder.
         */
        public Builder setListener(IToolbarItemListener listener)
        {
            this.listener = listener;
            return this;
        }

        /**
         * By default, the item's icon is placed inside a standard button with some padding. Setting
         * the full button icon option to true will ask the host to use the item's icon as the
         * entire button with no background.
         *
         * @param fullButtonIcon {@code true} if the icon should be used as the entire button,
         *                       {@code false} otherwise.
         * @return This builder.
         */
        public Builder setFullButtonIcon(boolean fullButtonIcon)
        {
            this.fullButtonIcon = fullButtonIcon;
            return this;
        }

        /**
         * Sets the category for the toolbar item. If not specified, the toolbar item is not categorized into any
         * grouping.
         *
         * @param category  The category
         * @return  This builder.
         */
        public Builder setCategory(String category)
        {
            this.category = category;
            return this;
        }

        public Builder setIdentifier(@NonNull String identifier)
        {
            Objects.requireNonNull(identifier);

            this.identifier = identifier;
            return this;
        }
        /**
         * Builds a new {@link ToolbarItem}.
         *
         * @return A new ToolbarItem based on this builder's configuration.
         */
        public ToolbarItem build()
        {
            Objects.requireNonNull(title, "A title must be provided.");
            Objects.requireNonNull(icon, "An icon must be provided.");
            Objects.requireNonNull(identifier, "An identifier must be provided.");

            return new ToolbarItem(this);
        }
    }
}
