package com.atakmap.android.importfiles.callbacks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.importfiles.ImportAlternateContactResolver;
import gov.tak.api.importfiles.ImportCotResolver;
import gov.tak.api.importfiles.ImportDTEDZResolver;
import gov.tak.api.importfiles.ImportGPXResolver;
import gov.tak.api.importfiles.ImportMissionPackageResolver;
import gov.tak.api.importfiles.ImportResolver;
import gov.tak.api.importfiles.ImportUserIconSetResolver;

/**
 * Self-registers callbacks ATAK requires for kernel-provided {@link ImportResolver}s.
 */
public final class ImportResolverCallbackRegistry {
    private static volatile ImportResolverCallbackRegistry instance;

    private final Map<Class<? extends ImportResolver>, Set<ImportResolver.MatchListener>> _matchListeners;
    private final Map<Class<? extends ImportResolver>, Set<ImportResolver.BeginImportListener>> _beginImportListeners;
    private final Map<Class<? extends ImportResolver>, Set<ImportResolver.FileSortedListener>> _fileSortedListeners;
    private final Map<Class<? extends ImportResolver>, Set<ImportResolver.ImportListener>> _importListeners;
    private final ConcurrentLinkedQueue<ImportResolver> _resolvers;

    public static ImportResolverCallbackRegistry getInstance() {
        if (instance == null) {
            synchronized (ImportResolverCallbackRegistry.class) {
                if (instance == null) {
                    instance = new ImportResolverCallbackRegistry();
                }
            }
        }
        return instance;
    }

    private ImportResolverCallbackRegistry() {
        _matchListeners = new ConcurrentHashMap<>();
        _beginImportListeners = new ConcurrentHashMap<>();
        _fileSortedListeners = new ConcurrentHashMap<>();
        _importListeners = new ConcurrentHashMap<>();
        _resolvers = new ConcurrentLinkedQueue<>();
        registerCallbacks();
    }

    private void registerCallbacks() {
        _beginImportListeners.put(ImportAlternateContactResolver.class, Set.of(new AlternateContactCallback()));
        _beginImportListeners.put(ImportCotResolver.class, Set.of(new CotCallback()));
        _fileSortedListeners.put(ImportGPXResolver.class, Set.of(new GPXRouteCallback()));
        _fileSortedListeners.put(ImportUserIconSetResolver.class, Set.of(new UserIconSetCallback()));

        MissionPackageCallback mpCallback = new MissionPackageCallback();
        _matchListeners.put(ImportMissionPackageResolver.class, Set.of(mpCallback));
        _beginImportListeners.put(ImportMissionPackageResolver.class, Set.of(mpCallback));

        DTEDZCallback dtedzCallback = new DTEDZCallback();
        _beginImportListeners.put(ImportDTEDZResolver.class, Set.of(dtedzCallback));
        _fileSortedListeners.put(ImportDTEDZResolver.class, Set.of(dtedzCallback));
    }

    /**
     * {@link ImportResolver}s have several inner callback interfaces. Registering an
     * {@link ImportResolver} here will apply any {@link ImportResolverCallbackRegistry}-registered
     * callback to the {@link ImportResolver}, as long as the callback has been registered against
     * the provided {@link ImportResolver}'s type.
     */
    public void registerResolver(ImportResolver resolver) {
        _resolvers.add(resolver);

        Class<? extends ImportResolver> clazz = resolver.getClass();
        Set<ImportResolver.MatchListener> matchListeners = getMatchListeners(clazz);
        if (matchListeners != null) {
            for (ImportResolver.MatchListener listener : matchListeners) {
                resolver.addMatchListener(listener);
            }
        }

        Set<ImportResolver.BeginImportListener> beginImportListeners = getBeginImportListeners(clazz);
        if (beginImportListeners != null) {
            for (ImportResolver.BeginImportListener listener : beginImportListeners) {
                resolver.addBeginImportListener(listener);
            }
        }

        Set<ImportResolver.FileSortedListener> fileSortedListeners = getFileSortedListeners(clazz);
        if (fileSortedListeners != null) {
            for (ImportResolver.FileSortedListener listener : fileSortedListeners) {
                resolver.addFileSortedListener(listener);
            }
        }

        Set<ImportResolver.ImportListener> importListeners =
                mergeSets(getImportListeners(clazz), _importListeners.get(ImportResolver.class));
        if (!importListeners.isEmpty()) {
            for (ImportResolver.ImportListener listener : importListeners) {
                resolver.addFileImportedListener(listener);
            }
        }
    }

    public void unregisterResolver(ImportResolver resolver) {
        _resolvers.remove(resolver);

        Class<? extends ImportResolver> clazz = resolver.getClass();
        Set<ImportResolver.MatchListener> matchListeners = getMatchListeners(clazz);
        if (matchListeners != null) {
            for (ImportResolver.MatchListener listener : matchListeners) {
                resolver.removeMatchListener(listener);
            }
        }

        Set<ImportResolver.BeginImportListener> beginImportListeners = getBeginImportListeners(clazz);
        if (beginImportListeners != null) {
            for (ImportResolver.BeginImportListener listener : beginImportListeners) {
                resolver.removeBeginImportListener(listener);
            }
        }

        Set<ImportResolver.FileSortedListener> fileSortedListeners = getFileSortedListeners(clazz);
        if (fileSortedListeners != null) {
            for (ImportResolver.FileSortedListener listener : fileSortedListeners) {
                resolver.removeFileSortedListener(listener);
            }
        }

        Set<ImportResolver.ImportListener> importListeners =
                mergeSets(getImportListeners(clazz), _importListeners.get(ImportResolver.class));
        if (!importListeners.isEmpty()) {
            for (ImportResolver.ImportListener listener : importListeners) {
                resolver.removeFileImportedListener(listener);
            }
        }
    }

    /**
     * Registers a listener that will be applied to all {@link ImportResolver}s registered via
     * {@link #registerResolver(ImportResolver)}. Existing, registered {@link ImportResolver}s
     * will have their callback lists updated w/ the provided {@link ImportResolver.ImportListener}.
     */
    public void registerImportListener(ImportResolver.ImportListener listener) {
        synchronized (_importListeners) {
            Set<ImportResolver.ImportListener> existingListeners = _importListeners.get(ImportResolver.class);
            if (existingListeners == null) {
                final HashSet hs = new HashSet();
                hs.add(listener);
                _importListeners.put(ImportResolver.class, hs);
            } else {
                existingListeners.add(listener);
                _importListeners.put(ImportResolver.class, existingListeners);
            }
        }

        // Legacy ImportResolver called into listeners registered dynamically on the ImportExportMapComponent.
        // For parity w/ that functionality, update existing resolvers as new listeners are added
        for (ImportResolver resolver : _resolvers) {
            if (!resolver.getImportListeners().contains(listener))
                resolver.addFileImportedListener(listener);
        }
    }

    public void unregisterImportListener(ImportResolver.ImportListener listener) {
        synchronized (_importListeners) {
            Set<ImportResolver.ImportListener> existingListeners = _importListeners.get(ImportResolver.class);
            if (existingListeners != null) {
                existingListeners.remove(listener);
                _importListeners.put(ImportResolver.class, existingListeners);
            }
        }
    }

    private Set<ImportResolver.MatchListener> getMatchListeners(Class<? extends ImportResolver> clazz) {
        Set<ImportResolver.MatchListener> matchListeners = new HashSet<>();
        for (Class<? extends ImportResolver> c : _matchListeners.keySet()) {
            if (c.isAssignableFrom(clazz)) {
                matchListeners = mergeSets(matchListeners, _matchListeners.get(c));
            }
        }
        return matchListeners;
    }

    private Set<ImportResolver.BeginImportListener> getBeginImportListeners(Class<? extends ImportResolver> clazz) {
        Set<ImportResolver.BeginImportListener> matchListeners = new HashSet<>();
        for (Class<? extends ImportResolver> c : _beginImportListeners.keySet()) {
            if (c.isAssignableFrom(clazz)) {
                matchListeners = mergeSets(matchListeners, _beginImportListeners.get(c));
            }
        }
        return matchListeners;
    }

    private Set<ImportResolver.FileSortedListener> getFileSortedListeners(Class<? extends ImportResolver> clazz) {
        Set<ImportResolver.FileSortedListener> matchListeners = new HashSet<>();
        for (Class<? extends ImportResolver> c : _fileSortedListeners.keySet()) {
            if (c.isAssignableFrom(clazz)) {
                matchListeners = mergeSets(matchListeners, _fileSortedListeners.get(c));
            }
        }
        return matchListeners;
    }

    private Set<ImportResolver.ImportListener> getImportListeners(Class<? extends ImportResolver> clazz) {
        Set<ImportResolver.ImportListener> matchListeners = new HashSet<>();
        for (Class<? extends ImportResolver> c : _importListeners.keySet()) {
            if (c.isAssignableFrom(clazz)) {
                matchListeners = mergeSets(matchListeners, _importListeners.get(c));
            }
        }
        return matchListeners;
    }

    private <T> Set<T> mergeSets(Set<T> s1, Set<T> s2) {
        Set<T> merged = new HashSet<>();
        if (s1 != null)
            merged.addAll(s1);
        if (s2 != null)
            merged.addAll(s2);
        return merged;
    }
}
