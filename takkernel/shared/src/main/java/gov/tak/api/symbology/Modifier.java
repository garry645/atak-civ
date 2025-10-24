package gov.tak.api.symbology;

public final class Modifier {
    final String id;
    final String[] name;
    final Class<?> parsedValueType;

    final int numValueFields;

    public Modifier(String id, String name) {
        this(id, new String[] {name}, String.class);
    }

    public Modifier(String id, String[] name, Class<?> parsedValueType) {
        this.id = id;
        this.name = name;
        this.parsedValueType = parsedValueType;
        this.numValueFields = name.length;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name[0];
    }

    public String getName(int fieldIndex) {
        return name[fieldIndex];
    }

    public Class<?> getParsedValueType() {
        return parsedValueType;
    }

    public int getNumFields() {
        return numValueFields;
    }
}
