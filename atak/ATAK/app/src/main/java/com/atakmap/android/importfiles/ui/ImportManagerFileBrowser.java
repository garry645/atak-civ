
package com.atakmap.android.importfiles.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atakmap.android.gui.ImportFileBrowser;
import com.atakmap.android.icons.IconsMapAdapter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.math.MathUtils;
import com.atakmap.app.R;
import com.atakmap.coremap.concurrent.NamedThreadFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import gov.tak.api.importfiles.ImportUserIconSetResolver;

/**
 * Extended version of the ImportFileBrowser that can be used to select multiple files
 * and includes an up button within the view instead of requiring an external one.
 */
public class ImportManagerFileBrowser extends ImportFileBrowser implements
        DialogInterface.OnKeyListener {

    private static final String TAG = "ImportManagerFileBrowser";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd", LocaleUtil.getCurrent());

    protected final Set<File> _selectedItems = new HashSet<>();
    protected boolean _multiSelect = true;
    protected boolean _directorySelect = true;
    protected FileSort _fileSort;
    protected final List<Button> _sortBtns = new ArrayList<>();
    protected ImageButton _selectAllBtn, _deselectAllBtn;

    protected final View.OnClickListener _selectAllChkListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ((ImportManagerFileBrowser.FileItemAdapter) _adapter)
                    .setAllChecked(true);
        }
    };

    protected final View.OnClickListener _deselectAllChkListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ((ImportManagerFileBrowser.FileItemAdapter) _adapter)
                    .setAllChecked(false);
        }
    };

    private final Comparator<File> _sortChecked = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            synchronized (_selectedItems) {
                boolean s1 = _selectedItems.contains(f1);
                boolean s2 = _selectedItems.contains(f2);
                return Boolean.compare(s1, s2);
            }
        }
    };

    public static ImportManagerFileBrowser inflate(MapView mv) {
        return (ImportManagerFileBrowser) LayoutInflater.from(mv.getContext())
                .inflate(R.layout.import_manager_file_browser, mv, false);
    }

    public ImportManagerFileBrowser(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImportManagerFileBrowser(Context context) {
        super(context);
    }

    /**
     * Set the title of this dialog
     * @param title Title string
     */
    public void setTitle(String title) {
        View titleView = findViewById(R.id.importManagerFileBrowserTitle);
        if (titleView != null)
            titleView.setVisibility(View.VISIBLE);
        TextView titleTV = findViewById(
                R.id.importManagerFileBrowserTitleText);
        if (titleTV != null)
            titleTV.setText(title);
    }

    /**
     * Set the title of this dialog
     * @param titleId Title string ID
     */
    public void setTitle(int titleId) {
        setTitle(getContext().getString(titleId));
    }

    /**
     * Set whether multi-select mode is enabled for the browser
     * @param multiSelect True to enable multi-select
     */
    public void setMultiSelect(boolean multiSelect) {
        _multiSelect = multiSelect;
        if (_adapter != null)
            _adapter.notifyDataSetChanged();
    }

    /**
     * Set whether directories can be selected in multi-select mode
     * @param dirSelect True to allow directories to be selected
     */
    public void allowDirectorySelect(boolean dirSelect) {
        _directorySelect = dirSelect;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // After the view finishes inflating, attach an on click
        // handler to the up/back button next to the current directory.
        ImageButton upButton = this
                .findViewById(R.id.importManagerFileBrowserUpButton);
        if (upButton != null) {
            setUpButton(upButton);
        }

        ImageButton internalButton = this.findViewById(R.id.phone);
        if (internalButton != null) {
            setInternalButton(internalButton);
        }

        ImageButton externalButton = this.findViewById(R.id.sdcard);
        if (externalButton != null) {
            setExternalButton(externalButton);
        }

        ImageButton newFolderBtn = findViewById(R.id.newFolderBtn);
        if (newFolderBtn != null)
            setNewFolderButton(newFolderBtn);

        ViewGroup sortModes = findViewById(R.id.importBrowserSortModes);
        if (sortModes != null) {
            setupSortButton(R.id.sort_name);
            setupSortButton(R.id.sort_date);
            setupSortButton(R.id.sort_size);
            setupSortButton(R.id.sort_checked);
        }

        ImageButton selectAllBtn = findViewById(
                R.id.select_all_import_manager_btn);
        ImageButton deselectAllBtn = findViewById(
                R.id.deselect_all_import_manager_btn);
        if (selectAllBtn != null)
            setupSelectAllButton(selectAllBtn);

        if (deselectAllBtn != null)
            setupDeselectAllBtn(deselectAllBtn);
    }

    @Override
    protected void _createAdapter() {
        // Create an adapter that can handle selection via check boxes.
        _adapter = new FileItemAdapter(getContext(),
                R.layout.import_manager_file_browser_fileitem,
                _fileList);
    }

    @Override
    public void setAlertDialog(AlertDialog alert) {
        super.setAlertDialog(alert);

        // This will take over the handling of events for the Back
        // button while the dialog is opened.
        alert.setOnKeyListener(this);
    }

    /**
     * Returns a list containing all of the files and directories
     * that the user has selected using this dialog. If no items
     * have been selected, an empty list will be returned.
     * @return List of selected files, or empty list if nothing was 
     * selected.
     */
    public List<File> getSelectedFiles() {
        synchronized (_selectedItems) {
            return new ArrayList<>(_selectedItems);
        }
    }

    /**
     * Returns a map containing all of the files and directories
     * that the user has selected using this dialog keyed by canonical path. If no items
     * have been selected, an empty list will be returned.
     * @return Map of selected files, or empty map if nothing was
     * selected.
     */
    public Map<String, File> getSelectedFilesMap() {
        HashMap<String, File> map = new HashMap<>(_selectedItems.size());
        synchronized (_selectedItems) {
            for (File file : _selectedItems) {
                try {
                    map.put(file.getCanonicalPath(), file);
                } catch (IOException e) {
                    Log.d(TAG, "Unable to resolve a canonical path", e);
                }
            }
        }
        return map;
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        // Handle the "UP" event for the device's back button. This hijacks the
        // "Cancel" behavior that is normally present in a dialog, and instead
        // treats a press as an indication that the user would like to move
        // up one directory.
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            _navigateUpOneDirectory();
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        ViewHolder holder = getHolder(view);
        if (holder != null) {
            holder.onClick(view);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        ViewHolder holder = getHolder(view);
        return holder != null && holder.onLongClick(view);
    }

    private ViewHolder getHolder(View view) {
        return view.getTag() instanceof ViewHolder
                ? (ViewHolder) view.getTag()
                : null;
    }

    protected class FileItemAdapter extends ArrayAdapter<FileItem> {

        protected final MapView _mapView;
        protected final Context _mapCtx;
        protected final Runnable _notifyDataSetChangedRunnable;

        protected FileItemAdapter(Context layoutContext, int resourceId,
                List<FileItem> items) {
            super(layoutContext, resourceId, items);
            _mapView = MapView.getMapView();
            _mapCtx = _mapView != null ? _mapView.getContext() : layoutContext;
            _notifyDataSetChangedRunnable = new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            };
        }

        public void setAllChecked(final boolean allChecked) {
            selectAllThread.execute(new Runnable() {
                @Override
                public void run() {
                    setAllChecked_impl(allChecked);
                }
            });
        }

        private final Executor selectAllThread = Executors
                .newSingleThreadExecutor(
                        new NamedThreadFactory(
                                "ImportManagerFileBrowserSelectAll"));

        private void setAllChecked_impl(final boolean allChecked) {
            final Map<String, File> selectedFilesMap = getSelectedFilesMap();
            if (allChecked) {
                synchronized (_selectedItems) {
                    for (int i = 0; i < getCount(); i++) {
                        final FileItem item = getItem(i);
                        if (item != null && item.file != null
                                && !item.file.isEmpty()) {
                            final File file = new File(_path, item.file);
                            final File parent = file.getParentFile();
                            boolean added = false;
                            try {
                                //check that parent has not already been added to list
                                //per disabling child checkboxes behavior
                                if (parent != null
                                        && !selectedFilesMap.containsKey(
                                                parent.getCanonicalPath())) {
                                    _selectedItems.add(file);
                                    added = true;
                                } else if (parent == null) {
                                    _selectedItems.add(file);
                                    added = true;
                                }
                                //if we have some child items, from a directory we are adding, already added,
                                //remove them from selected files in accordance with subitem checkbox disabling
                                //if parent is added
                                if (added && file.isDirectory()) {
                                    ImportUtils.removeSubFilesFromSetRecursive(
                                            file, selectedFilesMap,
                                            _selectedItems);
                                }
                            } catch (IOException e) {
                                Log.d(TAG,
                                        "Unable to get a canonical path in allChecked add",
                                        e);
                            } catch (StackOverflowError e) {
                                Log.d(TAG,
                                        "Filesystem is too deep, encountered stack overflow",
                                        e);
                            }
                        }
                    }
                }
            } else {
                File[] currentDirFiles = listFiles(_path, _fileFilter);
                if (currentDirFiles != null) {
                    for (File file : currentDirFiles) {
                        try {
                            File match = selectedFilesMap
                                    .get(file.getCanonicalPath());
                            if (match != null) {
                                synchronized (_selectedItems) {
                                    _selectedItems.remove(match);
                                }
                            }
                        } catch (IOException e) {
                            Log.d(TAG,
                                    "Unable to get a canonical path in allChecked remove",
                                    e);
                        }
                    }
                }
            }
            //run on ui thread
            ImportManagerFileBrowser.this.post(_notifyDataSetChangedRunnable);
        }

        @Override
        @NonNull
        public View getView(int position, View row, @NonNull ViewGroup parent) {
            final FileItem fileItem = getItem(position);

            ViewHolder h = row != null && row.getTag() instanceof ViewHolder
                    ? (ViewHolder) row.getTag()
                    : null;
            if (h == null) {
                h = createViewHolder(parent);
                row = h.root;
                row.setTag(h);
            }
            h.fileItem = fileItem;
            h.selected.setOnClickListener(h);

            if (fileItem == null)
                return row;

            boolean isDirectory = fileItem.type == FileItem.DIRECTORY;
            boolean showCheckbox = _multiSelect
                    && (_directorySelect || !isDirectory);

            // Handle the special case where a directory does not contain any files.
            // This changes the list to simply display an item that says "Directory is Empty"
            // without checkboxes or icons.
            if (_directoryEmpty) {
                h.icon.setVisibility(View.GONE);
                h.fileName
                        .setText(Html.fromHtml("<i>" + fileItem.file + "</i>"));
                h.fileInfo.setText("");
                h.selected.setVisibility(View.GONE);
                return row;
            } else {
                h.icon.setVisibility(View.VISIBLE);
                h.selected
                        .setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
            }

            if (showCheckbox) {
                // If this item has been selected, update the checkbox state,
                // otherwise uncheck it.
                File checkFile = new File(_path, fileItem.file);
                try {
                    checkFile = checkFile.getCanonicalFile();
                } catch (Exception ignore) {
                }
                synchronized (_selectedItems) {
                    h.selected.setChecked(_selectedItems.contains(checkFile));
                }

                // If one of the file's parents has been selected, disable the checkbox
                // so that it can't be toggled by itself. also make the box checked
                // to indicate that the user can't mess with them independent of the
                // parent.
                if (h.isParentSelected()) {
                    h.selected.setChecked(true);
                    h.selected.setEnabled(false);
                } else {
                    h.selected.setEnabled(true);
                }
            }

            File f = new File(_path, fileItem.file);

            Drawable icon = fileItem.iconDr;
            h.icon.setImageDrawable(icon);

            // Load image thumbnail if needed
            if (ImportUserIconSetResolver.IconFilenameFilter.accept(null, f.getName())) {
                icon.setCallback(h.icon);
                loadThumbnail(fileItem);
            }

            h.fileName.setText(f.getName());
            if (isDirectory) {
                // Filter out undesired file types
                h.fileInfo.setText(R.string.dashdash);
                _getFileCount(f, h.fileInfo);
            } else
                h.fileInfo.setText(
                        MathUtils.GetLengthString(ioProvider.length(f)));

            h.modDate.setText(DATE_FORMAT.format(new Date(
                    ioProvider.lastModified(f))));

            return row;
        }
    }

    protected ViewHolder createViewHolder(ViewGroup parent) {
        ViewHolder h = new ViewHolder();
        h.root = LayoutInflater.from(getContext()).inflate(
                R.layout.import_manager_file_browser_fileitem, parent, false);
        h.icon = h.root.findViewById(
                R.id.importManagerBrowserIcon);
        h.fileName = h.root.findViewById(
                R.id.importManagerBrowserFileName);
        h.fileInfo = h.root.findViewById(
                R.id.importManagerBrowserFileInfo);
        h.modDate = h.root.findViewById(R.id.importManagerBrowserDate);
        h.selected = h.root.findViewById(
                R.id.importManagerBrowserFileSelected);
        return h;
    }

    @Override
    protected void _loadFileList() {
        super._loadFileList();
        _fileSort = new FileSort(new FileSort.NameSort());
        for (Button btn : _sortBtns) {
            Pair<String, FileSort> p = (Pair<String, FileSort>) btn.getTag();
            if (p != null)
                btn.setText(p.first);
        }
    }

    public class ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        public View root;
        public ImageView icon;
        public TextView fileName, fileInfo, modDate;
        public CheckBox selected;
        public FileItem fileItem;

        // Check to see if a checkbox is checked or not, and update
        // the selectedItems set accordingly.
        protected void recordCheckBox() {
            if (!_directorySelect && fileItem.type == FileItem.DIRECTORY)
                return;
            boolean isChecked = selected.isChecked();
            try {
                File selectedFile = new File(_path, fileItem.file)
                        .getCanonicalFile();
                synchronized (_selectedItems) {
                    if (isChecked) {
                        // If the newly checked file is the parent of a file that
                        // is already in the selectedItems list, remove that item
                        // from the selectedItems list to avoid importing items
                        // twice.
                        for (Iterator<File> it = _selectedItems.iterator(); it
                                .hasNext();) {
                            File next = it.next();
                            if (isParent(selectedFile, next)) {
                                it.remove();
                            }
                        }
                        _selectedItems.add(selectedFile);
                    } else {
                        _selectedItems.remove(selectedFile);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Couldn't find canonical file.", e);
            }
        }

        protected boolean isParent(File possibleParent, File file) {
            if (!ioProvider.exists(possibleParent)
                    || !ioProvider.isDirectory(possibleParent) ||
                    possibleParent.equals(file)) {
                // this cannot possibly be the parent
                return false;
            }

            File possibleChild = file.getParentFile();
            while (possibleChild != null) {
                if (possibleChild.equals(possibleParent)) {
                    return true;
                }
                possibleChild = possibleChild.getParentFile();
            }

            // No match found, and we've hit the root directory
            return false;
        }

        protected boolean isParentSelected() {
            try {
                File fileToTest = new File(_path, fileItem.file)
                        .getCanonicalFile();
                synchronized (_selectedItems) {
                    for (File f : _selectedItems) {
                        if (isParent(f, fileToTest))
                            return true;
                    }
                }
                return false;
            } catch (IOException e) {
                Log.e(TAG, "Couldn't check for parent.", e);
                return false;
            }
        }

        @Override
        public void onClick(View v) {
            if (fileItem == null || _directoryEmpty)
                return;
            if (v == selected) {
                recordCheckBox();
                return;
            }
            _currFile = fileItem.file;
            File sel = new File(_path, _currFile);
            if (fileItem.type == FileItem.DIRECTORY) {
                if (ioProvider.canRead(sel)) {
                    _pathDirsList.add(_currFile);
                    _path = new File(sel + "");
                    _loadFileList();
                    _adapter.notifyDataSetChanged();
                    _updateCurrentDirectoryTextView();
                    _scrollListToTop();
                }
            } else {
                // Clicking on an item that is not a directory
                // treats the click the same as if the user had clicked
                // on the entry's checkbox.
                onLongClick(root);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (fileItem == null || _directoryEmpty
                    || !_directorySelect && fileItem.type == FileItem.DIRECTORY)
                return false;
            _currFile = fileItem.file;
            if (!_multiSelect) {
                File sel = new File(_path, _currFile);
                _returnFile(sel);
            } else if (selected != null) {
                selected.toggle();
                recordCheckBox();
            }
            return true;
        }
    }

    private void setupSelectAllButton(@NonNull
    final ImageButton btn) {
        _selectAllBtn = btn;
        btn.setOnClickListener(_selectAllChkListener);
    }

    private void setupDeselectAllBtn(@NonNull
    final ImageButton btn) {
        _deselectAllBtn = btn;
        btn.setOnClickListener(_deselectAllChkListener);
    }

    private void setupSortButton(int id) {
        final Button sortBtn = findViewById(id);
        if (sortBtn == null)
            return;
        Comparator<File> comp;
        if (id == R.id.sort_name)
            comp = new FileSort.NameSort();
        else if (id == R.id.sort_date)
            comp = new FileSort.DateSort(ioProvider);
        else if (id == R.id.sort_size)
            comp = new FileSort.SizeSort(ioProvider);
        else if (id == R.id.sort_checked)
            comp = _sortChecked;
        else
            return;
        _sortBtns.add(sortBtn);
        sortBtn.setTag(new Pair<>(sortBtn.getText(), new FileSort(comp)));
        sortBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Pair<String, FileSort> p = (Pair<String, FileSort>) v.getTag();
                _fileSort = p.second;
                _fileSort.setAscending(!_fileSort.isAscending());
                sortBtn.setText(p.first
                        + (_fileSort.isAscending() ? '\u25B2' : '\u25BC'));
                if (_adapter != null)
                    _adapter.sort(new Comparator<FileItem>() {
                        @Override
                        public int compare(FileItem f1, FileItem f2) {
                            int dirComp = Boolean.compare(
                                    f1.type == FileItem.DIRECTORY,
                                    f2.type == FileItem.DIRECTORY);
                            if (dirComp != 0)
                                return dirComp;
                            return _fileSort.compare(new File(_path, f1.file),
                                    new File(_path, f2.file));
                        }
                    });
                for (Button btn : _sortBtns) {
                    p = (Pair<String, FileSort>) btn.getTag();
                    if (p != null && btn != sortBtn) {
                        p.second.setAscending(true);
                        btn.setText(p.first);
                    }
                }
            }
        });
    }
}
