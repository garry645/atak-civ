package gov.tak.api.cot.detail;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.ModifierApi;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;

import java.util.Set;

import gov.tak.api.util.AttributeSet;

/**
 * Implementations convert {@link AttributeSet} meta data to {@link CotDetail}s and vice versa
 * These methods should be called when converting to and from CoT for a specific {@link CotDetail}
 * (usually supplied by {@link AttributeSet} metadata)
 *
 * @since 6.0.0
 *
 * @deprecated  use {@link ICotDetailHandler2}; interface will change to method-less class and only
 *              retain {@link ICotDetailHandler.ImportResult} enum
 */
@ModifierApi(since = "5.3.0", modifiers = {"final class"}, target = "5.7.0")
@Deprecated
@DeprecatedApi(since = "5.3.0", forRemoval = false, removeAt = "5.7.0")
public interface ICotDetailHandler {
    /**
     * Get the set of {@link CotDetail} names this {@link ICotDetailHandler} supports
     *
     * @return Set of {@link CotDetail} names
     */
    Set<String> getDetailNames();

    /**
     * Convert {@link CotEvent} {@link CotDetail} to {@link AttributeSet} metadata
     *
     * @param attrs {@link AttributeSet}
     * @param event {@link AttributeSet}'s associated {@link CotEvent}
     * @param detail The detail associated with this {@link ICotDetailHandler} (read from this)
     *
     * @return {@link ImportResult#SUCCESS} if handled successfully
     * {@link ImportResult#FAILURE} if handled but failed
     * {@link ImportResult#IGNORE} if not handled or N/A
     * {@link ImportResult#DEFERRED} if we should try again later
     */
    ICotDetailHandler.ImportResult toItemMetadata(AttributeSet attrs, CotEvent event,
                                                 CotDetail detail);
    /**
     * Convert map item metadata to a CoT detail
     *
     * @param attrs {@link AttributeSet} to read
     * @param event {@link AttributeSet}'s associated {@link CotEvent}
     * @param root The {@link CotEvent} root {@link CotDetail} (add to this)
     *
     * @return True if handled, false if not
     */
    boolean toCotDetail(AttributeSet attrs, CotEvent event,
                        CotDetail root);

    /**
     * Check if this handler supports this item
     * Used to filter out certain types from being processed by a handler
     *
     * @param attrs {@link AttributeSet}
     * @param event {@link CotEvent}
     * @param detail Associated {@link CotEvent}'s {@link CotDetail}
     * @return {@code true} if supported
     */
    boolean isSupported(AttributeSet attrs, CotEvent event, CotDetail detail);

    /**
     *  Status of an import, either one of
     *     {@code SUCCESS} - no problems
     *     {@code FAILURE} - problems not importable.
     *     {@code DEFERRED} - waiting on another item, so try again at the end.
     *     {@code IGNORE} - no problems, but also not handled
     */
    enum ImportResult {
        SUCCESS(1),
        FAILURE(3),
        DEFERRED(2),
        IGNORE(0);

        private final int priority;

        ImportResult(int priority) {
            this.priority = priority;
        }

        /**
         * Compare this {@link ImportResult} with another and return the result with
         * higher priority. This is useful when performing multiple import
         * operations in a method where a single result is returned.
         * IGNORE < SUCCESS < DEFERRED < FAILURE
         *
         * @param other Other result
         * @return The result with higher priority
         */
        public ICotDetailHandler.ImportResult getHigherPriority(
                ICotDetailHandler.ImportResult other) {
            return this.priority >= other.priority ? this : other;
        }
    }
}
