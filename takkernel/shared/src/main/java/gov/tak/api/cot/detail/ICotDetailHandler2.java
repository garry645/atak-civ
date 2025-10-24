package gov.tak.api.cot.detail;

import java.util.Set;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;

/**
 * Implementations convert {@link AttributeSet} meta data to {@link CotDetail}s and vice versa
 * These methods should be called when converting to and from CoT for a specific {@link CotDetail}
 * (usually supplied by {@link AttributeSet} metadata)
 *
 * @since 7.6.0
 */
public interface ICotDetailHandler2 {
    /**
     * Get the set of {@link CotDetail} names this {@link ICotDetailHandler} supports
     *
     * @return Set of {@link CotDetail} names
     */
    Set<String> getDetailNames();

    /**
     * Convert {@link CotEvent} {@link CotDetail} to {@link AttributeSet} metadata
     *
     * @param processItem The item being processed; owner of {@code attrs}
     * @param attrs {@link AttributeSet}
     * @param event {@link AttributeSet}'s associated {@link CotEvent}
     * @param detail The detail associated with this {@link ICotDetailHandler} (read from this)
     *
     * @return {@link ICotDetailHandler.ImportResult#SUCCESS} if handled successfully
     * {@link ICotDetailHandler.ImportResult#FAILURE} if handled but failed
     * {@link ICotDetailHandler.ImportResult#IGNORE} if not handled or N/A
     * {@link ICotDetailHandler.ImportResult#DEFERRED} if we should try again later
     */
    ICotDetailHandler.ImportResult toItemMetadata(Object processItem, AttributeSet attrs, CotEvent event,
                                                  CotDetail detail);
    /**
     * Convert map item metadata to a CoT detail
     *
     * @param processItem The item being processed; owner of {@code attrs}
     * @param attrs {@link AttributeSet} to read
     * @param event {@link AttributeSet}'s associated {@link CotEvent}
     * @param root The {@link CotEvent} root {@link CotDetail} (add to this)
     *
     * @return True if handled, false if not
     */
    boolean toCotDetail(Object processItem, AttributeSet attrs, CotEvent event,
                        CotDetail root);

    /**
     * Check if this handler supports this item
     * Used to filter out certain types from being processed by a handler
     *
     * @param processItem The item being processed; owner of {@code attrs}
     * @param attrs {@link AttributeSet}
     * @param event {@link CotEvent}
     * @param detail Associated {@link CotEvent}'s {@link CotDetail}
     * @return {@code true} if supported
     */
    boolean isSupported(Object processItem, AttributeSet attrs, CotEvent event, CotDetail detail);
}