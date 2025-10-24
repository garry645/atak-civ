package gov.tak.platform.symbology.milstd2525;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import armyc2.c5isr.renderer.utilities.MSInfo;

public class TreeManager {
    private static final Set<String> SYMBOL_BLACKLIST = new HashSet(Arrays.asList("25342900", "25343000", "25343100", "25343200", "25343300", "25343400", "25343500", "25343600", "25343700", "25343800", "25343900", "25344000", "25350000", "25350100", "25350101", "25350102", "25350103", "25350200", "25350201", "25350202", "25350203", "46120313", "46120301", "46120325", "46120400", "47", "10163601", "45162004"));
    public Node mil2525Tree;

    public TreeManager() {
    }

    public void buildTree(InputStream in) throws IOException {
        //InputStream in = context.getResources().openRawResource(raw.msd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        this.mil2525Tree = new Node("Root", "XX", "XX");
        Stack<Node> parentStack = new Stack();
        Node child = this.mil2525Tree;
        String symbolSet = "";

        while(true) {
            String line;
            int nodeDepth;
            String[] segments;
            label64:
            do {
                while((line = reader.readLine()) != null) {
                    for(nodeDepth = 1; line.charAt(0) == '\t'; ++nodeDepth) {
                        line = line.substring(1);
                    }

                    if (nodeDepth > parentStack.size()) {
                        parentStack.push(child);
                    }

                    while(nodeDepth < parentStack.size()) {
                        parentStack.pop();
                    }

                    if (nodeDepth != 1) {
                        continue label64;
                    }

                    segments = line.split("\\t");
                    symbolSet = segments[0];
                    if (!SYMBOL_BLACKLIST.contains(symbolSet)) {
                        child = new Node(MSInfo.parseSymbolSetName(symbolSet, 11), "000000", symbolSet);
                        ((Node)parentStack.peek()).addChild(child);
                        if (!segments[1].equals("Unspecified")) {
                            parentStack.push(child);
                            continue label64;
                        }
                    }
                }

                reader.close();
                return;
            } while(line.toLowerCase().contains("{reserved for future use}"));

            segments = line.split("\t+");
            String name;
            if (nodeDepth == 1) {
                name = segments[1];
            } else {
                name = segments[0];
            }

            String code = "XXXXXX";

            for(int i = 1; i < segments.length; ++i) {
                if (segments[i].matches("\\d{6}")) {
                    code = segments[i];
                    break;
                }
            }

            if (!SYMBOL_BLACKLIST.contains(symbolSet) && !SYMBOL_BLACKLIST.contains(symbolSet + code)) {
                child = new Node(name, code, symbolSet);
                ((Node)parentStack.peek()).addChild(child);
            }
        }
    }
}
