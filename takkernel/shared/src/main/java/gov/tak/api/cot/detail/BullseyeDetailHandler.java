package gov.tak.api.cot.detail;

import java.util.HashSet;
import java.util.Set;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;

/**
 * This handler only exists to deconflict {@code bullseyeUID}s sent on {@code bullseye}
 * {@link CotDetail}s by ATAK's {@code BullseyeDetailHandler}. ATAK appends these UIDs with
 * ".COMPAT" to satisfy a requirement of older ATAKs for Bullseye {@link CotEvent}s to have
 * different UIDs than the {@code bullseyeUID} attribute on the event's {@code bullseye} detail.
 * This handler will strip ".COMPAT" from the UID that is written to the {@link CotDetail} so
 * subsequent handlers can parse the UID as-is. When ATAK stops writing .COMPAT to it's Bullseye
 * {@link CotDetail}s, this handler can be removed; expected timeframe ~5.3.
 * <p/>
 * Otherwise, the only other existing Bullseye handler implementations depend entirely on types
 * specific to their clients, so no shared logic can be added to this handler. If future client
 * handlers can be implemented (or existing client handlers can be refactored) to utilize attributes
 * on the backing {@link AttributeSet}, corresponding {@link AttributeSet} reads/writes can be added
 * here.
 *
 * @since 6.0.0
 */
final class BullseyeDetailHandler implements ICotDetailHandler {
    private Set<String> _detailNames;

    public BullseyeDetailHandler() {
        _detailNames = new HashSet<>();
        _detailNames.add("bullseye");
    }

    @Override
    public boolean toCotDetail(AttributeSet attrs, CotEvent event, CotDetail detail) {
        return true;
    }

    @Override
    public boolean isSupported(AttributeSet attrs, CotEvent event, CotDetail detail) {
        return true;
    }

    @Override
    public Set<String> getDetailNames() {
        return _detailNames;
    }

    @Override
    public ImportResult toItemMetadata(AttributeSet attrs, CotEvent event,
                                       final CotDetail detail) {
        final String bullseyeUID = detail.getAttribute("bullseyeUID");
        if (bullseyeUID.endsWith(".COMPAT")) {
            detail.setAttribute("bullseyeUID", bullseyeUID.replace(".COMPAT", ""));
        }

        return ImportResult.SUCCESS;
    }
}
