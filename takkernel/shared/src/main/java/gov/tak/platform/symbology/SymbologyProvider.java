package gov.tak.platform.symbology;

import com.atakmap.map.layer.feature.Feature;
import com.atakmap.spi.PriorityServiceProviderRegistry2;
import com.atakmap.spi.ServiceProvider;
import com.atakmap.util.Visitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ISymbologyProvider2;
import gov.tak.api.symbology.Modifier;
import gov.tak.api.symbology.ShapeType;
import gov.tak.api.symbology.Status;
import gov.tak.api.util.AttributeSet;

public final class SymbologyProvider {
    final static PriorityServiceProviderRegistry2<ISymbologyProvider, String, Spi> _providers = new PriorityServiceProviderRegistry2<>();
    static private final Set<SymbologyProvidersChangedListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());


    public static interface SymbologyProvidersChangedListener {
        public void onSymbologyProvidersChanged();
    }

    public static void addSymbologyProvidersChangedListener(SymbologyProvidersChangedListener l) {
        listeners.add(l);
    }

    public static void removeSymbologyProvidersChangedListener(SymbologyProvidersChangedListener l) {
        listeners.remove(l);
    }

    static void providersChanged()
    {
        for(SymbologyProvidersChangedListener l : listeners)
        {
            l.onSymbologyProvidersChanged();
        }
    }

    private SymbologyProvider() {}

    public static void registerProvider(@NonNull ISymbologyProvider provider, int priority) {
        _providers.register(new Spi(provider), priority);
        providersChanged();
    }

    public static void unregisterProvider(@NonNull final ISymbologyProvider provider) {
        final Spi[] spi = new Spi[1];
        _providers.visitProviders(new Visitor<Iterator<Spi>>() {
            public void visit(Iterator<Spi> iter) {
                while(iter.hasNext()) {
                    final Spi p = iter.next();
                    if(p._impl == provider) {
                        spi[0] = p;
                    }
                }
            }
        });
        if(spi[0] != null) {
            _providers.unregister(spi[0]);
            providersChanged();
        }
    }

    public static ISymbologyProvider getProviderFromSymbol(@NonNull String symbolCode) {
        final ISymbologyProvider provider = _providers.create(symbolCode);
        if(provider != null)
            return provider;

        return null;
    }


    public static ISymbologyProvider getProvider(@NonNull String name) {
        final ISymbologyProvider[] spi = new ISymbologyProvider[1];
        _providers.visitProviders(new Visitor<Iterator<Spi>>() {
            public void visit(Iterator<Spi> iter) {
                while(iter.hasNext()) {
                    final Spi p = iter.next();
                    if(p._impl.getName().equals(name)) {
                        spi[0] = p._impl;
                    }
                }
            }
        });
        return spi[0];
    }

    public static Collection<ISymbologyProvider> getProviders() {
        final ArrayList<ISymbologyProvider> spis = new ArrayList<>();
        _providers.visitProviders(new Visitor<Iterator<Spi>>() {
            public void visit(Iterator<Spi> iter) {
                while(iter.hasNext()) {
                    final Spi p = iter.next();
                    spis.add(p._impl);
                }
            }
        });
        return spis;
    }

    // ISymbologyProvider interface
    /**
     * Returns a {@link Collection} of {@link Modifier} instances that are applicable for the given
     * symbol code.
     *
     * @param symbolCode
     * @return
     */
    public static ShapeType getDefaultSourceShape(String symbolCode) {
        final ISymbologyProvider provider = getProviderFromSymbol(symbolCode);
        return (provider != null) ? provider.getDefaultSourceShape(symbolCode) : null;
    }

    public static Collection<Modifier> getNullModifiersForCode(String symbolCode) {
        final ISymbologyProvider provider = getProviderFromSymbol(symbolCode);
        return (provider != null) ? provider.getModifiers(null) : null;

    }
    public static Collection<Modifier> getModifiers(String symbolCode) {
        final ISymbologyProvider provider = getProviderFromSymbol(symbolCode);
        return (provider != null) ? provider.getModifiers(symbolCode) : null;
    }

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
    public static Collection<Feature> renderMultipointSymbol(@NonNull String code, @NonNull  IGeoPoint[] points, @Nullable AttributeSet attrs, @Nullable ISymbologyProvider.RendererHints hints) {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        return (provider != null) ? provider.renderMultipointSymbol(code, points, attrs, hints) : null;
    }
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
    public static Bitmap renderSinglePointIcon(@NonNull  String code, @Nullable AttributeSet attrs, @Nullable ISymbologyProvider.RendererHints hints)
    {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        return (provider != null) ? provider.renderSinglePointIcon(code, attrs, hints) : null;
    }

    public static Affiliation getAffiliation(String code)
    {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        return (provider != null) ? provider.getAffiliation(code) : null;
    }

    public static Status getStatus(String code) {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        if(provider instanceof ISymbologyProvider2)
            return ((ISymbologyProvider2) provider).getStatus(code);

        return Status.Present;

    }

    public static String setStatus(String code, Status status) {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        if(provider instanceof ISymbologyProvider2)
            return ((ISymbologyProvider2) provider).setStatus(code, status);
        return null;
    }

    public static @Nullable Amplifier getAmplifier(String code) {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        if(provider instanceof ISymbologyProvider2)
            return ((ISymbologyProvider2) provider).getAmplifier(code);
        return null;

    }

    public static String setAmplifier(String code, @Nullable Amplifier amplifier) {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        if(provider instanceof ISymbologyProvider2)
            return ((ISymbologyProvider2) provider).setAmplifier(code, amplifier);
        return null;
    }

    public static int getHeadquartersTaskForceDummyMask(String code) {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        if(provider instanceof ISymbologyProvider2)
            return ((ISymbologyProvider2) provider).getHeadquartersTaskForceDummyMask(code);
        return 0;

    }

    public static String setHeadquartersTaskForceDummyMask(String code, int mask) {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        if(provider instanceof ISymbologyProvider2)
            return ((ISymbologyProvider2) provider).setHeadquartersTaskForceDummyMask(code, mask);
        return null;
    }

    /**
     *
     * @param code          The current symbol code
     * @param affiliation   The new affiliation
     *
     * @return The symbol code updated for the new affiliation
     */
    public static String setAffiliation(String code, Affiliation affiliation)
    {
        final ISymbologyProvider provider = getProviderFromSymbol(code);
        return (provider != null) ? provider.setAffiliation(code, affiliation) : null;
    }

    public static ISymbolTable getSymbolTable(String symbolCode)
    {
        final ISymbologyProvider provider = getProviderFromSymbol(symbolCode);
        if(provider == null)
            return null;
        return provider.getSymbolTable();
    }

    final static class Spi implements ServiceProvider<ISymbologyProvider, String> {

        final ISymbologyProvider _impl;

        Spi(ISymbologyProvider impl) {
            _impl = impl;
        }
        @Override
        public ISymbologyProvider create(String code) {
            return _impl.getSymbolTable().getSymbol(code) != null ? _impl : null;
        }
    }
    public static String getFullName(String symbolCode) {
        final ISymbologyProvider provider = getProviderFromSymbol(symbolCode);
        if(provider == null)
            return null;

        ISymbolTable.Symbol symbol = provider.getSymbolTable().getSymbol(symbolCode);
        if(symbol == null)
            return null;

        return symbol.getFullName();

    }
}
