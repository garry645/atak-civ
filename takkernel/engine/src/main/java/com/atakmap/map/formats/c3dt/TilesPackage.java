package com.atakmap.map.formats.c3dt;

import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.database.QueryIface;
import com.atakmap.database.StatementIface;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class TilesPackage implements ContentSource, ContentContainer
{
    String _cacheFile;
    String _uriBase;
    DatabaseIface _database;
    QueryIface _queryStmt;
    StatementIface _insertStmt;

    Set<OnContentChangedListener> _listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    TilesPackage(String cacheFile) {
        _cacheFile = cacheFile;
    }

    @Override
    public synchronized byte[] getData(String uri, long[] version) {
        if(_database == null)
            return null;

        if(_queryStmt == null)
            _queryStmt = _database.compileQuery("SELECT content FROM media WHERE key = ? LIMIT 1");
        try {
            _queryStmt.bind(1, uri);
            return _queryStmt.moveToNext() ? _queryStmt.getBlob(0) : null;
        } finally {
            _queryStmt.clearBindings();
        }
    }

    @Override
    public void addOnContentChangedListener(OnContentChangedListener l) {
        _listeners.add(l);
    }

    @Override
    public void removeOnContentChangedListener(OnContentChangedListener l) {
        _listeners.remove(l);
    }

    @Override
    public synchronized void connect() {
        if(_database == null) {
            File cacheFile = new File(_cacheFile);
            if(!cacheFile.exists())
                cacheFile.getParentFile().mkdirs();
            _database = Databases.openOrCreateDatabase(_cacheFile);

            // create tables as needed
            _database.execute("CREATE TABLE IF NOT EXISTS media (key TEXT PRIMARY KEY, content BLOB)", null);
        }
    }

    @Override
    public synchronized void disconnect() {
        if(_insertStmt != null) {
            _insertStmt.close();
            _insertStmt = null;
        }
        if(_queryStmt != null) {
            _queryStmt.close();
            _queryStmt = null;
        }
        if(_database != null) {
            _database.close();
            _database = null;
        }
    }

    @Override
    public void put(String uri, byte[] data, long version) {
        synchronized(this) {
            if(_database == null)
                return;
            if (_insertStmt == null)
                _insertStmt = _database.compileStatement("INSERT INTO media (key, content) VALUES(?, ?) ON CONFLICT(key) DO UPDATE SET content=excluded.content");
            try {
                _insertStmt.bind(1, uri);
                _insertStmt.bind(2, data);
                _insertStmt.execute();
            } finally {
                _insertStmt.clearBindings();
            }
        }

        if(!_listeners.isEmpty()) {
            for (OnContentChangedListener l : _listeners)
                l.onContentChanged(this);
        }
    }
}
