package com.atakmap.map.layer.feature.style;

import com.atakmap.interop.Pointer;

/**
 * Mesh style for point geometries. Renders the associated point geometry
 * using the specified mesh.
 */
public final class MeshPointStyle extends Style
{
    /**
     * Creates a new mesh style with the specified properties.
     *
     * @param meshUri          The mesh URI
     * @param color            The mesh color
     * @param transform        The local mesh transform
     */
    public MeshPointStyle(String meshUri, int color, float[] transform)
    {
        this(MeshPointStyle_create(meshUri, color, transform), null);
    }

    MeshPointStyle(Pointer pointer, Object owner)
    {
        super(pointer, owner);
    }

    /**
     * Returns the URI for the mesh.
     *
     * @return the mesh uri
     */
    public String getMeshUri()
    {
        this.rwlock.acquireRead();
        try
        {
            return MeshPointStyle_getUri(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the color to be modulated with the mesh. Each pixel value in the
     * mesh is multiplied by the returned color; a value of
     * <code>0xFFFFFFFF</code> should be used to render the mesh using its
     * original color.
     *
     * @return The mesh color.
     */
    public int getColor()
    {
        this.rwlock.acquireRead();
        try
        {
            return MeshPointStyle_getColor(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the mesh transform.
     *
     * @return The mesh transform
     */
    public float[] getTransform()
    {
        this.rwlock.acquireRead();
        try
        {
            return MeshPointStyle_getTransform(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }
} // MeshPointStyle
