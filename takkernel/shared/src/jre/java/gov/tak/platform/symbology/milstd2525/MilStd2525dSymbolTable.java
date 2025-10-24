package gov.tak.platform.symbology.milstd2525;

import armyc2.c5isr.renderer.utilities.MSLookup;
import gov.tak.platform.commons.resources.JavaResourceManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Stack;

final class MilStd2525dSymbolTable extends MilStd2525dSymbolTableBase
{
    final static Interop<Node> nodeInterop = new Interop<Node>() {
        @Override
        public String getName(Node node) { return node.getName(); }

        @Override
        public String getSymbolSetCode(Node node) { return node.getSymbolSetCode(); }

        @Override
        public String getCode(Node node) { return node.getCode(); }

        @Override
        public Collection<Node> getChildren(Node node) { return node.getChildren(); }
    };

    MilStd2525dSymbolTable(int version) {
        super(version);
    }

    @Override
    void initRoot() {
        if(symbolSummary.isEmpty())
            parseDescriptions(new JavaResourceManager());

        TreeManager tree = new TreeManager();
        try {
            tree.buildTree(MSLookup.class.getResourceAsStream("/data/msd.txt"));
            Node r = tree.mil2525Tree;
            for(Node n : r.getChildren())
                push(root, n, new Stack<>(), version, interop, nodeInterop);
        } catch(IOException ignored) {}
    }
}
