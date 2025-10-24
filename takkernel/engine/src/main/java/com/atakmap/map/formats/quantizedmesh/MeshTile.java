/*******************************************************************************
 * Copyright 2018 GeoData <geodata@soton.ac.uk>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
/**
 * @file MeshTile.cpp
 * @brief This defines the `MeshTile` class
 * @author Alvaro Huarte <ahuarte47@yahoo.es>
 */
package com.atakmap.map.formats.quantizedmesh;

import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.Material;
import com.atakmap.map.layer.model.Mesh;
import com.atakmap.map.layer.model.VertexDataLayout;
import com.atakmap.math.MathUtils;
import com.atakmap.math.PointD;
import com.atakmap.math.PointI;
import gov.tak.platform.io.DataOutputStream2;

import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

final class MeshTile {
    ////////////////////////////////////////////////////////////////////////////////
    // Utility functions

    // Constants taken from http://cesiumjs.org/2013/04/25/Horizon-culling
    final static double llh_ecef_radiusX = 6378137.0;
    final static double llh_ecef_radiusY = 6378137.0;
    final static double llh_ecef_radiusZ = 6356752.3142451793;

    final static double llh_ecef_rX = 1.0 / llh_ecef_radiusX;
    final static double llh_ecef_rY = 1.0 / llh_ecef_radiusY;
    final static double llh_ecef_rZ = 1.0 / llh_ecef_radiusZ;

    // Stolen from https://github.com/bistromath/gr-air-modes/blob/master/python/mlat.py
    // WGS84 reference ellipsoid constants
    // http://en.wikipedia.org/wiki/Geodetic_datum#Conversion_calculations
    // http://en.wikipedia.org/wiki/File%3aECEF.png
    //
    final static double llh_ecef_wgs84_a = llh_ecef_radiusX;       // Semi - major axis
    final static double llh_ecef_wgs84_b = llh_ecef_radiusZ;       // Semi - minor axis
    final static double llh_ecef_wgs84_e2 = 0.0066943799901975848; // First eccentricity squared

    // LLH2ECEF
    static double llh_ecef_n(double x) {
      double snx = Math.sin(x);
      return llh_ecef_wgs84_a / Math.sqrt(1.0 - llh_ecef_wgs84_e2 * (snx * snx));
    }
    static PointD LLH2ECEF(final PointD coordinate) {
      double lon = coordinate.x * (Math.PI / 180.0);
      double lat = coordinate.y * (Math.PI / 180.0);
      double alt = coordinate.z;

      double x = (llh_ecef_n(lat) + alt) * Math.cos(lat) * Math.cos(lon);
      double y = (llh_ecef_n(lat) + alt) * Math.cos(lat) * Math.sin(lon);
      double z = (llh_ecef_n(lat) * (1.0 - llh_ecef_wgs84_e2) + alt) * Math.sin(lat);

      return new PointD(x, y, z);
    }
    // HORIZON OCCLUSION POINT
    // https://cesiumjs.org/2013/05/09/Computing-the-horizon-occlusion-point
    //
    static double ocp_computeMagnitude(final PointD position, final PointD sphereCenter) {
      double magnitudeSquared = magnitudeSquared(position.x, position.y, position.z);
      double magnitude = Math.sqrt(magnitudeSquared);
      PointD direction = new PointD(
              position.x / magnitude,
              position.y / magnitude,
              position.z / magnitude);
    
      // For the purpose of this computation, points below the ellipsoid
      // are considered to be on it instead.
      magnitudeSquared = Math.max(1.0, magnitudeSquared);
      magnitude = Math.max(1.0, magnitude);
    
      double cosAlpha = dot(direction, sphereCenter);
      double sinAlpha = magnitude(cross(direction, sphereCenter));
      double cosBeta = 1.0 / magnitude;
      double sinBeta = Math.sqrt(magnitudeSquared - 1.0) * cosBeta;
    
      return 1.0 / (cosAlpha * cosBeta - sinAlpha * sinBeta);
    }
    static PointD ocp_fromPoints(final PointD[] points, final BoundingSphere boundingSphere) {
      final double MIN = Double.NEGATIVE_INFINITY;
      double max_magnitude = MIN;

      // Bring coordinates to ellipsoid scaled coordinates
      final PointD center = boundingSphere.center;
      PointD scaledCenter = new PointD(center.x * llh_ecef_rX, center.y * llh_ecef_rY, center.z * llh_ecef_rZ);

      for (int i = 0, icount = points.length; i < icount; i++) {
        final PointD point = points[i];
        PointD scaledPoint = new PointD(point.x * llh_ecef_rX, point.y * llh_ecef_rY, point.z * llh_ecef_rZ);

        double magnitude = ocp_computeMagnitude(scaledPoint, scaledCenter);
        if (magnitude > max_magnitude) max_magnitude = magnitude;
      }
      return new PointD(
              scaledCenter.x*max_magnitude,
              scaledCenter.y*max_magnitude,
              scaledCenter.z*max_magnitude);
    }

    // PACKAGE IO
    static final double SHORT_MAX = 32767.0;
    static final int BYTESPLIT = 65636;

    static int quantizeIndices(final double origin, final double factor, final double value) {
      return (int) Math.round((value - origin) * factor);
    }

    // Write the edge indices of the mesh
    static int writeEdgeIndices(DataOutput ostream, final Mesh mesh, double edgeCoord, int componentIndex) throws IOException {
      ArrayList<Integer> indices = new ArrayList<>();
      Map<Integer, Integer> ihash = new HashMap<>();

      for (int i = 0, icount = mesh.getNumFaces()*3; i < icount; i++) {
        int indice = mesh.getIndex(i);
        final PointD vertex = new PointD();
        mesh.getPosition(indice, vertex);
        double val = componentIndex == 0 ? vertex.x : (componentIndex == 1 ? vertex.y : vertex.z);

        if (val == edgeCoord) {
          Integer it = ihash.get(indice);

          if (it == null) {
            ihash.put(indice, i);
            indices.add(indice);
          }
        }
      }


      int edgeCount = indices.size();
      ostream.writeInt(edgeCount);
      if(mesh.getNumVertices() > BYTESPLIT) {
          for (int i = 0; i < edgeCount; i++) {
              int indice = indices.get(i);
              ostream.writeInt(indice);
          }
      } else {
          for (int i = 0; i < edgeCount; i++) {
              int indice = indices.get(i);
              ostream.writeShort((short)indice);
          }
      }
      return indices.size();
    }

    // ZigZag-Encodes a number (-1 = 1, -2 = 3, 0 = 0, 1 = 2, 2 = 4)
    static int zigZagEncode(int n) {
      return (n << 1) ^ (n >> 31);
    }

    // Triangle area
    static double triangleArea(final PointD a, final PointD b) {
      double i = Math.pow(a.y * b.z - a.z * b.y, 2);
      double j = Math.pow(a.z * b.x - a.x * b.z, 2);
      double k = Math.pow(a.x * b.y - a.y * b.x, 2);
      return 0.5 * Math.sqrt(i + j + k);
    }

    // Constraint a value to lie between two values
    static double clamp_value(double value, double min, double max) {
      return value < min ? min : value > max ? max : value;
    }

    // Converts a scalar value in the range [-1.0, 1.0] to a SNORM in the range [0, rangeMax]
    static int snorm_value(double value) { return snorm_value(value, 255); }
    static int snorm_value(double value, double rangeMax) {
      return (int)Math.round((clamp_value(value, -1.0, 1.0) * 0.5 + 0.5) * rangeMax);
    }

    /**
     * Encodes a normalized vector into 2 SNORM values in the range of [0-rangeMax] following the 'oct' encoding.
     *
     * Oct encoding is a compact representation of unit length vectors.
     * The 'oct' encoding is described in "A Survey of Efficient Representations of Independent Unit Vectors",
     * Cigolle et al 2014: {@link https://jcgt.org/published/0003/02/01/}
     */
    static PointI octEncode(final PointD vector) { return octEncode(vector, 255); }
    static PointI octEncode(final PointD vector, double rangeMax) {
      PointD temp = new PointD();
      double llnorm = Math.abs(vector.x) + Math.abs(vector.y) + Math.abs(vector.z);
      temp.x = vector.x / llnorm;
      temp.y = vector.y / llnorm;

      if (vector.z < 0) {
        double x = temp.x;
        double y = temp.y;
        temp.x = (1.0 - Math.abs(y)) * (x < 0.0 ? -1.0 : 1.0);
        temp.y = (1.0 - Math.abs(x)) * (y < 0.0 ? -1.0 : 1.0);
      }
      return new PointI(snorm_value(temp.x, rangeMax), snorm_value(temp.y, rangeMax));
    }

    ////////////////////////////////////////////////////////////////////////////////

    private Mesh mMesh;

    public MeshTile(Mesh mesh) {
        mMesh = mesh;
    }

    /**
     * @details This writes gzipped terrain data to a file.
     */
    void
    writeFile(File file) throws IOException { writeFile(file, false); }
    public void
    writeFile(File file, boolean writeVertexNormals) throws IOException {
      try(DataOutputStream2 ostream = new DataOutputStream2(new FileOutputStream(file), ByteOrder.LITTLE_ENDIAN)) {
          writeFile(ostream, writeVertexNormals);
      }
    }

    /**
     * @details This writes raw terrain data to an output stream.
     */
    void
    writeFile(DataOutputStream2 ostream) throws IOException { writeFile(ostream, false); }
    void
    writeFile(DataOutputStream2 ostream, boolean writeVertexNormals) throws IOException {
        writeFile(prepareForHighwaterMarkEncoding(mMesh), ostream, writeVertexNormals);
    }

    static double component(PointD p, int c) {
        return (c==0) ? p.x : ((c==1) ? p.y : p.z);
    }

    static void
    writeFile(Mesh mesh, DataOutput ostream, boolean writeVertexNormals) throws IOException {

      // Calculate main header mesh data
      final int numVertices = mesh.getNumVertices();
      PointD[] vertices = new PointD[numVertices];
      PointD[] cartesianVertices = new PointD[numVertices];
      for (int i = 0, icount = numVertices; i < icount; i++) {
        final PointD vertex = new PointD();
        mesh.getPosition(i, vertex);
        vertices[i] = vertex;
        cartesianVertices[i] = LLH2ECEF(vertex);
      }
      BoundingSphere cartesianBoundingSphere = boundingSphereFromPoints(cartesianVertices);
      Envelope cartesianBounds = boundingBoxFromPoints(cartesianVertices);
      Envelope bounds = mesh.getAABB();


      // # Write the mesh header data:
      // # https://github.com/AnalyticalGraphicsInc/quantized-mesh
      //
      // The center of the tile in Earth-centered Fixed coordinates.
      double centerX = cartesianBounds.minX + 0.5 * (cartesianBounds.maxX - cartesianBounds.minX);
      double centerY = cartesianBounds.minY + 0.5 * (cartesianBounds.maxY - cartesianBounds.minY);
      double centerZ = cartesianBounds.minZ + 0.5 * (cartesianBounds.maxZ - cartesianBounds.minZ);
      ostream.writeDouble(centerX);
      ostream.writeDouble(centerY);
      ostream.writeDouble(centerZ);
      //
      // The minimum and maximum heights in the area covered by this tile.
      float minimumHeight = (float)bounds.minZ;
      float maximumHeight = (float)bounds.maxZ;
      ostream.writeFloat(minimumHeight);
      ostream.writeFloat(maximumHeight);
      //
      // The tile's bounding sphere. The X,Y,Z coordinates are again expressed
      // in Earth-centered Fixed coordinates, and the radius is in meters.
      double boundingSphereCenterX = cartesianBoundingSphere.center.x;
      double boundingSphereCenterY = cartesianBoundingSphere.center.y;
      double boundingSphereCenterZ = cartesianBoundingSphere.center.z;
      double boundingSphereRadius  = cartesianBoundingSphere.radius;
      ostream.writeDouble(boundingSphereCenterX);
      ostream.writeDouble(boundingSphereCenterY);
      ostream.writeDouble(boundingSphereCenterZ);
      ostream.writeDouble(boundingSphereRadius);
      //
      // The horizon occlusion point, expressed in the ellipsoid-scaled Earth-centered Fixed frame.
      PointD horizonOcclusionPoint = ocp_fromPoints(cartesianVertices, cartesianBoundingSphere);
      ostream.writeDouble(horizonOcclusionPoint.x);
      ostream.writeDouble(horizonOcclusionPoint.y);
      ostream.writeDouble(horizonOcclusionPoint.z);


      // # Write mesh vertices (X Y Z components of each vertex):
      ostream.writeInt(numVertices);
      double[] bounds_min = new double[] { bounds.minX, bounds.minY, bounds.minZ };
      double[] bounds_max = new double[] { bounds.maxX, bounds.maxY, bounds.maxZ };
      for (int c = 0; c < 3; c++) {
        double origin = bounds_min[c];
        double factor = 0;
        if (bounds_max[c] > bounds_min[c]) factor = SHORT_MAX / (bounds_max[c] - bounds_min[c]);

        // Move the initial value
        int u0 = quantizeIndices(origin, factor, component(vertices[0], c)), u1, ud;
        int sval = zigZagEncode(u0);
        ostream.writeShort((short)sval);

        for (int i = 1, icount = numVertices; i < icount; i++) {
          u1 = quantizeIndices(origin, factor, component(vertices[i], c));
          ud = u1 - u0;
          sval = zigZagEncode(ud);
          ostream.writeShort((short)sval);
          u0 = u1;
        }
      }

      // # Write mesh indices:
      int triangleCount = mesh.getNumFaces();
      ostream.writeInt(triangleCount);
      if (numVertices > BYTESPLIT) {
        int highest = 0;
        int code;

        // Write main indices
        for (int i = 0, icount = (mesh.getNumFaces()*3); i < icount; i++) {
          code = highest - mesh.getIndex(i);
          ostream.writeInt(code);
          if (code == 0) highest++;
        }
      }
      else {
        int highest = 0;
        int code;

        // Write main indices
        for (int i = 0, icount = (mesh.getNumFaces()*3); i < icount; i++) {
          code = highest - mesh.getIndex(i);
        if(code < 0) throw new RuntimeException();
          ostream.writeShort((short)code);
          if (code == 0) highest++;


        }
      }
      // Write all vertices on the edge of the tile (W, S, E, N)
      writeEdgeIndices(ostream, mesh, bounds.minX, 0);
      writeEdgeIndices(ostream, mesh, bounds.minY, 1);
      writeEdgeIndices(ostream, mesh, bounds.maxX, 0);
      writeEdgeIndices(ostream, mesh, bounds.maxY, 1);


      // # Write 'Oct-Encoded Per-Vertex Normals' for Terrain Lighting:
      if (writeVertexNormals && triangleCount > 0) {
        byte extensionId = 1;
        ostream.writeByte(extensionId);
        int extensionLength = 2 * numVertices;
        ostream.writeInt(extensionLength);

        PointD[] normalsPerVertex = new PointD[numVertices];
        PointD[] normalsPerFace = new PointD[triangleCount];
        double[] areasPerFace = new double[triangleCount];

        for (int i = 0, icount = (mesh.getNumFaces()*3), j = 0; i < icount; i+=3, j++) {
          final PointD v0 = cartesianVertices[ mesh.getIndex(i  ) ];
          final PointD v1 = cartesianVertices[ mesh.getIndex(i+1) ];
          final PointD v2 = cartesianVertices[ mesh.getIndex(i+2) ];

          PointD normal = cross(
                  new PointD(v1.x-v0.x, v1.y-v0.y, v1.z-v0.z),
                  new PointD(v2.x-v0.x, v2.y-v0.y, v2.z-v0.z));
          double area = triangleArea(v0, v1);
          normalsPerFace[j] = normal;
          areasPerFace[j] = area;
        }
        for (int i = 0, icount = (mesh.getNumFaces()*3), j = 0; i < icount; i+=3, j++) {
          int indexV0 = mesh.getIndex(i  );
          int indexV1 = mesh.getIndex(i+1);
          int indexV2 = mesh.getIndex(i+2);

          PointD weightedNormal = new PointD(
                  normalsPerFace[j].x * areasPerFace[j],
                  normalsPerFace[j].y * areasPerFace[j],
                  normalsPerFace[j].z * areasPerFace[j]
                  );

          normalsPerVertex[indexV0] = new PointD(
                  normalsPerVertex[indexV0].x+weightedNormal.x,
                  normalsPerVertex[indexV0].y+weightedNormal.y,
                  normalsPerVertex[indexV0].z+weightedNormal.z
          );
          normalsPerVertex[indexV1] = new PointD(
                  normalsPerVertex[indexV1].x+weightedNormal.x,
                  normalsPerVertex[indexV1].y+weightedNormal.y,
                  normalsPerVertex[indexV1].z+weightedNormal.z
          );
          normalsPerVertex[indexV2] = new PointD(
                  normalsPerVertex[indexV2].x+weightedNormal.x,
                  normalsPerVertex[indexV2].y+weightedNormal.y,
                  normalsPerVertex[indexV2].z+weightedNormal.z
          );
        }
        for (int i = 0; i < numVertices; i++) {
          PointI xy = octEncode(normalize(normalsPerVertex[i]));
          ostream.writeByte((byte)xy.x);
          ostream.writeByte((byte)xy.y);
        }
      }
    }

    static Mesh prepareForHighwaterMarkEncoding(final Mesh mesh) {
        final int numVertices = mesh.getNumVertices();
        final int numIndices = mesh.getNumFaces()*3;
        Map<Integer, Integer> remapped = new HashMap<>();
        int[] tx = new int[numVertices];
        for(int i = 0; i < numIndices; i++) {
            final int idx = mesh.getIndex(i);
            if(!remapped.containsKey(idx)) {
                final int nextFree = remapped.size();
                tx[nextFree] = idx;
                remapped.put(idx, nextFree);
            }
        }

        return new Mesh() {
            @Override
            public int getNumVertices() { return mesh.getNumVertices(); }
            @Override
            public int getNumFaces() { return mesh.getNumFaces(); }
            @Override
            public boolean isIndexed() { return mesh.isIndexed(); }
            @Override
            public void getPosition(int i, PointD xyz) { mesh.getPosition(tx[i], xyz); }
            @Override
            public void getTextureCoordinate(int texCoordNum, int i, PointD uv) { mesh.getTextureCoordinate(texCoordNum, tx[i], uv); }
            @Override
            public void getNormal(int i, PointD xyz) { mesh.getNormal(tx[i], xyz); }
            @Override
            public int getColor(int i) { return mesh.getColor(tx[i]); }
            @Override
            public Class<?> getVertexAttributeType(int attr) { return mesh.getVertexAttributeType(attr); }
            @Override
            public Class<?> getIndexType() { return mesh.getIndexType(); }
            @Override
            public int getIndex(int i) { return remapped.get(mesh.getIndex(i)); }
            @Override
            public Buffer getIndices() { return null; }
            @Override
            public int getIndexOffset() { return mesh.getIndexOffset(); }
            @Override
            public Buffer getVertices(int attr) { return null; }
            @Override
            public WindingOrder getFaceWindingOrder() { return mesh.getFaceWindingOrder(); }
            @Override
            public DrawMode getDrawMode() { return mesh.getDrawMode(); }
            @Override
            public Envelope getAABB() { return mesh.getAABB(); }
            @Override
            public VertexDataLayout getVertexDataLayout() { return mesh.getVertexDataLayout(); }
            @Override
            public int getNumMaterials() { return mesh.getNumMaterials(); }
            @Override
            public Material getMaterial(int index) { return mesh.getMaterial(index); }
            @Override
            public Material getMaterial(Material.PropertyType propertyType) { return mesh.getMaterial(propertyType); }
            @Override
            public void dispose() { mesh.dispose(); }
        };
    }

    static Envelope boundingBoxFromPoints(final PointD[] points) {
        final double MAX =  Double.POSITIVE_INFINITY;
        final double MIN =  Double.NEGATIVE_INFINITY;
        double min_x = MAX;
        double min_y = MAX;
        double min_z = MAX;
        double max_x = MIN;
        double max_y = MIN;
        double max_z = MIN;

        for (int i = 0, icount = points.length; i < icount; i++) {
            final PointD point = points[i];

            if (point.x < min_x) min_x = point.x;
            if (point.y < min_y) min_y = point.y;
            if (point.z < min_z) min_z = point.z;
            if (point.x > max_x) max_x = point.x;
            if (point.y > max_y) max_y = point.y;
            if (point.z > max_z) max_z = point.z;
        }

        return new Envelope(min_x, min_y, min_z, max_x, max_y, max_z);
    }

    /// Calculate the center and radius from the specified point stream
    /// Based on Ritter's algorithm
    static BoundingSphere boundingSphereFromPoints(final PointD[] points) {
        if(points.length == 0)
            return new BoundingSphere();

        final double MAX =  Double.POSITIVE_INFINITY;
        final double MIN =  Double.NEGATIVE_INFINITY;

        PointD minPointX = new PointD(MAX, MAX, MAX);
        PointD minPointY = new PointD(MAX, MAX, MAX);
        PointD minPointZ = new PointD(MAX, MAX, MAX);
        PointD maxPointX = new PointD(MIN, MIN, MIN);
        PointD maxPointY = new PointD(MIN, MIN, MIN);
        PointD maxPointZ = new PointD(MIN, MIN, MIN);

        // Store the points containing the smallest and largest component
        // Used for the naive approach
        for (int i = 0, icount = points.length; i < icount; i++) {
            final PointD point = points[i];

            if (point.x < minPointX.x) minPointX = point;
            if (point.y < minPointY.y) minPointY = point;
            if (point.z < minPointZ.z) minPointZ = point;
            if (point.x > maxPointX.x) maxPointX = point;
            if (point.y > maxPointY.y) maxPointY = point;
            if (point.z > maxPointZ.z) maxPointZ = point;
        }

        // Squared distance between each component min and max
        double xSpan = MeshTile.magnitudeSquared(maxPointX.x-minPointX.x, maxPointX.y-minPointX.y, maxPointX.z-minPointX.z);
        double ySpan = MeshTile.magnitudeSquared(maxPointY.x-minPointY.x, maxPointY.y-minPointY.y, maxPointY.z-minPointY.z);
        double zSpan = MeshTile.magnitudeSquared(maxPointZ.x-minPointZ.x, maxPointZ.y-minPointZ.y, maxPointZ.z-minPointZ.z);

        PointD diameter1 = minPointX;
        PointD diameter2 = maxPointX;
        double maxSpan = xSpan;
        if (ySpan > maxSpan) {
            diameter1 = minPointY;
            diameter2 = maxPointY;
            maxSpan = ySpan;
        }
        if (zSpan > maxSpan) {
            diameter1 = minPointZ;
            diameter2 = maxPointZ;
            maxSpan = zSpan;
        }

        PointD ritterCenter = new PointD(
                (diameter1.x + diameter2.x) * 0.5,
                (diameter1.y + diameter2.y) * 0.5,
                (diameter1.z + diameter2.z) * 0.5
        );
        //double radiusSquared = (diameter2.subtract(ritterCenter)).magnitudeSquared();
        double radiusSquared = MeshTile.magnitudeSquared(diameter2.x-ritterCenter.x, diameter2.y-ritterCenter.y, diameter2.z-ritterCenter.z);
        double ritterRadius = Math.sqrt(radiusSquared);

        // Initial center and radius (naive) get min and max box
        PointD minBoxPt = new PointD(minPointX.x, minPointY.y, minPointZ.z);
        PointD maxBoxPt = new PointD(maxPointX.x, maxPointY.y, maxPointZ.z);
        PointD naiveCenter = new PointD(
                (minBoxPt.x+maxBoxPt.x) * 0.5,
                (minBoxPt.y+maxBoxPt.y) * 0.5,
                (minBoxPt.z+maxBoxPt.z) * 0.5
        );
        double naiveRadius = 0;

        for (int i = 0, icount = points.length; i < icount; i++) {
            final PointD point = points[i];

            // Find the furthest point from the naive center to calculate the naive radius.
            double r = MathUtils.distance(point.x, point.y, point.z, naiveCenter.x, naiveCenter.y, naiveCenter.z);
            if (r > naiveRadius) naiveRadius = r;

            // Make adjustments to the Ritter Sphere to include all points.
            //double oldCenterToPointSquared = (point.subtract(ritterCenter)).magnitudeSquared();
            double oldCenterToPointSquared = MeshTile.magnitudeSquared(point.x-ritterCenter.x, point.y-ritterCenter.y, point.z-ritterCenter.z);

            if (oldCenterToPointSquared > radiusSquared) {
                double oldCenterToPoint = Math.sqrt(oldCenterToPointSquared);
                ritterRadius = (ritterRadius + oldCenterToPoint) * 0.5;

                // Calculate center of new Ritter sphere
                double oldToNew = oldCenterToPoint - ritterRadius;
                ritterCenter.x = (ritterRadius * ritterCenter.x + oldToNew * point.x) / oldCenterToPoint;
                ritterCenter.y = (ritterRadius * ritterCenter.y + oldToNew * point.y) / oldCenterToPoint;
                ritterCenter.z = (ritterRadius * ritterCenter.z + oldToNew * point.z) / oldCenterToPoint;
            }
        }

        BoundingSphere retval = new BoundingSphere();
        // Keep the naive sphere if smaller
        if (naiveRadius < ritterRadius) {
            retval.center = ritterCenter;
            retval.radius = ritterRadius;
        } else {
            retval.center = naiveCenter;
            retval.radius = naiveRadius;
        }
        return retval;
    }

    static double magnitudeSquared(PointD p) {
        return (p.x*p.x) + (p.y*p.y) + (p.z*p.z);
    }
    static double magnitude(PointD p) {
        return Math.sqrt(magnitudeSquared(p));
    }
    static double magnitudeSquared(double x, double y, double z) {
        return (x*x) + (y*y) + (z*z);
    }

    static double dot(PointD a, PointD b)
    {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    static PointD cross(PointD a, PointD b)
    {
        return new PointD(a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x);
    }
    static PointD normalize(PointD p) {
        final double mag = magnitude(p);
        return new PointD(p.x/mag, p.y/mag, p.z/mag);
    }

    final static class BoundingSphere {
        PointD center = new PointD();
        double radius = 0d;
    }
}
