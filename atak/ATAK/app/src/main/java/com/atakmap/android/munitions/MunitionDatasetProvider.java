package com.atakmap.android.munitions;

import android.content.Context;

import com.atakmap.android.databridge.Dataset;
import com.atakmap.android.databridge.DatasetDefinition;
import com.atakmap.android.databridge.DatasetElement;
import com.atakmap.android.databridge.DatasetProvider;
import com.atakmap.android.databridge.DatasetProviderCallback;
import com.atakmap.android.databridge.DatasetQueryParam;
import gov.tak.platform.lang.Parsers;
import com.atakmap.app.system.FlavorProvider;
import com.atakmap.app.system.SystemComponentLoader;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.xml.XMLUtils;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.util.Disposable;

class MunitionDatasetProvider implements DatasetProvider, Disposable {

    private final String TAG = "MunitionDatasetProvider";
    private final String UID = "ada5be38-9d4c-4d67-a731-7741d2a9410d";
    private static final String DIRNAME = FileSystemUtils.TOOL_DATA_DIRECTORY
            + File.separatorChar + "fires";

    private static final String ORDNANCE_XML = "ordnance/ordnance_table.xml";

    final List<Dataset> weapons = new ArrayList<>();

    private final DatasetDefinition definition;

    private final Context context;

    private static MunitionDatasetProvider _instance;


    static MunitionDatasetProvider getInstance() {
        return _instance;
    }

    MunitionDatasetProvider(Context c) {
        context = c;

        definition = new DatasetDefinition(UID, "munition data v1");
        definition.addDataElement(new DatasetElement("group", "the group for the weapon",
                DatasetElement.Type.String, true));
        definition.addDataElement(new DatasetElement("category", "the category for the weapon",
                DatasetElement.Type.String, true));
        definition.addDataElement(new DatasetElement("weapon_id", "the weapon id",
                DatasetElement.Type.String, false));
        definition.addDataElement(new DatasetElement("weapon_name", "the weapon name",
                DatasetElement.Type.String, false));
        definition.addDataElement(new DatasetElement("weapon_description", "the weapon description",
                DatasetElement.Type.String, false));
        definition.addDataElement(new DatasetElement("weapon_standing", "the weapon radius in meters to cause injury or death for a person in a standing position",
                DatasetElement.Type.Integer, false));
        definition.addDataElement(new DatasetElement("weapon_prone", "the weapon radius in meters to cause injury or death for a person in prone position",
                DatasetElement.Type.Integer, false));
        definition.addDataElement(new DatasetElement("weapon_proneprotected", "the weapon radius in meters to cause injury or death for a person in a prone and protected position",
                DatasetElement.Type.Integer, false));
        definition.addDataElement(new DatasetElement("weapon_ricochetfan_angle", "the weapon ricochet fan degrees",
                DatasetElement.Type.Integer, false));
        definition.addDataElement(new DatasetElement("weapon_ricochetfan_distance", "the weapon ricochet fan meters",
                DatasetElement.Type.Integer, false));
        populate(c);
        loadCustoms();

        _instance = this;

    }


    synchronized void remove(@NonNull String weapon_id) {
        for (int i = 0; i < weapons.size(); ++i) {
            String id = weapons.get(i).get("weapon_id", (String)null);
            if (Objects.equals(id, weapon_id)) {
                weapons.remove(i);
                return;
            }
        }
    }

    /**
     * Notify that a new weapon has been added
     * @param group the group for the new weapon
     * @param category the category
     * @param weapon_id the id
     * @param weapon_name the weapon name
     * @param weapon_desc the weapon description
     * @param weapon_standing the RED or MSD in meters (-1 to ignore)
     * @param weapon_prone the RED or MSD in meters (-1 to ignore)
     * @param weapon_proneprotected the RED or MSD in meters (-1 to ignore)
     * @param weapon_ricochetfan_angle the RED or MSD in meters (-1 to ignore)
     * @param weapon_ricochetfan_distance the RED or MSD in meters (-1 to ignore)
     */
    synchronized void add(String group, String category, @NonNull String weapon_id,
                          String weapon_name, String weapon_desc,
                          int weapon_standing, int weapon_prone, int weapon_proneprotected,
                          int weapon_ricochetfan_angle, int weapon_ricochetfan_distance) {

        remove(weapon_id);
        Dataset ds = new Dataset(getUID());
        if (group != null)
            ds.set("group", group);
        if (category != null)
            ds.set("category", category);

        ds.set("weapon_id", weapon_id);

        if (weapon_name != null)
            ds.set("weapon_name", weapon_name);

        if (weapon_desc != null)
            ds.set("weapon_description", weapon_desc);

        if (weapon_standing >= 0)
            ds.set("weapon_standing", weapon_standing);

        if (weapon_prone >= 0)
            ds.set("weapon_prone", weapon_prone);

        if (weapon_proneprotected >= 0)
            ds.set("weapon_proneprotected", weapon_proneprotected);

        if (weapon_ricochetfan_angle >= 0)
            ds.set("weapon_ricochetfan_angle", weapon_ricochetfan_angle);

        if (weapon_ricochetfan_distance >= 0)
            ds.set("weapon_ricochetfan_distance", weapon_ricochetfan_distance);

        ds.seal();
        weapons.add(ds);
    }

    @Override
    public String getUID() {
        return UID;
    }

    @Override
    public String getName() {
        return "Default Ordnance";
    }

    @Override
    public String getDescription() {
        return "Provides the built in information that is used with the REDs and MSD screens";
    }

    @Override
    public String getPackageName() {
        return context.getPackageName();
    }

    @Override
    public List<DatasetDefinition> getDefinitions() {
        return Collections.singletonList(definition);
    }

    @Override
    public void subscribe(String tag, List<DatasetQueryParam> query, DatasetProviderCallback dataProviderCallback) {
        List<Dataset> retval;
        synchronized (this) {
            if (query.isEmpty()) {
                retval = new ArrayList<>(weapons);
            } else {
                Set<Dataset> returnSet = new HashSet<>(weapons);

                for (DatasetQueryParam q : query) {
                    Iterator<Dataset> i = returnSet.iterator();
                    while (i.hasNext()) {
                        Dataset next = i.next();
                        String val = next.get(q.key, (String) null);
                        if (q.operation == DatasetQueryParam.Operation.EQUALS) {
                            if (!Objects.equals(val, q.value)) {
                                i.remove();
                            }
                        } else if (q.operation == DatasetQueryParam.Operation.NOT_EQUALS) {
                            if (Objects.equals(val, q.value)) {
                                i.remove();
                            }
                        }
                    }


                }
                retval = new ArrayList<>(returnSet);
            }
        }
        dataProviderCallback.onData(tag, this, query, retval,
                DatasetProviderCallback.Status.COMPLETE, "success");

    }

    @Override
    public void unsubscribe(DatasetProviderCallback dataProviderCallback) {

    }

    @Override
    public void dispose() {

    }

    private void loadCustoms() {
        String location = FileSystemUtils.getItem(DIRNAME).getPath()
                + File.separator;
        String item = "customs.xml";


        Log.d(TAG, "load customs: " + location + item);
        File f = new File(location + item);
        InputStream is = null;
        if (IOProviderFactory.exists(f)) {
            try {
                is = IOProviderFactory.getInputStream(f);
                DocumentBuilderFactory docFactory = XMLUtils
                        .getDocumenBuilderFactory();
                DocumentBuilder docBuilder = docFactory
                        .newDocumentBuilder();
                Document dom = docBuilder.parse(is);


                if (dom != null) {
                    Node munitions = dom.getFirstChild();
                    if (!munitions.getNodeName().equals("Custom_Threat_Rings"))
                        return;

                    NodeList nodes = munitions.getChildNodes();
                    for (int i = 0; i < nodes.getLength();++i) {
                        Node n = nodes.item(i);
                        if (n.getNodeName().equals("weapon")) {
                            populateEntries(n, null, null);
                        } else{
                            populateEntries(n, n.getNodeName(), null);
                        }
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                Log.e(TAG, "error: ", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }

    }

    private void populate(Context context) {
        InputStream is = null;
        try {
            DocumentBuilderFactory docFactory = XMLUtils
                    .getDocumenBuilderFactory();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // see if the flavor supplies the ordnance tables.
            FlavorProvider provider = SystemComponentLoader.getFlavorProvider();
            if (provider != null) {
                is = provider.getAssetInputStream(ORDNANCE_XML);
            }

            // if the flavor does not, then just grab the localized.
            if (is == null) {
                is = context.getAssets().open(ORDNANCE_XML);
            }

            Document dom = docBuilder.parse(is);
            // first node is the munitions node
            Node munitions = dom.getFirstChild();
            if (!munitions.getNodeName().equals("munitions"))
                return;

            NodeList nodes = munitions.getChildNodes();
            for (int i = 0; i < nodes.getLength();++i) {
                Node n = nodes.item(i);
                NodeList subnodes = n.getChildNodes();
                for (int j = 0; j < subnodes.getLength();++j) {
                    Node sn = subnodes.item(j);
                    populateEntries(sn, n.getNodeName(), null);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.e(TAG, "error: ", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private void populateEntries(Node n, String group, String category) {
        if (n.getNodeName().equals("category")) {
            NodeList children = n.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                Node subnode = children.item(i);
                if (subnode.getNodeName().equals("weapon"))
                    populateEntries(subnode, group, n.getAttributes().getNamedItem("name").getTextContent());
            }
        } else if (n.getNodeName().equals("weapon")) {
            NamedNodeMap attributes = n.getAttributes();
            add(group, category,
                    attributes.getNamedItem("ID").getTextContent(),
                    attributes.getNamedItem("name").getTextContent(),
                    attributes.getNamedItem("description").getTextContent(),
                    getOrDefault(attributes, "standing", -1),
                    getOrDefault(attributes, "prone", -1),
                    getOrDefault(attributes, "proneprotected", -1),
                    getOrDefault(attributes, "ricochetfan_angle", -1),
                    getOrDefault(attributes, "ricochetfan_distance", -1));
        }
    }
    private int getOrDefault(NamedNodeMap attributes, String name, int defVal) {
        Node attribute = attributes.getNamedItem(name);
        if (attribute != null)
            return Parsers.parseInt(attribute.getTextContent(), defVal);
        return defVal;
    }

}
