
package com.atakmap.android.cotselector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atakmap.android.drawing.milsym.MilSym;
import com.atakmap.android.icons.CotDescriptions;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.platform.symbology.SymbologyProvider;

class MilStd2525CAdapter extends ArrayAdapter<String> {

    private final CustomListView clv;
    private final Context context;
    public final String requires;
    public String path = null;
    public ISymbolTable.Folder folder;
    public ISymbolTable.Symbol symbol;
    private MilStd2525CAdapter parent;

    private String _currentType = "undefined";


    final private ArrayList<MilStd2525CAdapter> children = new ArrayList<>();

    public void setParent(MilStd2525CAdapter parent) {
        this.parent = parent;
    }

    public void addChild(MilStd2525CAdapter child) {
        children.add(child);
    }

    public MilStd2525CAdapter getParent() {
        return parent;
    }

    public Collection<MilStd2525CAdapter> getChildren() {
        return children;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    MilStd2525CAdapter(Context context, int textViewResourceId,
                       CustomListView clv,
                       ArrayList<String> objects,
                       String requires, MilStd2525CAdapter prev,
                       ISymbolTable.Folder folder) {
        super(context, textViewResourceId, objects);
        this.context = context;
        this.clv = clv;
        this.folder = folder;
        this.parent = prev;
        this.requires = requires;
    }

    public void setType(String type) {
        _currentType = type;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView,
            @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater li = LayoutInflater.from(context);
            convertView = li.inflate(R.layout.a2525listitem, null, true);
            viewHolder = new ViewHolder();
            viewHolder.current = convertView.findViewById(R.id.button1);
            viewHolder.lower = convertView.findViewById(R.id.button2);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String item =this.getItem(position);

        if (item != null) {
            String desc;
            ISymbolTable.Symbol findsym = null;
            MilStd2525CAdapter findnextAdapter = null;

            if (folder != null) {
                for (ISymbolTable.Symbol s : folder.getSymbols()) {
                    if (s.getName().equalsIgnoreCase(item)) {
                        findsym = s;
                        break;
                    }
                }
            }

            for(MilStd2525CAdapter adapter : children) {
                if(adapter.folder != null && adapter.folder.getName().equalsIgnoreCase(item)) {
                    findnextAdapter = adapter;
                    break;
                }
            }


            final MilStd2525CAdapter nextAdapter = findnextAdapter;
            desc = MilSym.getTranslatedName(findsym == null ? nextAdapter.folder : findsym);

            String findaffil = "";

            Affiliation defaultSymbolAffil = null;

            final String lcItemName = item.toLowerCase(Locale.US);
            switch (lcItemName) {
                case "friendly":
                    findaffil = "f";
                    defaultSymbolAffil = Affiliation.Friend;
                    break;
                case "hostile":
                    findaffil = "h";
                    defaultSymbolAffil = Affiliation.Hostile;
                    break;
                case "neutral":
                    findaffil = "n";
                    defaultSymbolAffil = Affiliation.Neutral;
                    break;
                case "unknown":
                    findaffil = "u";
                    defaultSymbolAffil = Affiliation.Unknown;
                    break;
            }

            final ISymbolTable.Symbol sym = findsym;
            final String symCode = (sym == null ? "" : findsym.getCode());

            final String affil = findaffil;
            SpannableString spanString = new SpannableString(desc);
            if (sym != null && compareSymbols(sym.getCode(), _currentType)) {
                viewHolder.current.setText("*" + spanString + "*");
            } else {
                viewHolder.current.setText(spanString);
            }

                OnClickListener onClickListener = null;
            if (sym != null && !requires.equals("#REQ:ROOT")) {
                onClickListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clv.sendCoTFrom2525(sym.getCode());
                    }

                };
            } else if (nextAdapter != null) {
                onClickListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clv.goDeeper(nextAdapter); //if it's the root level, we can always go deeper
                        if (!affil.isEmpty())
                            changeAffil(affil);
                    }
                };
            } else if (!requires.equals("#REQ:ROOT")) {
                onClickListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeAffil(affil);
                        clv.goDeeper(nextAdapter); //if it's the root level, we can always go deeper
                    }
                };
            }
            viewHolder.current.setOnClickListener(onClickListener);
            viewHolder.current
                    .setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            if (FileSystemUtils.isEmpty(symCode))
                                return false;

                            StringBuilder path = new StringBuilder();
                            for (String s : CotDescriptions
                                    .getDescriptionPath(context, symCode,
                                            -1)) {
                                if (path.length() > 0)
                                    path.append("--> ");
                                path.append(s);
                            }
                            AlertDialog.Builder builder = new AlertDialog.Builder(
                                    context);
                            builder.setTitle(R.string.more_details);
                            View v = LayoutInflater.from(context)
                                    .inflate(R.layout.a2525diag, null);
                            TextView tv = v.findViewById(R.id.text);
                            ImageView iv = v.findViewById(R.id.icon);

                            final BitmapDrawable bmd = clv
                                    .requestLoadIcon(symCode + ".png");
                            iv.setImageDrawable(bmd);
                            tv.setText(
                                    "2525C: " + symCode
                                            + "\n"
                                            +
                                            "English Description: "
                                            + requires
                                            + "\n" +
                                            "Translated Description: "
                                            + CotDescriptions
                                                    .getDescription(
                                                            context,
                                                            symCode)
                                            + "\n" +
                                            "Whole Path: \n" + path);
                            builder.setView(v);
                            builder.setPositiveButton(R.string.ok, null);
                            builder.show();
                            return false;
                        }
                    });


            // the preview icon is the affiliated basic ground symbol for the top-level Friendly,
            // Neutral, Hostile, Unknown or the symbol preview if symbol is available for the
            // hierarchy node
            final String previewIcon = (sym == null && defaultSymbolAffil != null) ?
                    SymbologyProvider.setAffiliation("SUG------------", defaultSymbolAffil) :
                    symCode;
            // purposely running instead of start in order to avoid race conditions with the
            // adapter.
            //noinspection CallToThreadRun
            new RqstLoadImgThread(previewIcon + ".png", viewHolder.current).run();

            if (nextAdapter != null) {
                viewHolder.lower.setVisibility(View.VISIBLE);// since it could be being adapted, make sure
                // it's visible
                viewHolder.lower.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        viewHolder.current.setCompoundDrawables(null, null,
                                null, null);
                        clv.goDeeper(nextAdapter);
                        if (!affil.isEmpty()) {
                            changeAffil(affil);
                        }
                    }

                });
            } else {
                viewHolder.lower.setVisibility(View.INVISIBLE);
            }

        }
        return convertView;
    }

    private static boolean compareSymbols(String s1, String s2) {
        final String a = CotDescriptions.normalize(s1);
        final String b = CotDescriptions.normalize(s2);
        return a.equals(b);
    }

    static class ViewHolder {
        Button current;
        ImageButton lower;
    }

    private void changeAffil(final String affil) {

        final Affiliation result;
        switch (affil) {
            case "f":
                result = Affiliation.Friend;
                break;
            case "n":
                result = Affiliation.Neutral;
                break;
            case "h":
                result = Affiliation.Hostile;
                break;
            case "u":
            default:
                result = Affiliation.Unknown;
                break;
        }
        clv.setSelectedAffil(result);
    }

    private class RqstLoadImgThread extends Thread {

        private final String fn;
        private final Button b;

        RqstLoadImgThread(String fn, Button b) {
            this.fn = fn;
            this.b = b;
        }

        @Override
        public void run() {
            final BitmapDrawable bmd = clv.requestLoadIcon(fn);

            if (bmd != null) {
                Activity a = (Activity) context;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        b.setCompoundDrawablesWithIntrinsicBounds(bmd, null,
                                null, null);
                    }
                });
            }
        }
    }
}
