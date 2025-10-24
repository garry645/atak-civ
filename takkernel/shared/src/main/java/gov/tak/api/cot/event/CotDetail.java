package gov.tak.api.cot.event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.cot.CotFormatting;

/**
 * Data extracted from or to be composed into a Cursor-on-Target detail element.
 * This manages a set of attributes (name/value pairs encoded/decoded as XML element attributes, and a list of child
 * CotDetail elements or a text value.
 *
 * @since 6.0.0
 */
public class CotDetail
{
    public static final String DETAIL = "detail";

    private final String elementName;

    private final Map<String, String> attributes = new ConcurrentHashMap<>();
    private final List<CotDetail> children = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, CotDetail> firstNodeCache = new ConcurrentHashMap<>();

    private volatile String innerText;

    /**
     * Create a default detail tag
     */
    public CotDetail()
    {
        this(DETAIL);
    }

    /**
     * Copy Constructor.  Note that this is a shallow copy, the source's children are copied by reference.
     */
    public CotDetail(final CotDetail source)
    {
        elementName = source.getElementName();
        innerText = source.getInnerText();
        attributes.putAll(source.attributes);

        children.addAll(source.children);
    }

    /**
     * Create a detail tag with the given element name.
     *
     * @param elementName the element name
     */
    public CotDetail(String elementName)
    {
        if (isInvalidXmlName(elementName)) throw new IllegalArgumentException("invalid name: '" + elementName + "'");

        this.elementName = elementName;
    }

    /**
     * Create a detail element with the given name, setting an initial attribute value.
     *
     * @param elementName    the element name
     * @param attributeName  Name of attribute to set
     * @param attributeValue Value of attribute to set
     * @see #setAttribute(String, String)
     */
    public CotDetail(String elementName, String attributeName, String attributeValue)
    {
        this(elementName);
        setAttribute(attributeName, attributeValue);
    }

    /**
     * Create a detail element with the given name, setting the inner text value.
     *
     * @param elementName the element name
     * @param innerText   The inner text to set
     * @see #setInnerText(String)
     */
    public CotDetail(String elementName, String innerText)
    {
        this(elementName);
        setInnerText(innerText);
    }

    /**
     * @return the element name
     */
    public String getElementName()
    {
        return elementName;
    }

    /**
     * Get the inner text of the tag if any. This does not return XML representation of sub tags.
     * Inner-text and sub-tags are mutually exclusive.
     *
     * @return the inner text of the tag
     */
    public String getInnerText()
    {
        return innerText;
    }

    /**
     * Set the inner text of this tag. Since sub tags and inner text are mutually exclusive, calling
     * this removes any sub detail tags.
     *
     * @param text Text to use instead of child elements
     */
    public void setInnerText(final String text)
    {
        synchronized (children)
        {
            firstNodeCache.clear(); // No children, clear the cache.
            children.clear();
            innerText = text;
        }
    }

    /**
     * Test if a given attribute has been set.
     *
     * @param name attribute name
     * @return {@code true} if an attribute of that name has been set
     */
    public boolean hasAttribute(String name)
    {
        return attributes.containsKey(name);
    }

    /**
     * Get an attribute value by name.
     *
     * @param name attribute name
     * @return the attribute
     */
    @Nullable
    public String getAttribute(@NonNull String name)
    {
        return attributes.get(name);
    }

    /**
     * Get an attribute value by name, returning the provided default if the attribute is {@code null}.
     *
     * @param name         attribute name
     * @param defaultValue The default value to use if the attribute is {@code null}
     * @return the attribute (or default value if {@code null})
     */
    @NonNull
    public String getAttribute(@NonNull String name, @NonNull String defaultValue)
    {
        String attributeValue = attributes.get(name);
        return attributeValue == null ? defaultValue : attributeValue;
    }

    /**
     * Set an attribute. There is no need to do any special escaping of the value. When an XML
     * string is built, escaping of illegal characters is done automatically.
     *
     * @param name  the attribute name
     * @param value the value
     */
    public void setAttribute(String name, String value)
    {
        if (value != null)
        {
            if (isInvalidXmlName(name)) throw new IllegalArgumentException("invalid attribute name: '" + name + "'");
            attributes.put(name, value);
        }
    }

    /**
     * Remove an attribute.
     *
     * @param name the attribute name (can be anything)
     * @return get the value removed if any
     */
    public String removeAttribute(final String name)
    {
        return attributes.remove(name);
    }

    /**
     * Remove all attributes
     */
    public void clearAttributes()
    {
        attributes.clear();
    }

    /**
     * @return the number of attributes in a tag
     */
    public int getAttributeCount()
    {
        return attributes.size();
    }

    /**
     * Get an array of the immutable attributes of the detail tag.
     *
     * @return get a copy of the current attributes.
     */
    public CotAttribute[] getAttributes()
    {
        CotAttribute[] attrs = new CotAttribute[attributes.size()];
        Set<Map.Entry<String, String>> entries = attributes.entrySet();
        Iterator<Map.Entry<String, String>> it = entries.iterator();
        int index = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            attrs[index++] = new CotAttribute(entry.getKey(), entry.getValue());
        }
        return attrs;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        buildXml(sb);
        return sb.toString();
    }

    /**
     * The number of sub tags in this detail tag
     *
     * @return the number of sub tags
     */
    public int childCount()
    {
        return children.size();
    }

    /**
     * Get a child detail tag at a given index or {@code null} if the index is invalid at the time of the call.
     *
     * @param index Index of child to retrieve
     * @return the child  at the provided index
     */
    public CotDetail getChild(final int index)
    {
        try
        {
            return children.get(index);
        } catch (IndexOutOfBoundsException e)
        {
            return null;
        }
    }

    /**
     * Get a list of this detail's children. This is much faster than calling {@link #getChild(int)} in a loop.
     *
     * @return List of child nodes
     */
    public List<CotDetail> getChildren()
    {
        synchronized (children)
        {
            return new ArrayList<>(children);
        }
    }

    /**
     * Get all children details with a matching element name
     *
     * @param name Element name
     * @return List of child nodes
     */
    public List<CotDetail> getChildrenByName(String name)
    {
        final List<CotDetail> matchingChildren = new ArrayList<>();

        synchronized (children)
        {
            for (CotDetail detail : children)
            {
                if (name.equals(detail.getElementName())) matchingChildren.add(detail);
            }
        }
        return matchingChildren;
    }

    /**
     * Add a sub detail tag. Since sub tags and inner text are mutually exclusive, this will clear any inner text.
     *
     * @param detail Detail child to add
     * @return The child added
     */
    public CotDetail addChild(@NonNull CotDetail detail)
    {
        innerText = null;
        children.add(detail);

        return detail;
    }

    /**
     * Remove a child.
     */
    public void removeChild(@NonNull final CotDetail detail)
    {
        synchronized (children)
        {
            firstNodeCache.remove(detail.elementName); // children are changing, remove this detail from the cache
            children.remove(detail);
        }
    }

    /**
     * Get the first child element of a given name starting at index 0, returning {@code null} if no child with the
     * given name is found, or the startIndex is out of bounds.
     *
     * @param childElementName element name of interest
     * @return the first child with the child element name, or {@code null} if not found
     */
    public CotDetail getFirstChildByName(String childElementName)
    {
        return getFirstChildByName(0, childElementName);
    }

    /**
     * Get the first child element of a given name starting at the given index, returning {@code null} if no child
     * with the given name is found, or the startIndex is out of bounds.
     *
     * @param startIndex       the start index
     * @param childElementName element name of interest
     * @return the first child with the child element name, or {@code null} if not found
     */
    public CotDetail getFirstChildByName(int startIndex, String childElementName)
    {
        CotDetail cd;

        synchronized (children)
        {
            if (startIndex == 0)
            {
                // check the cache for the first node
                cd = firstNodeCache.get(childElementName);
                if (cd != null)
                {
                    return cd;
                }
            }
            for (int i = startIndex; i < children.size(); ++i)
            {
                cd = getChild(i); // includes protection for an index out of bound exception
                if (cd != null && cd.getElementName().equals(childElementName))
                {
                    if (startIndex == 0)
                    {
                        // populate the cache for the first found node.
                        firstNodeCache.put(childElementName, cd);
                    }
                    return cd;
                }
            }
        }
        return null;
    }

    public void buildXml(final StringBuffer b)
    {
        try
        {
            buildXmlImpl(b);
        } catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    public void buildXml(final StringBuilder b)
    {
        try
        {
            buildXmlImpl(b);
        } catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Build the XML representation of the detail tag
     *
     * @param b the appendable to use when generating the xml.
     */
    public void buildXml(final Appendable b) throws IOException
    {
        buildXmlImpl(b);
    }

    private void buildXmlImpl(final Appendable b) throws IOException
    {
        b.append("<").append(elementName);

        for (Map.Entry<String, String> entry : attributes.entrySet())
        {
            CotFormatting.appendAttribute(entry.getKey(), entry.getValue(), b);
        }

        final boolean hasInnerText = innerText != null;

        if (hasInnerText || !children.isEmpty())
        {
            b.append(">");

            if (hasInnerText)
            {
                b.append(CotFormatting.escapeXmlText(innerText));
            } else
            {
                synchronized (children)
                {
                    for (CotDetail child : children) child.buildXml(b);
                }
            }

            b.append("</").append(elementName).append(">");
        } else
        {
            b.append("/>");
        }
    }

    /**
     * Determine if the given name is an illegal XML element or attribute name,
     *
     * @param name Name to validate
     * @return false if the name is invalid
     */
    private boolean isInvalidXmlName(final String name)
    {
        if (name == null || name.isEmpty()) return true;

        if (name.toLowerCase().startsWith("xml")) return true;

        final char firstChar = name.charAt(0);

        if (!(Character.isLetter(firstChar) || firstChar == '_' || firstChar == ':')) return true;

        for (int i = 1, length = name.length(); i < length; i++)
        {
            final char c = name.charAt(i);
            final boolean valid = c == '_' || c == ':' || c == '-' || c == '.' || Character.isLetter(c) || Character.isDigit(c);
            if (!valid) return true;
        }

        return false;
    }
}
