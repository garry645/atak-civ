package gov.tak.platform.symbology.milstd2525;

import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class Node {
    private final String name;
    private final String code;
    private final String symbolSetCode;
    private final ArrayList<Node> children;

    public Node(String name, String code, String symbolSetCode) {
        this.name = name;
        this.code = code;
        this.symbolSetCode = symbolSetCode;
        this.children = new ArrayList();
    }

    public void addChild(Node node) {
        this.children.add(node);
    }

    public void addChildren(List<Node> nodes) {
        this.children.addAll(nodes);
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public String getSymbolSetCode() {
        return this.symbolSetCode;
    }

    public ArrayList<Node> getChildren() {
        return this.children;
    }

    public ArrayList<Node> flatten() {
        ArrayList<Node> result = new ArrayList();
        Iterator var2 = this.children.iterator();

        while(var2.hasNext()) {
            Node child = (Node)var2.next();
            result.add(child);
            result.addAll(child.flatten());
        }

        return result;
    }

    public int getSize() {
        int size = 1;

        Node n;
        for(Iterator var2 = this.children.iterator(); var2.hasNext(); size += n.getSize()) {
            n = (Node)var2.next();
        }

        return size;
    }

    public void logTree(int depth) {
        StringBuilder indent = new StringBuilder();

        for(int i = 0; i < depth; ++i) {
            indent.append("\t");
        }

        Log.d("Node", indent + this.name + "\t" + this.code);
        Iterator var5 = this.children.iterator();

        while(var5.hasNext()) {
            Node n = (Node)var5.next();
            n.logTree(depth + 1);
        }

    }
}
