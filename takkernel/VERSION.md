# Version History

## 8.30.3

* 2525D+ versions rolled back to `mil-sym-java@2.1.5` and `mil-sym-android@2.4.0`

## 8.30.2

* Label Font Size works for 2525C and 2525D

## 8.30.1

* Correct parsing of TAK Server Connection informtion introduced as part of toning down the logic when receiving a malformed network connect string

## 8.30.0

* Refactor KMZ DatasetDescriptor from ATAK into takkernel

## 8.29.5

* Set label font size for single point graphics

## 8.29.4

* Revert 2525 D/E changes from "8.29.3"
* 2525 C/D/E
  * Mark applicable modifiers as `Number` parsed value type
  * Add support for reference+unit prefs for single-point altitude modifiers
  * If control point is not available for altitude modifier, force MSL reference
  * Better consistency for 2525D+ line patterns across different Android DPIs with 2525C renderer
  * Bump 2525D+ to `mil-sym-java@2.2.0` and `mil-sym-android@2.5.0`

## 8.29.3

* Fixed 2525 D/E implementations not rendering modifiers in passed AttributeSet

## 8.29.2

* Tone down the logging when receiving a malformed network connect string

## 8.29.1

* Copy over the padding from the IScaleWidget2

## 8.29.0

* Improve VectorTile line width and label size

## 8.28.0

* Expose GLBatchGeometryFeatureDataStoreRenderer3::Options via JNI.

## 8.27.2

* ConnectionEntry.toUrl create an invalid srt url with a /? instead of just ?

## 8.27.1

* Pairing Line is missing for laser designator

## 8.27.0

* Wrap per-feature traits in a single struct.
* Add LineMode trait to specify Rhumb/GreatCircle line modes.

## 8.26.0

* Add CLI GeoCalculations overloads by adding a DateTime parameter to match JNI.

## 8.25.25

* `FileSystemUtils` supports unzip of files created on other operating systems while correctly preserving directory structuure

## 8.25.24

* Java tile clients use local truststores to read tiles from TAK Server file server with client cert auth

## 8.25.23

* Properly handle a # in the uri for a video resource

## 8.25.22

* Retain previous behavior for 2525C ATAK Tests

## 8.25.21

* Add heading parsing for icon styles in KMLDriverDefinition2.

## 8.25.20

* Importing imagery in a directory structure was broken when adding in the Copy/Move/Inplace code.  Restore working behavior in the now common ImportResolver code

## 8.25.19

* 2525D / E symbol table check for version
* Fix 2525E tests for version
* Fix startup race condition

## 8.25.18

* MilSym renderer shows optionally user configured units and altitude reference

## 8.25.17

* Explicitly mark `NativeFeatureDefinition3` as _do-not-obfuscate_ for R8 compatibility

## 8.25.16

* Keep the state of the customRole part of the map item in sync with the incomming CoT detail

## 8.25.15

* Internal C3DT parser uses correct path separator for URLs

## 8.25.14

* Fix seams in for MapBox Terrain RGB tiles due to off-by-one error

## 8.25.13

* GDAL update for improved consistency with Google Earth

## 8.25.12

* `OSMDroidTileContainer`
  * adopts metadata from _spec_ during container creation
  * `getOriginY` returns correct value
  * _spec_ compatibility allows for some threshold when comparing expected tile grid origins

## 8.25.11

* Improved KML consistency with Google Earth
  * Intrepret `<tessellate>` tag to inform the feature's `AltitudeMode`
  * Default line and polygon color is white
  * gracefully handle placemarks without geometry

## 8.25.10

* Upgrade to the WMM (HR) 2025

## 8.25.9

* Fix tatic initialization for `MilStd2525` on desktop

## 8.25.8

* Remove unused include

## 8.25.7

* Revert change to CompositeStyle automatic labeling handling.

## 8.25.6

* Apply tessellation settings when editing shapes using the FeatureEditControl.

## 8.25.5

* Restore maven local publication for desktop artifacts

## 8.25.4

* When making arrays for 2525 control point storage use IGeoPoint[] and not concrete GeoPoint[].

## 8.25.3

* Allow KML default point behavior in a compound style

## 8.25.2

* Add rendering support for additional 2525 symbols.

## 8.25.1

* When necessary, render 2525C Rectangle types as Irregular to fix Modifier layout

## 8.25.0

* Update to Milsym Android Renderer 2.1.0 (2525 Legacy Renderer)

## 8.24.0

* Update Milsym Android to 2.4.0

## 8.23.0

* correct 2525D icon offset
* Missing symbol folders 
* cotTypeFromMil2525C handles all upper case input
* "WARFIGHTING SYMBOLS" folder included in "Warfighting Symbology"
* Update to Gradle 8 and Android Gradle 8 matching ATAK version
* Update Android Build tools to 35
* Update Milsym Android to 2.3.3

## 8.22.8

* Fix potential `nullptr` dereference when deserializing QME blobs from files

## 8.22.7

* Better marshaling of strings between managed and native code to support offline imagery with non-English layer/file names.

## 8.22.6

* `AtakMapView.getGeoCorners()` is deprecated for removal

## 8.22.5

* Redirect spdlog to TAK logger in cesium-native.
* Bring in updated cesium-native 0.40.1 build that includes _DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR fix.

## 8.22.4

* Add in missed ATAK's ImportResolver improvements missed during the switch over.

## 8.22.3

* Handle potential `NullPointerException` rendering C3DT if old import did not have associated metadata

## 8.22.2

* Refactor 2525 helper class to retain package private access after R8

## 8.22.1

* Update RBT identification based on NSG 

## 8.22.0

* Symbology Provider API
  * Add 2525D/E symbology providers
  * Adds abstraction for _amplifiers_ and _hq-taskforce-dummy_ symbol code manipulation
  * `MilStd2525` class for MIL-STD-2525 specific utilities
* Add utility to `CotUtil` for converting between CoT type and 2525C codes  

## 8.21.8

* Instantiate 3D Tiles shaders per-RenderContext.
* Query URL handlers for 3D tile assets in a specific order.

## 8.21.7

* coordinate transformation micro-optimization
* `MBTilesContainer` bounds update only considers max zoom

## 8.21.6

* Add fill style support to native feature factory KML parser.

## 8.21.5

* Fix feature labels not respecting altitude mode.

## 8.21.4

* Update minimum CMake version to 3.21.0 for VS2022 support on Windows hosts (ENGINE-1006)
* GenerateCMakeProjects.vcxproj: Specify VS2022 generator when invoking CMake as this is needed by dependency libraries

## 8.21.3

* ConnectionEntryBase: Treat SRT URL timeout specifications as microseconds to match serialization in getURL().  Attempt to auto-detect erroneous uses of milliseconds in SRT URLs. (ATAK-20030)

## 8.21.2

* Performance improvements to 3D tile rendering. Send 3D tiles in a batch to render.

## 8.21.1

* CLI bindings handle World Mercator / 3395

## 8.21.0

* Port ATAK's ImportResolver and several implementations. Introduce callback interfaces for client-specific implementation details.

## 8.21.8

* Instantiate 3D Tiles shaders per-RenderContext.
* Query URL handlers for 3D tile assets in a specific order.

## 8.21.7

* coordinate transformation micro-optimization
* `MBTilesContainer` bounds update only considers max zoom

## 8.21.6

* Add fill style support to native feature factory KML parser.

## 8.21.5

* Fix feature labels not respecting altitude mode.

## 8.21.4

* Update minimum CMake version to 3.21.0 for VS2022 support on Windows hosts (ENGINE-1006)
* GenerateCMakeProjects.vcxproj: Specify VS2022 generator when invoking CMake as this is needed by dependency libraries

## 8.21.3

* ConnectionEntryBase: Treat SRT URL timeout specifications as microseconds to match serialization in getURL().  Attempt to auto-detect erroneous uses of milliseconds in SRT URLs. (ATAK-20030)

## 8.21.2

* Performance improvements to 3D tile rendering. Send 3D tiles in a batch to render.

## 8.21.1

* CLI bindings handle World Mercator / 3395

## 8.21.0

* Port ATAK's ImportResolver and several implementations. Introduce callback interfaces for client-specific implementation details.

## 8.20.0

* `Cesium Native` integration

## 8.19.13

* Guard against very large sample rates when constructing ellipse geometries

## 8.19.12

* Improve terrain LOD normalization for scenes with a high degree of topographical relief

## 8.19.11

* Address compilation failure for the JRE with gcc 14.2.0

## 8.19.10

* Avoid deadlock caused by setting font while other threads are using the renderer

## 8.19.9

* `GLMagnifierLayer` disables terrain for magnifier ortho view to reduce potential latency during `release()`

## 8.19.8

* Index the user added roles so getRoles(String, String) is operational

## 8.19.7

* Do not attempt to load duplicate renderers for a given layer

## 8.19.6

* Preserve port and path for RAW video aliases to match the behavior already in use by the manual entry

## 8.19.5

* `GdalElevationChunk` adds detection for AW3D data
* `GdalElevationChunk` utilizes additional GeoTIFF metadata for heightmap identification

## 8.19.4

* `TileScraper` finds closest zoom level when the request min and max resolution are the same

## 8.19.3

* Deduplicate vector tile basemap and overlay content from same tile server
* Fix bug resulting in continuous background query for offscreen vector tiles

## 8.19.2

* Fix burned-in icon text on 32-bit Android

## 8.19.1

* Fixed sizing issue related to MIL-STD-2525 icon annotated symbols

## 8.19.0

* Add support for Geopackage `2d-gridded-coverage` extension

## 8.18.1

* Fix bounds error in `GLLabel.IntersectRotatedRectangle()`

## 8.18.0

* `TerrainRGBChunkSpi.getMimeType()` returns correct value
* Quantized Mesh `TerrainData_deserialize` is micro-optimized for chunk decoding
* Fix `Transmute.collection(Collection, Function)` infinite recursion

## 8.17.0

* Implement `QMEChunkSpi` decode and encode
* QME `TerrainData` may be deserialized from arbitrary blobs in addition to files
* General optimizations/cleanup in QME implementation

## 8.16.10

* Fix for drawing milstd shapes with a draw rule of `DRAW_CATEGORY_RECTANGULAR_PARAMETERED_AUTOSHAPE`.

## 8.16.9

* Fix role abbreviation collision for Tactical Communicator and Tactical Operations Center

## 8.16.8

* Restore legacy tile prioritization for imagery based on some cosmetic regression with `8.16.5`

## 8.16.7

* GLTexture: Respect GL alignment when uploading Android Bitmaps to texture (ATAK-19376)

## 8.16.6

* Minor modifications to embedded OMT overlay style

## 8.16.5

* Tile clients skip the queuing and background download of tiles that are not currently visible on the screen

## 8.16.4

* `PfpsMosaicDb` attempts to handle changes to username/mapped network drive letters
* `PfpsMosaicDb` has better consistency with legacy mosaicdb for zoom level selection

## 8.16.3

* Streaming Tiles JSON config v4 supports implicit tile grids for well-known SRS (4326, 3857 and 3395)

## 8.16.2

* Vector Tiles _place names_ LODs more closely match commercial UX
* Implement label localization

## 8.16.1

* Eliminate vector tiles _place name_ duplicate labels 

## 8.16.0

* Update included vector tile styles
* Improved vector tile support

## 8.15.0

* Add Roles API

## 8.14.1

* GLText: Annotate createTextFormat() as DontObfuscate to prevent failed JNI lookups in release builds (see ATAK-19827)

## 8.14.0

* Mark the default implementation of hitTest in LayerHitTestControl as deprecated and a note require implementations to provide an implementation.

## 8.13.1

* Civil sea surface tracks now rendered with purple fill color

## 8.13.0

* `GLGlyphAtlas2` supports dynamic (m)SDF glyph generation, eliminating need for pre-generated atlases
* `GLLabelManager` transitions to `GLGlyphAtlas2` and associated machinery
* Java `TextFormatFactory` is replaced by an _off-the-shelf_ library implementation and is no longer pluggable
* Improve `MapTextFormat` font family lookups with aliasing to reduce misses on Android

## 8.12.0

* `FileSystemUtils` allows _one-time_ initialization with an explicit mount point

## 8.11.1

* refactor MBTiles `DbSchema` classes into `MBTilesContainer` to avoid bad obfuscation

## 8.11.0

* `MBTilesInfo` max grid scanning is optional and off by default

## 8.10.0

* abstract MBtiles schema
* implement OMT MBtiles schema
* `MBTilesContainer` implements optional asynchronous writes

## 8.9.3

* Detect MBTiles containers with terrain content

## 8.9.2

* Fixed issues with MIL-STD-2525 modifiers

## 8.12.0

* `FileSystemUtils` allows _one-time_ initialization with an explicit mount point

## 8.11.1

* refactor MBTiles `DbSchema` classes into `MBTilesContainer` to avoid bad obfuscation

## 8.11.0

* `MBTilesInfo` max grid scanning is optional and off by default

## 8.10.0

* abstract MBtiles schema
* implement OMT MBtiles schema
* `MBTilesContainer` implements optional asynchronous writes

## 8.9.3

* Detect MBTiles containers with terrain content

## 8.9.2

* Fixed issues with MIL-STD-2525 modifiers

## 8.9.1

* refactor PFPS impl into internal classes for better maintainability
* `PfpsMosaicDb` stores index records in _patches_ to reduce initialization and query overhead
* `GLMosaicMapLayer` parallelizes frame loading during query
* fix bug with coverage reporting that broke layer lock

## 8.9.0

* AttributeSet-based get, set support for DataModelSpi and fires models. Detail handlers updated accordingly. Map-based methods deprecated

## 8.8.0
* Update TTP to 4.1.3
* Add new MsAccessDatabaseProvider, MDBAccessDatabaseProvider, against cross-platform native library rather than depending on platform-specific solutions (ENGINE-973)

## 8.7.0

* change `okhttp` to `api` dependency to align desktop transitive dependencies with Android

## 8.6.1

* Terrain RGB implementation supports World Mercator tile grid

## 8.6.0

* Add `GdalElevationChunk` method for determining if a GDAL file has a vertical coordinate system

## 8.5.1

* Correct soft-clamping logic in `GdalElevationChunk` to subpixel

## 8.5.0

* `ElevationManager` heightmap construction allows for user specified SRID 

## 8.4.0

* Add `StreamingTiles.create` to create streaming tiles configuration JSON

## 8.3.6

* `CotParseUtils` implements case-insensitive handling for non-numeric double value constants (e.g. `"NaN"`) 

## 8.3.5

* Suppress benign warning logging during library initialization

## 8.3.4

* GLLabelManager now calls TextFormat2 methods on the render thread and caches the results

## 8.3.3

GLTile is subject to legacy obfuscation causing - java.lang.UnsatisfiedLinkError: No implementation found for boolean com.atakmap.map.formats.c3dt.n.intersects(long, double, double, double, double).   Refactor the package private static method into a new class

## 8.3.2

* Optimize mosaicdb creation for .NET bindings for massive datasets on network drives
* Micro-optimize C++ mosaic renderer

## 8.3.1

* Remove use of regex replacement functions on Linux in favor of built-in regex functions.

## 8.3.0

* Add support for single point MIL-STD-2525 modifiers

## 8.2.2

* Disable simplification when editing features.

## 8.2.1

* Fix the parameter order for ElMgrTerrainRenderService GLTexture2 method calling.

## 8.2.0

* Protect read/write accesses of ElMgr's shared_ptr using atomics.

## 8.1.0

* Downgrade to `okhttp@4.11.0` for min SDK version 21 compatibility
* Expose `okhttp` as transitive dependency on AAR

## 8.0.0

* Change CI to build Linux using Oracle Linux 8
* Change CI to build Windows using VS2022
* `HttpClientBuilder` uses `okhttp` as default implementation
* `:takkernel:engine` JNI sources do proper incremental builds (only rebuild on modification)
* Dependency Updates
  * `takthirdparty@4.0.1`
  * `c5isr-renderer@2.1.1`
  * `mil-sym-android-renderer@0.1.60`

## 7.14.5

* Micro-optimize `DefaultDatasetProjection2` and `GdalDataestProjection`

## 7.14.4

* Check to see if file path points to a database before attempting to parse 

## 7.14.3

* Fix rectangle intersects algorithm for coincident but non-adjacent edges

## 7.14.2

* Fix `SpatialCalculator` unit tests

## 7.14.1

* Update `liblas` - fixes leaking of file handles when initial parsing fails (ENGINE-950)

## 7.14.0

* `CertificateManagerBase` add DoD Root CA 6 to trusted roots (ENGINE-936)

## 7.13.0

* Port ATAK's VideoDetailHandler. Direct <__video> detail support. <ConnectionEntry> support still partially deferred to client.

## 7.12.3

* Fix infinite recursion in SpatialCalculator#createPolygon(Collection<IGeoPoint>, Collection<Collection<IGeoPoint>>, IGeoPoint)

## 7.12.2

* Remove verbose logging during KML style parse

## 7.12.1

* Fix style parse for icons synthesized for MIL-STD-2525 multi-point graphics on Windows

## 7.12.0

* Extract repeated error message handling from `MobacTileClient2` out into a new `SuppressibleErrorTracker` class.
  And add `UnknownHostException` to the list of exceptions that have error reporting suppressed by it.

## 7.11.0

* Overload `SpatialCalculator` for `IGeoPoint`
* Add `computeAverage` to `gov.tak.api.engine.map.coords.GeoCalculations`
* `MarshalService.marshal` overloaded for array types

## 7.10.14

* Legacy `FeatureDataStore` adapter raises checked `DataStoreException` for implementation level errors that are otherwise valid invocations by the client

## 7.10.13

* Micro-optimize `GdalElevationChunk` / `GdalDatasetProjection2` based on profiling

## 7.10.12

* `MilStd2525cInterop.getSymbolDef` defers to tactical graphics before units to bias default shape for collisions

## 7.10.11

* Don't flatten geometry collections for OGR file overlays parse

## 7.10.10

* Elevation adapter for `MosaicDatabase2` will utilize `Observable` control to propagate _content changed_ events

## 7.10.9

* Adjust feature label text color for contrast if forced background is enabled

## 7.10.8

* `TerrainRGBChunkSpi` defer decoding of compressed tile to Bitmap until samples are requested; optimizes cases where only broad info is needed up front (such as ElevationManager queries - see ENGINE-929)

## 7.10.7

* ElevationManager: don't attempt to fetch elevation for NaN input points (ENGINE-937)

## 7.10.6

* Fix logic for filling in no data values in GDAL elevation chunks.

## 7.10.5

* Vector tiles renderer supports online/offline toggle for streaming tile sources

## 7.10.4

* Guardsquare Proguard no longer supported for Gradle 8, prepare build system for kernel to work with Gradle 8

## 7.10.3

* `NativeProjection` returns `null` and does not throw if `forward`/`inverse` fails, per contract

## 7.10.2

* `GeoPackageTileContainer` does not interact with database after being disposed

## 7.10.1

* `OSMDroidTileContainer` avoids interaction with database after disposed

## 7.10.0

* Add MeshPointStyle CLI Wrappers, Add Conversion in StyleInterop.cpp For MeshPointStyle_CLI

## 7.9.3

* Support "/vsimem/" paths in GdlaElevationChunk
* Modify getTileData() logic to avoid downloading unnecessary tiles that are non-expired.

## 7.9.2

* Fix invert of water/land regions for vector tiles along anti-meridian

## 7.9.1

* Clean up TileClient if GLVectorTiles.SPI is not going to use it.

## 7.9.0

* Add ability to provide FeatureQueryParameters filter to GLBatchGeometryFeatureDataStoreRenderer3

## 7.8.1

* Prevent invalid memory access when drawing filled features across the IDL

## 7.8.0

* Fix implementation of GeometryFactory.fromEnvelope()

## 7.7.0

* Java runtime Cesium 3D Tiles improvements
  * Enable draco compression support
  * Add GLB tile content blob support
  * Implement root tileset refresh
  * Improve handling for different volume types
  * Handle absolute content URIs
  * Implement 3D Tiles Package (sqlite) as local cache to accommodate content names of arbitrary length
  * Improve low resolution tile pops during tilt/pan/rotate

## 7.6.7

* Protect against a null pointer condition when initialization of MapSceneModel 

## 7.6.6

* Clicking the radial menu _expander_ icon traverses submenus for desktop platforms

## 7.6.5

* `android-port@6.1.2`: allow client to specify absolute path for app data files via `TAK_APP_DATA_PATH` property 

## 7.6.4

* Refactor `DtedElevationSource` cursor `moveToNext` loop to eliminate erroneous double increment of index

## 7.6.3

* Use highp for stippled line rendering

## 7.6.2

* Prevent excessive tile iteration in C++ TileMatrixElevationSource impl 

## 7.6.1

* Add `null` check in `AtakCertificateDatabaseBase.checkValidity`

## 7.6.0

* Introduce `ICotDetailHandler2` allowing opaque _process item_ to simplify client side integration of library interface

## 7.5.9
* `MobacTileClient2`: suppress verbosity on Forbidden error (403) logging

## 7.5.8
* Clear old callback when setting new drawable in GLDrawableWidget to avoid memory leak

## 7.5.7
* Elevation Java interop: Fix use-after-delete of a JNI jobject reference that was resulting in undefined behavior (ATAK-19273)

## 7.5.6
* Elevation Java interop: Fix undefined behavior in interop layer

## 7.5.5
* GLScaleWidget should cache measurements to reduce calls made to _mapRenderer.inverse

## 7.5.4
* 'MobacMapSource:Config enables adaptive feedback by default

## 7.5.3

* `GLAsynchronousMapRenderable3` takes ownership of `target_state_.renderTiles` on assignment to avoid potential subsequent invalid memory access 

## 7.5.2

* Android: Fix use of conan.dir passed via properties for controlled build in like manner to main engine build

## 7.5.1

* Handle potential GPKG crash for SRS transformations outside of content bounds

## 7.5.0

* Add the optional MIL-STD-6090 caveat and releasableTo attributes to `CotEvent`

## 7.4.0

* Enable imagery renderers using non-deprecated bases
* Mark `GLNativeIamgeryRasterLayer2` deprecated

## 7.3.1

* Android: Fix use of conan.dir passed via properties

## 7.3.0

* add `PersistentDataSourceFeatureDataStore3.queryFeatureSets(File)`  for API parity with legacy
* `GeoPackageFeatureDataStore2` and associated APIs to replace legacy
* `WFSFeatureDataStore4` replacement for legacy

## 7.2.1

* Lazy initialize a Java Thread object and get the class loader to identify native vs java originated threads.

## 7.2.0

* Apply initial set of API deprecation markings for TAK 5.3 baseline

## 7.1.0

* `GLBitmapLoader` uses static registry for pluggable loaders

## 7.0.14

* Fix resource leaks in LibcurlHttpClientBuilder (see ATAK-19298):
  * Fix native-side global reference leak
  * Ensure native cleanup when request results in 404 error

## 7.0.13

* Ensure `MosaicDatabase2.close` is invoked to prevent potential memory leak
* Eliminate unnecessary bounds caching in `PersistentRasterDataStore`

## 7.0.12

* `MBTilesContainer` checks if file is database before opening
* Reduce logging for failed tile container opens

## 7.0.11

* `CameraController` _interactive_ pan-to function gracefully handles requests where the camera is below the target focus altitude

## 7.0.10

* Apply 2525C unit symbol code collision for Android interop

## 7.0.9

* Implement icon support for several multi-point symbologies (e.g. Bio, Chem and Nuclear contaminated areas)

## 7.0.8

* Ensure polyline input is supported for various _autoshape_ 2525C multipoint symbols

## 7.0.7

* Handle LRU container creation failure

## 7.0.6

* AABB handling for 2525C arrow/two-point line shapes checks for rectangle-like input, otherwise uses inputs directly as control points

## 7.0.5

* Implement support for _two-point rect parametered autoshape_ draw rule (e.g. kill box purple rectangular)

## 7.0.4

* AABB handling for 2525C "auto" shapes checks for rectangle-like input, otherwise uses inputs directly as control points

## 7.0.3

* Mitigate potential `NullPointerException` for "3D" multi-point symbologies

## 7.0.2

* `AttributeSet`: Fix bug introduced in 6.11.0 that can cause invalid memory accesses and client-side crashes when an AttributeSet containing a String array is copied (see ATAK-19217)

## 7.0.1

* Fix RPF coverage derivation from file name for frames in southern hemisphere

## 7.0.0

* `jogl@2.5.0`
* `bouncycastle@1.78.1`
* remove legacy `:takkernel:controlled` publications with _classifier_
* Add RenderSurface2 to access surface size in native pixels

## 6.15.5

* `MobacTileClient2`: suppress verbosity on NoRouteToHostException logging (ATAK-19175)

## 6.15.4

* Use highp precision to accommodate feature lighting on some GPUs.

## 6.15.3

* `OSMDroidTileContainer` checks if file is database before opening

## 6.15.2

* Fix for GLBatchGeometryFeatureDataStoreRenderer3 in multi-globe environments.

## 6.15.1

* ATAK-19216 Opensource Pull Request calling waitFor causing deadlock

## 6.15.0

* takthirdparty updated to 3.1.1, enables annotation rendering in gdal for GeoPDFs (ATAK-17915)

## 6.14.1

* Use `JNILocalRef` to handle automatic cleanup of identified JNI local ref leaks

## 6.14.0

* Add certificates to TAK.Engine assembly and add cert_catalog.json. WTK-11179
* CertificateManagerBase: Change embedded certificate loading to use new cert_catalog.json (ATAK-19196)

## 6.13.4

* C++ features renderer workarounds for GL implementation gaps on MPU
  * emulate `gl_PointCoord` in batch points fragment shader
  * disable line patterns
  * use oversized FBO instead of translated viewport for hit-test

## 6.13.3

* CertificateManager(Base): Fix bugs querying certificate database introduced in 6.6.0 (see ATAK-19168)

## 6.13.2

* `MilStd2525cSymboloryProviderBase` toggles property to enable rendering of unit display modifiers

## 6.13.1

* deprecate legacy Java features rendering APIs

## 6.13.0

* Align `:controlled` module publication with `:engine`

## 6.12.1

* `MapBoxGLStyleSheet` implements support for `visibility` layout property

## 6.12.0

* Add _overlay_ pre-packed vector styles

## 6.11.5

* `ImageryFileType` MBtiles includes `.mvt` extension
* Add _overlay_ flag to descriptors returned by `StreamingContentDatasetDescriptorSpi` (when appropriate)

## 6.11.4

* `OGRFeatureDataStore`'s `openOpts.value` pointer needs to be reassigned when additional options are appended

## 6.11.3

* GPKG tile rendering only uses quadtree if SRID 4326 or SRID 3857

## 6.11.2

* `DtedElevationSource` enforces default order when types are explicitly specified in filter

## 6.11.1

* Preserve ordering of hit test results from C++ renderer in Java GLBatchGeometryFeatureDataStoreRenderer2.

## 6.11.0

* Vector Tile support for Java runtimes based on `TileMatrix` framework
* Refactor `ElMgrTerrainRenderService` globe tile management into `TiledGlobe` template class
* Reimplement `ElMgrTerrainRenderService` using `TiledGlobe`
* `MBTilesInfo` vector tile identification
* Implement MapBox style parser
* Add default styles for offline vector tiles following OMT or RBT schemas
* Implement default "inspector" style for generic vector tiles schema
* Obsolete legacy `MVTFeatureDataSource`

## 6.10.6

* Fix CI for linux hosts for `dpkg`/`usrmerge` changes

## 6.10.5

* CertificateManagerBase: Fix missing private trust anchors when configured to include public/well known CAs (ATAK-19104, regression introduced in 6.3.0)

## 6.10.4

* Fix handling of coordinates in ElevationChunk. 
* Fix SRID returned from ManagedElevationChunk.Data.

## 6.10.3

* Change GLScaleWidget to respect the minimum width when in rounding mode.

## 6.10.2

* Fix Android ABI for new methods on `ElevationSourceBuilder`

## 6.10.1

* ATAK-18898 Remove constructions bounds for GeoPoint Altitude - correct GeoPoint construction from CotEvent

## 6.10.0

* `CotDetailManager` now registers a new ("__milSym") `MilSymDetailHandler`

## 6.9.0

* `AttributeSetDataModelSpi.put()` now removes KVPs if given null for Object types or a constant for primitives (the primitive Object type's `MIN_VALUE`).
* `LineBase` is public; clients will sometimes only need the base type.
* `LineBase` and it's inheritors now have default-accepting getters.
* `AttributeSetBase` locks around writes and exposes the associated (package-private) `ReadWriteLock`.
* `AttributeSetUtils` offers thread-safe replacements for race-prone `AttributeSet`.containsAttribute + getAttribute calls (for primitives).

## 6.8.0

* Add stars rendering.

## 6.7.0

* Introduce additional `ElevationSource` wrappers
  * `CascadingElevationSource` -- provides an aggregate view of multiple `ElevationSource` instances. Query results are returned sequentially.
  * `MultiplexingElevationSource` -- provides an aggregate view of multiple `ElevationSource` instances. Query results are sorted in accordance with the query param specified order.
  * `ResolutionConstrainedElevationSource` -- wraps an existing `ElevationSource` and restricts query results to specified min/max resolution thresholds
  * `VirtualCLODElevationSource` -- provides an aggregate view of multiple `ElevationSource` instances. Query results are returned sequentially.
  * `ElMgrTerrainRenderService` static utility accessor to the default lo-res `ElevationSource`
* C++ `ElevationTileClient` optionally accepts an existing `ElevationSource` to derive data. If not specified, default behavior derives from all available `ElevationSource`s via `ElevationManager` API
* Java `ElevationSourceBuilder` exposes static utilities to instantiate several new `ElevationSource` wrappers
* `OSMDroidInfo` reads and reports _content type_ metadata
* `TilesetInfo` defers to `OSMDroidInfo` for osmdroid parse
* `NativeElevationChunk` implements bulk fetch
* `StreamingTileClient` provides access to underlying file and `StreamingTiles` via `Controls` interface
* Introduce `ContainerInitializationControl` to allow client to inject metadata into a container during initial creation
* `OSMDroidTileClient` provides controls access
  * `ContainerInitializationControl`
  * `TilesMetadataControl`
  * `File`
* `TileProxy` implements `TileClientControl`
* `TileProxy` provides access to cache and client via `Controls` interface

## 6.6.1

* ATAK-18557 Playstore Crash: NullPointerException GLScaleWidget during uninitialized padding read

## 6.6.0

* CertificateManager: Add certificate pinning by host (see ATAK-18961)

## 6.5.7

* Fix atmosphere rendering on GPUs with reduced highp precision.

## 6.5.6

* Fix XRay rendering of SDF rendered text.

## 6.5.5

* Set proper terrain adjusted elevation for labels when AutoSurfaceOffsetAdjust is not set.

## 6.5.4

* `StreamingContentDatasetDescriptorSpi` validates that tile client can be created for v3 and later before returning a result

## 6.5.3

* Eliminate potential double string escape in `GeoPackageTileContainer`

## 6.5.2

* Remove constructions bounds for GeoPoint Altitude

## 6.5.1

* Quote the table name when dealing with gpkg files since the table name can 50a7-b684 or 50a7.b684

## 6.5.0

* Add cone and dome mesh primitives
* Workaround assimp's undesired _default material_

## 6.4.1

* Address `LaserBasket` style deficiencies
  * Wedges apply `BasicFillStyle`
  * Target line applies `ArrowStrokeStyle`
  * Generated features apply labels when builder option toggled

## 6.4.0

* `GdalElevationChunk` determines MSL vs HAE

## 6.3.0

* CertificateManager: Fix incorrect caching of SocketFactory
* CertificateManager: Correct misleading/backwards Javadoc
* CertificateManager(Base), CentralTrustManager (see ATAK-18905): 
  * Change local trust manager to optionally exclude public CAs
  * Exclude public CAs when socket factory set to disable hostname verification

## 6.2.0

* Add fontSize to symbology provider rendering hints.

## 6.1.6

* Add version directive to depth sampler shader.

## 6.1.5

* Fix animation flag causing unnecessary refresh.

## 6.1.4

* Fix bug when drawing route symbology control points.

## 6.1.3

* Terrain collision handling in the animation loop always uses `AdjustFocus` mode for an intuitive result

## 6.1.2

* Fixed bug where all of a MILSTD2525 symbol's modifiers were not being returned

## 6.1.1

* All unit tests pass on OS X target

## 6.1.0

* Add assimp and kml 3D scene support on Linux
* takthirdparty updated to 3.0.3

## 6.0.2

* 2525C symbology provider preview icon generation failure
  * better conform preview symbol control points based on impl requirements
  * set required modifiers for preview symbols
  * handle case of symbol reported as unit
  * filter out `DRAW_CATEGORY_DONOTDRAW` symbols
  * update control points interpretation for additional symbol specialization
  * adjust logger level

## 6.0.1

* `MilStd2525cSymbologyProvider` supports _Circular Parameterized Autoshape_ draw rule

## 6.0.0

* Port TAKX's CotEvent, CotDetail and their dependencies into gov.tak.api.cot(.event) packages.
* Add an AttributeSet-based CotDetailManager and ICotDetailHandler. API similar to ATAK's.
* Add a BullseyeDetailHandler, extracting the portions from ATAK's that use meta values directly from the AttributeSet.

## 5.11.2

* Fix bug when drawing route symbology control points.

## 5.11.1

* Introduce logic to prevent a OutOfBoundsException as suggested in ATAK-18850 
UTMPoint.fromLatLng() StringIndexOutOfBoundsException

## 5.11.0

* Add FinalApproachHeading


## 5.9.11

* Feature renderer icon dimension constraint is captured at instantiation
* Default icon dimension constraint is 32 to match default icon size in `GLGeometryBatchBuilder`

## 5.9.10

* JNI local ref leak of _spatial filter_ in `ManagedElevationSource.query`

## 5.9.9

* Fix crash in `GLBatchGeometryRenderer4.hitTest` if no surface tiles intersected

## 5.9.8

* `TerrainRGBChunkSpi` uses pooled `Bitmap` instances to reduce object spam
* `TileMatrixElevationSource` further constrains query depth for elevation profile use-case

## 5.9.7

* Feature hit-test includes multiple surface feature hits

## 5.9.6

* ATAK-18804 Laser Basket Exception when no laser basket is visible caused during concurrent building of a laser basket

## 5.9.5

* Rework terrain collision mitigation from `5.5.1` as an implementation detail of camera animation in `GLGlobe::animate`

## 5.9.4

* Fix removeOnCameraChangedListener() implementation

## 5.9.3

* Perform alpha check for textured and mesh feature rendering

## 5.9.2

* Update takthirdparty to 3.0.3, fixing use of assimp on Android x86_64 (see ATAK-17828)

## 5.9.1

* Automatically clean up registered native factories at exit

## 5.9.0

* Add per-vertex color and lighting support to mesh feature rendering
* Fix issues with models appearing at wrong altitude
* Add shape mesh resources
* Modify TAKAssimpIOStream to support ProtocolHandler.

## 5.8.3

* Fix performance regression in pan+zoom+tilt when tilted

## 5.8.2

* Fix bugs with sizing, position, and hit testing in ButtonWidget.

## 5.8.1

* PRI transitive windows dependency links against MSVC release runtime

## 5.8.0

* takthirdparty updated to 3.0.2 - upgrades openssl to 3.0
* Fix rehash of ca certs to not utilize undefined behavior (Android only)

## 5.7.0

* Implement individual `DatasetDescriptor` removal for `PersistentRasterDataStore`

## 5.6.1

* ENGINE-787 Add `AttributeSetUtils` unit tests. Coverage for `AttributeSet`/`Map` conversions.
* Fix minor `AttributeSetUtils` bug. `Byte` vs. `byte[]` instanceof check. 

## 5.6.0

* Add, implement `DataModelSpi` and `AttributeSetDataModelSpi` in `:takkernel:shared`to provide generic backing collection support to new Fires models
* Add `AttributeSetUtils` to support `Map`/`AttributeSet` conversions
* Implement an `IMarshal` from `AttributeSet` to `NineLine`

## 5.5.4

* AtakCertificateDatabase: Don't bail out of validity checking after first certificate in store;  continue validating and return certificate with soonest expiration (ATAK-18566)

## 5.5.3

* ATAK-18672 Mitigate GLMapView calls to format for diagnostic performance hit

## 5.5.2

* Set the current GL context to associate the correct globe with the correct resources.

## 5.5.1

* Mitigate regression introduced with the embedded lo-res default elevation source for collision handling when no user elevation data is available
* Make `GLGlobe::handleCollision` a no-op for `CameraCollision::Ignore`
* Fix potential invalid access in `TileMatrixElevationSource`

## 5.5.0

* Add `SymbologyProvider` as static registry and symbology rendering service for `ISymbologyProvider` instances registered with the system
* Add 2525D `ISymbologyProvider` implementation to `:takkernel:controlled`
* Use _purple_ fill color for 2525 civilian unit icons
* Implement multi-point preview icon generation for 2525C desktop

## 5.4.2

* ATAK-18633 Playstore Crash: NullPointerException EUD Token Refresh

## 5.4.1

* Add per-vertex color and lighting support to mesh feature rendering

## 5.4.0

* Add MeshPointStyle

## 5.3.4

* Fix runtime issue in `GLMapView.inverse` with obfuscated lamda

## 5.3.3

* `GLLabelManager` only renders top-most label with `GLLabel::Hints::DisableDeconflict` toggled when there is an overlap

## 5.3.2

* Fix gravity for BOTTOM_EDGE widget layout.

## 5.3.1

* Feature point hit-test honors hit-test radius

## 5.3.0

* Add LaserBasket

## 5.2.0

* Create Wedge geometry in C++ and JNI / CLI

## 5.1.0

* Add `Nineline` and `Fiveline` data model definitions to `:takkernel:controlled`

## 5.0.1

* `CotEvent._readString` checks for `null`

## 5.0.0

* C++17
* Refactor `CotEvent` from ATAK application codebase into `:takkernel:shared`

## 4.32.2

* Suppress redundant model hit-test in CLI `GLMapView`

## 4.32.1

* Fix y-coordinate origin when Java `GLMapView` invokes through to `GLGlobe::inverse` for native scene hit-test

## 4.32.0

* Add `MagnifierLayer` and `CrosshairLayer`

## 4.31.0

* Export `mil-sym-reendeerer` as transitive dependency of AAR

## 4.30.1

* Implement World Mercator (EPSG:3395) projection

## 4.30.0

* `GLLabelManager` can configure minimum priority level of labels to be displayed

## 4.29.0

* Add JNI bindings for c++ model renderer.
* Include native HitTestControl results in ModelHitTestControl results.

## 4.28.1

* Fix various issues in `MilStd2525cSymbologyProvider` related to windows runtime integration

## 4.28.0

* Add RGB<>HSV conversions to `Color`

## 4.27.2

* Eliminate C++ warnings pending update to C++17 

## 4.27.1

* Bearing produces well-defined result for coincident points

## 4.27.0

* Introduce _Symbology Provider_ API

## 4.26.6

* CLI `GLMapView.targeting` property value properly honored by native peer

## 4.26.5

* ATAK-18540 Playstore Crash: Vector2D ArrayOutOfBounds

## 4.26.4

* `TerrainRGBChunkSpi` derives tile metadata from PNG custom chunk, when available

## 4.26.3

* `GdalDatasetProjection2` support for _geotransform_ accounts for proj -> image mapping being grid aligned vs pixel aligned. This addresses "seams" observed on image edges when processing heightmaps.

## 4.26.2

* GLBatchGeometryFeatureDataStoreRenderer CLI: Protect orphaned managed geometry from finalization until native call using its backing native object is completed (see WTK-10795)

## 4.26.1

* Fix crash when editing more than 128 features.

## 4.26.0

* Restore GLLabelManager hittest() method.

## 4.25.1

* CustomWmsMobacMapSource CLI: Use Invariant culture to format doubles for Bounding Box parameters in WMS query URL formation (see WTK-10777)

## 4.25.0

* Add support for rendering surface text.

## 4.24.7

* Check for valid non-empty bitmap data when detecting tile size from database.

## 4.24.6

* Better parsing of baseUri for external tile sets

## 4.24.5

* Update takthirdparty to 2.12.4 to pick up openssl and curl updates (see ATAK-18181 and ATAK-18331)

## 4.24.4

* Add version directive to depth sampler shader program
* Implement getDefaultTextFormat() on Windows platform

## 4.24.3

* Use altitude-adjusted PointStream to feed HeightOffsetPointStream

## 4.24.2

* Fix transpose type in `TerrainRGBChunkSpi` preventing correct interpretation of 4326 tiles

## 4.24.1

* Decode any potential URL encoding in GLBitmapLoader.getMountedArchive().

## 4.24.0

* `TileMatrixElevationSource` is hintable for orthometric vs ellipsoidal height
  * add `TilesMetadataControl` to allow pass through of metadata from client
  * `ElevationChunkSpi.Hints` adds metadata extras
  * `TerrainRGBElevationChunkSpi` assumes MSL by default
  * streaming tiles schema 2.1.0 adds metadata

## 4.23.4

* Handle null Style in getPointer()

## 4.23.3

* Apply relative display density to anchor X and anchor Y for corrected rendering when using anchors

## 4.23.2

* Add FileNotFound to the exceptions that can have subsequent error messages suppressed in MobacTileClient2

## 4.23.1

* Dispose TileContainer created in MobacTileClient2

## 4.23.0

* `android-port@5.2.0`
  * Consistent files/cache dir from `android.content.Context`
  * `SharedPreferences` persistent storage under `Context.getFilesDir()`

## 4.22.3

* Orderly destruct of CLI `ElevationHeatMapLayer`

## 4.22.2

* Update takthirdparty to 2.12.2 to include jassimp native builds on Windows

## 4.22.1

* `GLGlobe` screen-to-geo calculation does bounded check on _no-data_ tiles to avoid selecting data populated tiles on other side of the globe 

## 4.22.0

* Streaming Tiles framework support for tiled terrain data

## 4.21.0

* Add feature renderer option to hint client pre-sort of features

## 4.20.0

* Add `GLLabel::Hints::DisableDeconflict` to disable deconfliction on a per-label basis
* Expose new hint via CLI `GLLabelManager::setAutoDeconflict(...)`

## 4.19.9

* `GLLabel::setForceClampToGround(...)` applies to all anchor geometry types

## 4.19.8

* terrain renderer populates mesh for lowest level tiles
* `GLElevationHeatMapLayer` determines min/max based on all visible tiles

## 4.19.7

* v3 encoding of QHM tile data as architecture portable
* `ElevationTileClient` discards data that appears as corrupt on decode, avoiding potential segfault
* `LRUTileContainer` check for potential buffer-overwrite resulting in heap corruption in `BlockPoolAllocator`

## 4.19.6

* Suggestive change to Collection2::firstOrNull to prevent potential race condition on concurrent collections.

## 4.19.5

* CLI `GLBatchGeometryFeatureDataStoreRenderer` correctly interprets `GeometrySpecificRenderers` as filter rejects
* `GLBatchGeometryRenderer4` renders sprite lines before sprite polygons for translucent occlusion

## 4.19.4

* `CustomWmsMobacMapSource_CLI`: Fix incorrect string handling of URL (see WTK-10603)

## 4.19.3

* Eliminate dependency on `android.os.Build` for desktop runtimes

## 4.19.2

* signal that editing features are dirty after icon loader task completes

## 4.19.1

* Implement `NativeElevationChunk.sample(...)`
* `GdalElevationChunk` processes zip files

## 4.19.0

* Add `FeaturesLabelControl` to allow application to customize feature labeling defaults

## 4.18.2

* implement support for top-level <Region> KML tags to derive LOD information

## 4.18.1

* fix leaked `ElevationChunk` in `TiledElevationSource`
* no need to `TerrainRGBCache`
* track various managed objects returned from Java with `JNILocalRef`
* apply additional maximum resolution constraints in `TiledElevationSource` query implementation

## 4.18.0

* add `ElevationSourceControl` to allow configuration of the source used for terrain rendering

## 4.17.0

* Add a global, low resolution dataset derived from DTED0
* `DTEDSampler`
  * use single file handle for lifetime of sampler
  * only read header data once
* `ElevationTileClient` is extensible, to include heightmap construction
* `ElMgrTerrainRenderService` composes several different `ElevationSource` instances for the default `ElevationSource`

## 4.16.7

* replace all floating point calls to `abs()` with `fabs()` 

## 4.16.6

* replace use of regex in Linux with alternative implementation

## 4.16.5

* address nullpointer dereference and return null if the OAUTH tokenData is null which would occur if it has been revoked - client side usage indicates this is the appropriate behavior

## 4.16.4

* check label lock before updating replace_labels_ to ensure new labels are placed properly

## 4.16.3

* `ElMgrTerrainRenderService` minimum terrain fetch level becomes implementation detail via a _resolution constrained_ `ElevationSource` wrapper 

## 4.16.2

* add version directive for batch point geometry shader

## 4.16.1

* return null on caught exception in `ZipBitmapLoaderSpi`

## 4.16.0

* Add support for ArrowHeadMode

## 4.15.0

* update takthirdparty to 2.12.0, most notably updating openssl to 1.1.1v
  * For platforms that dynamic link, the openssl DLLs have changed names (lib.ext.1.1 to lib.ext.81.1.1)

## 4.14.10

* `StreamingTileClient` defers instantiation of `auth` until tile request to avoid circular dependency at application layer that cannot be easily unwound

## 4.14.9

* Minor fixes for 3D Tiles support based CESIUM ion integration
  * fix potential illegal access to `&std::vector.at(0)`
  * store bounds in `ModelInfo.metadata` to allow for downstream to use appropriate geometry on original insert 
  * add `RenderContext.requestRefresh()` on content changed and content loaded

## 4.14.8

* fix crash when inserting a label point feature with a double quote

## 4.14.7

* fix regression that caused circular polylines to be over-simplified (regression introduced `4.12.6`) 

## 4.14.6

* drawing tool `Pen` specifies extrude mode as per-vertex by default (regression introduced `3.4.0`)

## 4.14.5

* micro-optimize `GLBatchGeometryRenderer4` surface hit-test via application of scissor to limit fill rate
* `GLBatchGeometryFeatureDataStoreRenderer2.hitTest` only applies for FIDs

## 4.14.4

* Fix regression when rendering stroked polygon holes.

## 4.14.3

* implement deferred coverage computation for mosaics
* mosaicdb uses MBR intersect rather than spatial index
* eliminate erroneous double query in `FDB.getFeature`
* fix query params setup in `OutlinesFeatureDataStore2`
* hide but don't evict datasets on external SD
* `ServiceProviderRegistry` utilizes R/W mutex to allow for concurrent processing
* remove dead/redundant code between `LayersDatabase` and `PersistentRasterDataStore`
* `LayersDatabase` monitors for deferred coverage computation
* `GLMosaicMapLayer` micro-optimizations
  * interleave dataset preloading with query rather than post-process step
  * utilizes thread-pool for async dataset loading
  * preload into live renderables list (use `ConcurrentSkipListMap`)
* `GdalLayerInfo` explicitly closes (aka `Dataset.delete()`) opened datasets during processing
* `GdalLibrary.openDatasetFromFile` micro-optimization
  * attempt initial open with subset of prioritized drivers (NITF, MrSID, GeoTIFF)
  * NITF driver open suppresses checks for most "sidecar" metadata files 

## 4.14.2

* Use corrected string for filename comparison in `ZipFile::Impl::gotoEntry`. (WTK-10501)

## 4.14.1

* Cover all `addPoint` overloads with patch from `4.7.2` to address envelope invalidation on first point add. (WTK-10460)

## 4.14.0

* Add CLI bindings for `FeatureEditControl`

## 4.13.0

* Add JNI bindings for ArrowPatternStyle

## 4.12.7

* Fix for upper hemisphere blurring

## 4.12.6

* `GLGeometryBatchBuilder` implements in-line simplification to better balance geometry preservation concerns

## 4.12.5

* Format numbers destined for DB using invariant culture

## 4.12.4

* Fix GLDrawableWidget rendering on desktop platform

## 4.12.3

* Calculate minZoomLevel for MBTiles instead of relying on meta data

## 4.12.2

* `GLBatchGeometryRenderer4` lollipop rendering uses instanced triangles instead of line strip

## 4.12.1

* Fix handling of jar URIs on desktop platforms

## 4.12.0

* Add support to visit native Layer Controls in Java
* Add LegendControl and JNI bindings
* Add config option to disable legend drawing for terrain-slope rendering

## 4.11.1

* `OutlinesFeatureDataStore2` refresh query fix

## 4.11.0

* Add click gesture count to `MotionEvent` for desktop environment

## 4.10.0

* Change GLGlyphBatchFactory API to allow assumption of ownership of resources passed to it. Clean owned resources.

## 4.9.1

* Prevent GLGlyphBatch namespace from retaining references to shaders

## 4.9.0

* GLBatchGeometryFeatureDataStore2.SPI fix to prevent leaking a wrapped managed datastore

## 4.8.7

* Narrow feature label default text for only point geometries
* Fix potential out of range exception in `GLLabel`

## 4.8.6

* Massage `HttpProtocolHandler` logic to wait for initial 401 response before authentication attempts

## 4.8.5

* BuilderQueryContext in GLBatchGeometryFeatureDataStoreRenderer3 was creating new labels instead of updating existing. deferredRemoveIndex should be reset to 0 every feature to allow correct label reuse.

## 4.8.4

* Use the formed sql statement instead of the hardcoded string which will make use of the correct number of bind arguments

## 4.8.3

* Bump android-port version to include fix for spaces in Uris on desktop platforms

## 4.8.2

* Use feature name as default when label style has no text

## 4.8.1

* Fix issue where fontName in `GLLabelManager::setTextFormat` was prematurely freed when marshal_context went out of scope.
* `GLLabelManager::toGlyphBufferOpts` only defaults font face if it is not supported by either the SDF or legacy glyph renderers

## 4.8.0

* `libcurl` HTTP client utilizes system CA store on windows
* `IHttpClient` option for explicitly disabling SSL peer verification

## 4.7.2

* `GLBatchGeometryFeatureDataStore::updateFeature` uses appropriate `Feature2` constructor to handle potential `nullptr` fields

## 4.7.1

* Fix C++ `LineString` envelope cache implementation for initial point add

## 4.7.0

* Add font face and style JNI bindings.
* Fix default glyph atlas assets uri

## 4.6.4

* Fix `GdalBitmapReader` scale to 8-bit

## 4.6.3

* `GLMosaicMapLayer2` filters out selected type if not visible

## 4.6.2

* Attempt to coerce bitmap format if `GLUtils.texImage2D` fails

## 4.6.1

* `GLBatchGeometryRenderer4` lollipops rendering needs to set `u_hitTestRadius`

## 4.6.0

* Update to using yet another new root certificate for Digicert

## 4.5.5

* Set level of detail on labels generated by the features renderer

## 4.5.4

* Update CI to address failure to override group variables

## 4.5.3

* Add gitlab pipeline job in the publish stage to apply a `git tag` of the TAKKernel version.  The job follows the same rules as other jobs in the publish stage.  It will run automatically for the default branch (master) and can be manually run for maintenance branches.

## 4.5.2

* Default `GLLabel` horizontal alignment should be `TEHA_Center`

## 4.5.1

* `GLBatchGeometryRenderer4` reorders sprites primitive rendering to allow for points displayed in translucent polygons

## 4.5.0

* Add rendering pixel offset values for point geometry. 

## 4.4.1

* Eliminate latency in `GLLabelManager` when evaluating labels outside of the visible terrain mesh

## 4.4.0

* Add LevelOfDetailStyle JNI bindings.

## 4.3.0

* Add `FeatureEditControl` for interactive editing of individual features during batch rendering
* C++ `GLBatchGeometryFeatureDataStoreRenderer3` adds `FeatureEditControl` support

## 4.2.8

* Micro-optimize `CoordinatedTime`

## 4.2.7

* Auto-scale feature icons based on globe zoom (50% at low zoom levels)

## 4.2.6

* Fix typo in hemisphere normalization during terrain tile selection

## 4.2.5

* `GLBatchFeatureDataStoreRenderer3` begins and ends batch at top level query to aggregate features for both hemispheres for IDL crossing subqueries
* `GLGeometryBatchBuilder` handles IDL crossing when generating primitives

## 4.2.4

* Batch point shader adjusts point sprite size to eliminate clipping of rotated icons

## 4.2.3

* Fix shift operator typo in `GLBatchGeometryShaders` for shader deconfliction key

## 4.2.2

* Allow using a URI to specify font atlas path.

## 4.2.1

* Micro-optimize C++/CLI -> C-string marshaling in `GLLabelManager`

## 4.2.0

* `FBD2` handles update mask
* Java `FeatureStyleParser` defers to C++ implementation
* `GdalBitmapReader` fix band mapping request for monochrome+alpha datasets
* `GLBatchGeometryFeatureDataStoreRenderer3` only processes labels on feature version update
* `GLBatchGeometryFeatureDataStoreRenderer3` supports label hit-test for label only features
* * `GLBatchGeometryFeatureDataStoreRenderer3::hitTest2` implement support for hit-test radius
* `GLGeometryBatchBuilder` handles explicit `LabelPointStyle`
* correct javadocs for `IconPointStyle`
* add style support for extrusion stroking
* `GLLabel` supports `Polygon2` geometry
* `GLBatchGeometryRenderer4` considers opaque black as _no-hit_ during hit testing
* `GLBatchGeometryRenderer4` explicitly toggles point sprite rendering for desktop GL runtimes
* `GLBatchGeometryShaders` disambiguates cached program on _all_ options settings
* JNI local reference cleanup for native event queuing

## 4.1.4

* `JNIEnv` thread-local-storage caching ignores external native threads

## 4.1.3

* Remove reference to pre-4.0 header in C++ test project

## 4.1.2

* Allow some overlap between labels before hiding during deconfliction

## 4.1.1

* Fix leaking `Pipe` in `LibcurlHttpClientBuilder` when exception is raised during `ResponseImpl` construction

## 4.1.0

* Enable C++ `GLBatchGeometryFeatureDataStoreRenderer3` via JNI bindings
* `LocalJNIEnv` attaches to thread at most once
* implement JNI wrappers for `FeatureDataStore`, `FeatureCursor`, `FeatureSetCursor`

## 4.0.3

* Create bindArgs collection of correct size in LazyQueryAdapterForward::BindImpl

## 4.0.2

* C++ `GLText2::draw` needs to set matrices on `GLRenderBatch2`

## 4.0.1

* Fix stackoverflow/infinite recursion from bad search-and-replace in `GLES20` and `GLES30` Android source set

## 4.0.0

* Remove deprecated APIs
* Update, remove or mark as implementation various dependencies

## 3.42.7

* Fix C++/CLI ManagedFeatureDefinition2 to properly convert feature name from managed to native (WTK-10174)

## 3.42.6

* Use whitePixel texture from GLMapRenderGlobals in GLGlobe

## 3.42.5

* Update to Android Gradle Plugin 7

## 3.42.4

* Adjust precision for ID ignore in batch geometry shaders (~1/4 pixel intensity)
* `GLBatchGeometryRenderer4::hitTest` early exit on elapsed time requires multiple results

## 3.42.3

* Micro-optimize C++ `calculateRange` iteration

## 3.42.2

* Bump android-port version to include VertexAttribArray fixes

## 3.42.1

* Associate VertexAttribArray instances per-GLContext

## 3.42.0

* Add transaction support to `FDB2` as implementation detail of `acquireModifyLock`/`releaseModifyLock`.

## 3.41.0

* Fix label flickering issue on rapid feature update
* Fix zombie first label issue and other issues caused by valid/invalid zero label ID mismatch

## 3.40.1

* Implement more orderly disposal for CLI `SceneLayer`

## 3.40.0

* Fix various path-based issues with SQLite tile containers rendering support on windows
* implement `TileMatrix.Utils.isQuadtreeable`
* GeoPackage tile containers render via `GLQuadTileNode4` if quadtreeable

## 3.39.3

* Fix TileProxy constructor not calling delegating constructor properly

## 3.39.2

* Reject default GDAL geotransform as being valid

## 3.39.1

* Add rendering support for point features with `BasicPointStyle`

## 3.39.0

* Desktop runtime honors platform relative display scaling
* `TextWidget` employs anti-aliasing on desktop
* `RootLayoutWidget` border layouts are now functional
* Add `WidgetLayer` and `GLWidgetLayer`

## 3.37.2

* Fix URL encoding in GDAL path on Windows

## 3.37.1

* Fix `ZipFile` table of content missing folder issues with regard to loading and rendering zipped Cesium 3D Tile sets
* Fix crash at shutdown in CLI environments when Cesium 3D tileset is visible on screen

## 3.37.0

* Update to using the new root certificate for Digicert

## 3.36.4

* Handle windows pathing properly when constructing URI.

## 3.36.3

* Add support for Android x86_64 (ATAK-17332)

## 3.36.2

* Fix intermittent animation issue on scrolling text labels

## 3.36.1

* Change native dependencies (libLAS/LASzip, pri, takthirdparty) to versions built against Android 21 to match minimum API version used by ATAK (ATAK-17496)

## 3.36.0

* Add JNI bindings for createHeightmap().

## 3.35.7

* `GLGeometryBatchBuilder` specifies default hints for labels

## 3.35.6

* Normalize tile mesh SSE for foreground tiles based on minimum SSE for highest level tile intersecting viewport

## 3.35.5

* Assume LLA projection if only GeoTransform is provided in GDAL dataset.

## 3.35.4

* Detect MPU and employ workarounds for line drawing

## 3.35.3

* Optimize for KMZ DAE with large numbers of meshes
  * `GLScene` uses thread-pools for initialization
  * Share per-context resources such as `GLSceneNodeLoader`, `MaterialManager`, etc. across multiple `GLScene` instances
  * pre-sort terrain tiles based on camera location at time of request
  * loop optimization based on profiling
  * only display one indicator per KMZ file

## 3.35.2

* Handle WFS sources with swapped X, Y axes

## 3.35.1

* Clear deprecated resource value pending removal

## 3.35.0

* Move `dynamic_cast` occurrences to respective `takenginejni.so` and `takengine.so` boundaries to fix `dynamic_cast` failure on Android

## 3.34.5

* Limit smart cache queue backlog to 4 requests

## 3.34.4

* Java _Native Imagery_ layer renders other types as background when _locked on_ to a specific type

## 3.34.3

* `libcurl` HTTP client uses pool of `CURL` handles
* augment error handling and propagation for `libcurl` HTTP client

## 3.34.2

* `libcurl` based HTTP client uses Android system CAs

## 3.34.1

* Implement `ImageryRelativeScaleControl`

## 3.34.0

* Introduce new configuration file and tile reading machinery for robust description of streaming imagery sources
* Add HTTP client abstraction interfaces and implementation

## 3.33.1

* Caching Service only uses most recent default for smart cache triggers without explicit source/sink
* employ comparator for source/sink defaults list

## 3.33.0

* Add greenrobot `EventBus` to aarbundler

## 3.32.1

* `CachingService` adopts min/max res from source when not explicitly specified
* CLI `CachingService` does not raise exceptions on event inputs

## 3.32.0

* Update takthirdparty and libLAS to versions built using NDK 25b (ATAK-17371)

## 3.31.0

* Add ConfigOption to enable debug features for the CachingService
* Native crash fixes for CachingService

## 3.30.4

* Dispose InsertContext object in insertFeatures() method.

## 3.30.3

* Fix support for devices running Android 23 and lower

## 3.30.2

* Caching Service disables smart caching by default

## 3.30.1

* Fix for linux test

## 3.30.0

* Add Enable / Disable to Smart Caching Service

## 3.29.1

* Key the native to managed wrapper map using a weak_ptr instead of memory address.

## 3.29.0

* C++/CLI `UpdateCacheSource` event uses 1:1 source:sink mapping

## 3.28.1

* Add ConfigOption to request suppressing duplicate logs. Implement support in GLQuadTreeNode4 and MobacTileClient2.
* Replace System.out() during GDAL initialization with proper logging.

## 3.28.0

* Add download limit for smart cache requests

## 3.27.1

* Implement `BitmapFactory2_encode`

## 3.27.0

* Add hit-testing to `GLLabelManager`

## 3.26.2

* Reference takthirdparty 2.8.6 to pick up openssl update to 1.1.1t

## 3.26.1

* TileContainerFactory only creates on TileContainer per path / readOnly combo

## 3.26.0

* Add DTED support clases.

## 3.25.1

* Allow hit-test on x-ray portion of arrows

## 3.25.0

* bump to making use of NDK 25B LTS

## 3.24.0

* `MapSceneModel2` / `CameraController` fix look at negative HAE locations
* Add `DrawableWidget`
* Add `JoystickWidget`

## 3.23.1

* Unregister managed Tile Clients to prevent deadlock

## 3.23.0

* Add progress callback for caching service
* Implement `CachingService_cancelRequest`
* Caching service supports multiple sources/sinks

## 3.22.0

* Add CLI SPI for MobacTileClient and OSMDroidContainer


## 3.21.3

* Database2: Maintain sqlite user_version on cipher update following rework of cipher update code in 3.21.2


## 3.21.2

* Database2: Preferred cipher parameters on ARM32 Android runtimes has been changed to be less computationally intensive.  A migration path from the prior cipher parameters is provided. Cipher parameters on other platforms is unchanged. See ATAK-17199

## 3.21.1

* Java `TileClientFactory` allows for `null` options

## 3.21.0

* Dont allow for obfuscation of com.atakmap.util.Visitor

## 3.20.0

* Introduce `LevelOfDetailStyle` that allows for associating display thresholds against a style
* Update batch geometry shaders to perform vertex discared based on LOD
* Add min/max LOD thresholds to `GLLabel` and `GLLabelManager`

## 3.19.0

* Add JNI bindings for `tilematrix` API

## 3.18.0

* Add C++ and C++/CLI _clamp-to-ground-control_
* Integrate _clamp-to-ground-control_ support in C++ `GLBatchGeometryRenderer4` and `GLLabelManager` 

## 3.17.0

* Introduce portable plugin API for User Interface
  * `IHostUIService` defines entry points for integrating with host application UI
  * `ToolbarItem` as abstraction for nav bar/ribbon integration
  * `IPane` as opaque abstraction for user UI component
* Add `Drawable` and `Bitmap` to portability API

## 3.16.0

* Add C++/CLI _Thread_ API to allow injection of thread factory to eliminate app domain issues for automated tests 

## 3.15.4

* Java 3D Tiles renderer insets mesh into existing scene to mitigate depth fighting with local terrain sources.

## 3.15.3

* Fix handling of GDAL paths on desktop systems

## 3.15.2

* Update FileSystemUtils to only restrict root filesystems on Android. Disallow forward slashes in Windows file names.

## 3.15.1

* Fix reuse of iterator after erase()

## 3.15.0

* Add `CacheEventService` to java engine implementation

## 3.14.0

* `GLLabel` utilizes more performant _fallback_ terrain mesh elevation lookup
* Refactor elevation fetch from `TerrainTile` into global function for re-use

## 3.13.0

* Add CLI cache event classes and tests

## 3.12.4

* Implement batched notifications in LocalRasterDataStore

## 3.12.3

* Ignore IO errors when extracting kernel resources

## 3.12.2

* `LRUTileContainer` uses abstraction layer for DB open
* JNI `Bindable` API initializes on entry

## 3.12.1

* Fix implicit label inserts in `GLLabelManager` resulting in phantom labels

## 3.12.0

* Add additional units to `Angle`

## 3.11.2

* Update takthirdparty to 2.8.5, for new cng-engine build that resolves a multithread issue with smart card readers.

## 3.11.1

* C++ `CatalogDatabase2` handles paths with extended characters on windows

## 3.11.0

* `CachingService` Java API

## 3.10.0

* Add Geometry classes to Java API
* Add Caching Service support events to Java API

## 3.9.2

* Update takthirdparty to 2.8.4, for new gdal build that resolves issues with `BIT Systems` WFS servers.

## 3.9.1

* Fix WinTAK build issue with CachingServiceTest.

## 3.9.0

* Add CLI bindings for `CachingService`

## 3.8.4
* Use a point in polygon test for feature hit testing on desktop.

## 3.8.3
* Fix string conversion to/from native UTF8 to/from CLI managed in additional Feature api related classes (WTK-9498)

## 3.8.2

* Fix PersistentDataSourceFeatureDataStore2 to avoid trying to open a temporary file using a name derived from the source filename for both security reasons and because the engine IO apis do not properly support utf8 strings containing extended characters in filenames (see WTK-9483)
* Fix string conversion of CLI managed string to UTF8 for file paths in NativeDataSourceFeatureDataStore2::add() (WTK-9483)
* Fix string conversion from native UTF8 to CLI managed in several Feature api related classes (WTK-9496)
>>>>>>> master

## 3.8.1

* Update takthirdparty to 2.8.3, for new gdal build that resolves an issue sending a bounding box filter to some WFS servers.

## 3.8.0

* Add `CachingService` C++ API stub

## 3.7.1

* Migrate to Gradle build system 7.5.1-all from 6.9.1-all

## 3.7.0

* Enhanced read-only fileset import preference to support WinTAK

## 3.6.3

* Add support for KML style maps with _cascading style_ and CDATA hrefs
* Fix memory leak when parsing KML style map

## 3.6.2

* Shapefile driver default styling for polygons is stroked, not filled.

## 3.6.1

* Add DB trigger to update version field on feature change

## 3.6.0

* Include `org.greenrobot.EventBus` with public API

## 3.5.3

* Fix string conversion from CLI managed strings to UTF8 in CLI binding for Database2

## 3.5.2

* Micro-optimize Java `GLTextureCache` by way of clearing cache content via bulk `glDeleteTexture` per some buffer to reduce thrash once limit is reached.

## 3.5.1

* Java `GLBatchPoint` applies outline if there is no label background
* `GLLabelManager` correctly invalidates when locking/unlocking labels
* `GLLabelManager` reduces refresh request spamming

## 3.5.0

* `DefaultContactStore` accepts `ExecutorService` via package private constructor to eliminate dependency on asynchronous evaluation in unit tests

## 3.4.2

* `GLLabel` assignment operator needs to preserve `mark_dirty_` flag for proper invalidation

## 3.4.1

* `GLLabelManager` marks self as _invalid_ on label updates when labels are not locked

## 3.4.0

* Add `IContactService.getAllContactsOfType`
* Add new `IContact` methods: `getProtocols`, `setProtocols`, and `supportsProtocol`

## 3.3.12

* `MobileOutlinesDataStore2` coverage resolver catches and quietly fails on unhandled exceptions

## 3.3.11

* `PersistentRasterDataStore.DatasetCoverageResolver` catch and quietly fail on unhandled exceptions

## 3.3.10

* `GLAntiAliasedLine` stores _pattern_ as instance geometry and updates on every draw

## 3.3.9

* Replace problematic _split-string-on-newline_ in `OGR_Content2` feature name retrieval with _replace-newline-with-space_ to eliminate JNI `NewStringUTF` crash and for better consistency with Google Earth. Note that changing to `std::getline` also eliminated the crash as an alternative replacement implementation for legacy.
* `OGR_Content2` coerces `DriverDefinition2` to produce default style when style link is defined but cannot be resolved
* Fix bug with file overlays labels not appearing on restart

## 3.3.8

* Normalize tile size for `GdalTileReader` to avoid degenerate tile (entire image, scanline, etc.)

## 3.3.7

* Java `GLQuadTileNode` manages FBO for texture copy more tightly
* Use cache instead of spamming `NodeContextResources` instances

## 3.3.6

* Fix C++ `FDB` argument binding order for feature visibility toggle

## 3.3.5

* Fix rendering of IconPointStyle rotation - see ENGINE-603

## 3.3.4

* Tear down GDAL drivers during shutdown hook to ensure deletions happen while takkernel is still loaded.

## 3.3.3

* `AssimpModelSpi` return source primitive draw mode
* Java `GLMesh` renders lines meshes

## 3.3.2

* Java batch polygon rendering appropriately strokes polygon holes

## 3.3.1

* Fix camera controller panning issue where focus point in western hemisphere becomes offset 180 degrees to the east

## 3.3.0

* Make the read-only attribute of imported feature sets configurable

## 3.2.1

* Cap overlay files minimum display threshold at ~10m

## 3.2.0

* C++ `GLBatchGeometryFeatureDataStoreRenderer3` adds `Options` struct for construction
* C++ `GLBatchGeometryFeatureDataStoreRenderer3` passes through geometry filters via query params
* Introduce `GLGeometryBatchBuilder::Callback2` as subinterface to allow `GLLabel` by r-value to eliminate copies
* `GLGeometryBatchBuilder` micro-optimizes labeler via lazy instantiation of `GLLabel` and label text
* `GLLabel` micro-optimizes heap alloc for `Point` geometries via stack memory
* C++ `GLAsynchronousMapRenderable3` only requests refresh if not already invalid
* CLI `GLBatchGeometryFeatureDataStoreRenderer` adds `Options` struct for construction
* CLI `GLBatchGeometryFeatureDataStoreRenderer` allows for geometry-type based native renderers for better throughput
* CLI `AbstractFeatureDataStore2` micro-optimizes array view of listeners by obtaining on change
* Fix exception handling in `ManagedFeatureCursor`
* `ManagedFeatureDefinition` allows for instance re-use in `ManagedFeatureCursor` to micro-optimize heap allocs
* Add C++ `StringMap` and `StringSet` to micro-optimize away implicit `std::string` or `Port::String` construction/copies for _string_ based containers

## 3.1.2

* Handle potential invalid cursor column access for C++ `FDB` feature queries

## 3.1.1

* Control geometry simplification per _clamp-to-ground_ rather than 2D vs 3D geometry type

## 3.1.0

* Added code to more closely match Google Earth behavior with regards to markers missing icons

## 3.0.2

* Fix issue when using interleaved data with `VertexAttribArrays`.
* Use `glVertexAttribIPointer()` with integer attribute `a_pattern`.

## 3.0.1

* Add lines support to `ASSIMPSceneSpi`
* Add `GL_LINES` support to `GLMesh`

## 3.0.0

* remove dependency on `avro`
* remove depenedency on `hibernate`
* bump `org.json` dependency
* add explicit dependency on `jackson` post `hibernate` removal
* remove deprecated _Contacts_ APIs
* remove deprecated `IGlobeEvent` APIs

## 2.14.2

* Use platform specific file separator for `NativeImageryMosaicDatabase2` layer key.

## 2.14.1

* Ensure orderly destruct order of Java `Globe` and `GLMapView`
* Address ambiguity when registering multiple `FeatureDataSource2` instances against same name

## 2.14.0

* Add C++ and C++/CLI `ArrowStrokeStyle`
* C++ Features renderer supports `ArrowStrokeStyle`

## 2.13.0

* Implement C++ `GLBatchGeometryRenderer4` multiple hit-test results via _primitive discard_ in the shader

## 2.12.1

* Address potential race in `GLBatchGeometryFeatureDataStoreRenderer3::hitTest2`
* Address potential `nullptr` to `std::string` conversion

## 2.12.0

* Introduce callback based parse for C++ `GeometryFactory`

## 2.11.0

* Apply significant micro-optimizations to `GLLabelManager`
* Add `String_intern`

## 2.10.6

* Fix regressions in `GdalLayerInfo` introduced when improving cross platform support

## 2.10.5

* Update takthirdparty to 2.8.2, for new gdal build that modifies the https client certificate authentication support.  If a public cert is found for an issuer but unable to get the private key, look for another cert from the same issuer. (WTK-8939)

## 2.10.4

* Update takthirdparty to 2.8.1, for new gdal build that modifies the https client certificate authentication support to only use certs that are valid for client authentication. (WTK-8939)

## 2.10.3

* Fix for opening of zip files for zip:// protocol when UNC path is specified on Windows

## 2.10.2

* Fix for changing the position of a layer in a MultiLayer

## 2.10.1

* Use shared LRU cache db for terrain cache against a given path
* Don't treat failure to update last access time as an error in `LRUTileContainer::getData`

## 2.10.0

* Add CLI binding for `ProtocolHandler`

## 2.9.1

* Micro-optimize C++ `GLBatchGeometryRenderer3::extrudePoints` via eliminating dynamic allocations

## 2.9.0

* Various fixes for running TAK Kernel in Windows environments. Added SystemUtils for OS detection.

## 2.8.3

* C++ `LineString` allows empty geometry

## 2.8.2

* C++ `GLBatchGeometryRenderer3` correctly colorizes point sprites

## 2.8.1

* ATAK-16509: Fix GLBitmapLoader caching files which have been changed

## 2.8.0

* add weighted CLI `GeoCalculations.PointAtDistance`
* implement CLI `AttributeSet.setAttribute(key, arraytype)`
* add CLI `Envelope2` copy constructor
* make CLI `GLLayerFactory2` more .NET friendly
* add flag to control global tessellation for V3 features renderer
* add CLI `FeatureDataStore2` wrapper


## 2.7.2

* Add back in the number class for the regular expression used by FileSystemUtils

## 2.7.1

* Expand the regular expression to include the entire letter class

## 2.7.0

* IPlugin should not be obfuscated.

## 2.6.1

* `NativeFeatureDataStore_CLI` and `FeatureSetDatabase_CLI` eliminate read-lock acquisition in disposer to avoid deadlock

## 2.6.0

* Java anti-alias line render uses C++ shader
* Modify anti-alias line rendering to utilize instancing

## 2.5.0

* Update takthirdparty to 2.8.0, picking up new gdal build to add https client certificate authentication from Windows certificate store and adding openssl support to gdal on windows (WTK-8939)

## 2.4.1

* For Android Apps targeting Build.VERSION_CODES.R on devices running OS versions Android R or later, onSharedPreferenceChanged will receive a null value when preferences are cleared.

## 2.4.0

* Add _very_ basic support for line joins in C++ anti-alias line renderers, applied to surface lines

## 2.3.0

* Refactor `ElMgrTerrainRenderService` to construct mesh from an `ElevationSource` instance as opposed to `ElevationManager`
* Introduce `ElevationTileClient` for heightmap tile based access to registered elevation sources
* Introduce `TileMatrixElevationSource` to implement `ElevationSource` via a `TileMatrix` instance
* Add `BlockPoolAllocator::allocate<T>` for typed buffer block allocation
* Eliminate redundant `GLMapView` instantiation in default Android `RenderSurface` factory

## 2.2.1

* CLI `GLMapView::Inverse` eliminates per-object overhead for scene hit-testing
* Address unlikely, but potential race, in `GLBatchGeometryFeatureDataStoreRenderer3` hit-test

## 2.2.0

* Prevent mixing of MSDF and legacy font rendering within the same label.
* Add additional commonly used glyphs to pregenerated font atlases.

## 2.1.0

* Enable WinTAK Graphics Features Rendering

## 2.0.1

* Compilation fixes for Ubuntu Linux compiling Android from takkernel only using NDK 23B.  Do not see any issues compiling takkernel from ATAK proper.

## 2.0.0
* Update takthirdparty dependencies to address security reports (WTK-8925)
* Database2/SpatiaLiteDB: Add major version upgrade migration best practices when opening keyed SQLCipher databases
* Note: sqlcipher has been updated a full major version. Users opening databases without using the built-in keying support in takkernel (and doing keying themselves) will need to account for proper migration. See https://discuss.zetetic.net/t/upgrading-to-sqlcipher-4/3283

## 1.31.1

* Only simplify for non-3D geometries

## 1.31.0

* Add IGlobeEventService2 interface

## 1.30.0

* Fix texture loading issues for C++ mesh renderers

## 1.29.0

* remove com.atakmap.util.Disposable per the deprecation policy

## 1.28.2

* `FDB` guards database access against post-dispose invocation
* `OutlinesFeatureDataStore2` unsubscribes itself as listener on dispose

## 1.28.1

* Fix DTED cell name derivation for W001 and S001

## 1.28.1

* `LASSceneSpi` guards non-reentrant libLAS API with mutex

## 1.28.0

* Add `GLGlobeBase::RenderPass::SurfacePrefetch` and deprecate `GLGlobeBase::RenderPass::Surface2`.

## 1.27.2

* Change `GLGlyphBatch` to use attribute based batch rather than packing glyph data in a texture to elinate texture fetch roundtrip issues for some hardware.

## 1.27.1

* Revert thread-naming mechanism for windows and constrain to debug mode to address deadlocks observed after `1.23.2`

## 1.27.0

* Add computeGridConvergence() and NorthReference for bearing conversion

## 1.26.0

* Introduce new shaders supporting `GLLinesEmulation` for anti-aliasing during emulated line rendering

## 1.25.0

* Add `IResourceManager` as portable mechanism for well-defined access to resources packed in library

## 1.24.0

* Made max SSE configurable

## 1.23.2

* Changed code for setting thread name that caused an issue on Win10 LTSB v1607

## 1.23.1

* Fix globe rendering for Samsung Tab 4; valid attributes may have any value >= 0

## 1.23.0

* Mark GeoPoint as @DontObfuscate

## 1.22.2

* Add tessellation to `LineStringPointStream`

## 1.22.1

* Fix modification-while-iterating in `GLMegaTexture::release`

## 1.22.0

* Introduce unified `GeoCalculations` API
* Enable support for `:takkernel:engine` instrumented tests

## 1.21.1

* `GLAsynchronousMapRenderable` tunes query GSD based on camera distance from local terrain

## 1.21.0

* Replace instances of TE_Unsupported with TE_InvalidArg and TE_NotImplemented

## 1.20.2

* `NullPointerException` in `GLAsynchronousMapRenderable` when marking regions dirty after release

## 1.20.1

* ATAK-16213: Remove exception throwing from Databases_openDatabase() internals to mitigate crashes during exception unwinding

## 1.20.0

* Add .NET binding for `IGeoPoint`

## 1.19.0

* Contacts API adopts common vocabulary

## 1.18.0

* Introduce `IGeoPoint` interface
* Introduce _Globe Interaction Events API_

## 1.17.0

* Deprecate Android resource defaultTrustStorePassword and remove singular use thereof

## 1.16.2

* `GLQuadTileNode4` dynamically selects resolution circa poles to reduce tile count
* `GLQuadTileNode4` quickfix for tile composition bug

## 1.16.1

* `AsynchronousIO2` uses thread-safe variable during sort

## 1.16.0

* Add Java `IPlugin` interface as base for shared plugin entry point
* Add `IServiceController` as opaque mechanism for service query and component registration

## 1.15.1

* Update floating-point literal declaration in `GeomagnetismHeader` as _long double_ is not interpreted as expected in some runtimes

## 1.15.0

* Reduce pixelation past 67th parallel resulting from artificial suppression of surface tile resolution

## 1.14.0

* Restore the IOProviderFactory registerProvider(IOProvider provider, boolean default) method.   No impact on obfuscation for the client but does introduce an API change.

## 1.13.0

* `GLMosaicMapLayer` fetches at current GSD rather than modifying
* `NativeImageryMosaicDatabase2` adjust GSD for native imagery mosaic DBs to match legacy UX
* _GLAsynchronousMapRenderable_ constrains query GSD per nominal max surface GSD to reduce number of frames fetched while terrain is being resolved
* Introduce _GLAsynchronousMapRenderable_ `dirtyRegions` to provide explicit mechanism for marking surface dirty per query results
* Micro-optimize `SQLiteSingleTileReader` construction

## 1.12.0

* reimplement tile read prioritization for Java `TileReader` framework

## 1.11.0

* Improve support for multiple globes/multiple render contexts within a single runtime

## 1.10.7

* Only draw borrowed tile if we have no pre-existing data or only partial pre-existing data

## 1.10.6

* Correctly marshal Unicode strings between C# and native in NativeStatement2::Bind and NativeQuery2::GetString.

## 1.10.5

* Advance streambuf end pointer in DataInput2Streambuf

## 1.10.4

* `GLQuadTileNode4` address potential `NullPointerException` for renderers without surface control

## 1.10.3

* `DatabaseInformation.getUri(String &)` should not check for `NULL`-ness

## 1.10.2

* `GdalTileReader` supports concurrent read lanes for different subsample rates

## 1.10.1

* `GLGlobeSurfaceRenderer` sorts dirty tiles before updates

## 1.10.0

* Java `GLQuadTileNode4` prefetches nodes to be rendered based on surface bounds at render pump start

## 1.9.0

* Refactor outlines data store to `:takkernel:engine`

## 1.8.0

* Refactor `DatabaseInformation::getUri()` and `::getPassphrase()` `const char *` -> `TAK::Engine::Port::String` to retain ownership over memory

## 1.7.3

* `GLLabelManager` interprets text size of zero as default for consistency pre `0.61.0`

## 1.7.2

* Make `OGR_Content2` constructor exception free
* `OGR_Content2` constructs path via chaining as appropriate
* make `NativeFeatureDataSource` JNI implementation more permissive, but stay within bounds of contract, to avoid unhandled exceptions

## 1.7.1

* Initialize various fields on `Shader` and `Shader2` structs to address misbehavior observed in some runtimes

## 1.7.0

* Overload _interactive_ `CameraController` pan functions to allow client to specify whether or not to perform smooth pan over poles

## 1.6.0

* Introduce new `ICertificateStore` and `ICredentialsStore` as API replacement for legacy certificate and authentication databases
* Replace legacy certificate and authentication database implementation

## 1.5.0

* Add new `Strings.isBlank(String)` to remove _implementation_ dependency `apache-commons-slim` from `:shared` 

## 1.4.2

* Apply some threshold to mitigate globe rotating on pan when zoomed fully out

## 1.4.1

* Pass through allocator instance for PFI blocks

## 1.4.0

* Enable JNI dependencies for desktop 
  * Attach desktop engine runtime JAR to test dependencies/classpath
  * Utilize separate source set root, `jniTest`, for tests with JNI dependencies to better support Android <> Desktop crossplatform development

## 1.3.0

* Android `AtakCertificateDatabaseAdapter` derives from common `AtakCertificateDatabaseAdapterBase`

## 1.2.0

* add in documentation to the AtakAuthenticationDatabaseIFace 
* add in `AtakAuthenticationDatabaseIFace.PERPETUAL` constant -1 for (which mirrors the desired behavior and underlying current impl
* `AtakCertificateDatabaseIFace` and `AtakAuthenticationDatabaseIFace` extend from `Disposable` instead of providing the own definition, helps with both intended behavior as well as proguard issues (where two interfaces define dispose and then obfuscation ends up not correctly obfuscating the method the same way (cant have two names at once)


## 1.1.1

* correct orientation when panning across pole to avoid spinning globe

## 1.1.0

* expose `apache-commons-lang-slim` as API dependency on AAR

## 1.0.4

* Assume user has canceled if CLI `HttpConnectionHandler` does not receive credentials

## 1.0.3

* Add config option `"overlays.default-label-render-resolution"` to control default label render resolution

## 1.0.2
* Update TTP dependency to 2.7.2 to pick up OpenSSL update to address CVE-2022-0778

## 1.0.1

* `GLBatchGeometryFeatureDataStoreRenderer3` uses default icon dimension constraint of `64`
* `GLBatchGeometryFeatureDataStoreRenderer3` pulls icon dimension constraint from _config options_

## 1.0.0

* Upgrade Android to NDK23
* Utilize NDK supplied CMake toolchain file
* Utilize `javac -h` for JNI header generation
* Local compilation compatibility through JDK17
* Remove sources for thirdparty dependencies; replace with managed dependencies or remove without replacement
* Remove deprecated code marked for removal 4.5 or earlier

## 0.63.0

* Added new FeatureDataStore wrappers
  * FeatureDataStoreProxy
  * FeatureDataStoreLruCacheLogic
* OGRFeatureDataStore uses these new FDS wrappers instead of `RuntimeCachingFeatureDataStore`
* Added `atakmap::feature::Envelope::operator==`
* Added `TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::operator==`
* Added `TAK::Engine::Feature::FeatureDataStore2::FeatureSetQueryParameters::operator==`
* Added `TAK::Engine::Feature::FeatureDataStore2` copy constructor
* Fixed issues in `RuntimeFeatureDataStore2`
  * Cursor returned by `RuntimeFeatureDataStore2::queryFeatures` now behaves like other cursors.  Call `moveToNext` before attempting to `get` the first item.
  * Removed `bulkModify` tracking from `RuntimeFeatureDataStore2`.  It's handled by `AbstractFeatureDataStore2`.
  * `RuntimeFeatureDataStore2::insertFeatureImpl` will call `setContentChanged` before returning when `inserted` is true
  * `RuntimeFeatureDataStore2::deleteAllFeaturesImpl` will call `dispatchDataStoreContentChangedNoSync`
* Added `TE_CHECKLOGCONTINUE_CODE` to use in loops when a message should be logged and then continue rather than break

## 0.62.0

* Add _xray color_ property to C++ `SceneInfo`
* C++ scene renderer uses `SceneInfo::xrayColor`

## 0.61.0

* Reimplement `GLLabelManager` text rendering via SDF

## 0.60.0

* Add LOB intersect to Java `GeoCalculations`

## 0.59.3

* Fix submenu inner/outer radius highlight having extra width

## 0.59.2

* Added additional file validation checks for Cesium JSON files

## 0.59.1

* Fix regression with `GLGlobe::lookAt` honoring minimum zoom introduced in `0.42.0`

## 0.59.0

* bump to `jogl@2.2.4` for closer API compatibility with TAKX

## 0.58.5

* Fix logic error causing failure of `Interop.getObject`

## 0.58.4

* Fix `jcameracontroller.cpp` signature for `tiltTo` causing JNI method resolution failure

## 0.58.3

* Address CVEs
  * CVE-2020-15522
  * CVE-2020-28052
  * CVE-2020-13956

## 0.58.2

* Preload mosaic root nodes on background thread

## 0.58.1

* Make Java `NodeContextResources.discardBuffers` instance based to avoid single render thread/context constraint

## 0.58.0

* Add default radial menu icon resources

## 0.57.2

* Make `GroupContactBase` public to resolve JDK limitations on "split packages" across modules.
  This is NOT intended to make the API published, but is required by the JDK.

## 0.57.1

* Deprecate `GroupContact`, `GroupContactBuilder` and `GroupContactBuilderBase`
* Create `GroupContactBuilderBase2`, replacing `GroupContactBuilderBase`
* Fix persistence errors in `AttributeSet` by using JPA annotations vs Hibernate `Lifecycle` methods
* Delete classes from `experimental.chat`, since they were moved out to TAKX 4 months ago.

## 0.56.1

* Support ModelInfo from zip comment for KMZ files containing only one model.

## 0.56.0

* Port updated submenu support for radial

## 0.55.4

* Re-order static initialization for `AtakAuthenticationDatabase`, `AtakCertificateDatabase` and `CertificateManager`

## 0.55.3

* Cleanup debugging for `Unsafe.allocateDirect` API discovery

## 0.55.2

* Handle potential `NullPointerException` in `Unsafe` due to `Class.getClassLoader()` may return `null`

## 0.55.1

* Crash dumps on Windows will attempt to capture full memory.

## 0.55.0

* Add `IPersistable` interface
* Add persistence support for JRE flavor of `GroupContact`
* `IGroupContact` adds _default_ `setParentContact`


## 0.54.5

* `DtedElevationSource` applies _flags_ filter appropriately on query params

## 0.54.4

* fix C++ surface mesh/model depth buffer based hit test

## 0.54.3

* MOBAC sources now correctly support `minZoom` greater than zero

## 0.54.2

* fix C++ `FDB` encoding for overloaded schemas

## 0.54.1

* fix issue with HTTPS redirects causing WMS/WMTS GetCapabilities to timeout for C++/CLI client

## 0.54.0

* port most recent changes to radial menu

## 0.53.3

* Add nullptr check to Tesselate_polygon to fix failing unit test

## 0.53.2

* Apply perspective divide in `GLLinesEmulation`

## 0.53.1

* Disable polygon tessellation threshold based on limited utility versus potential for resource exhaustion.

## 0.53.0

* Implement new C++ feature renderer
* C++/CLI `GLBatchGeometryFeatureDataStoreRenderer` allows for select between old and new C++ impls
* C++ `Tessellate` adds support for callback based processing to eliminate the need for pre-allocated buffers
* Fix bug in C++ `IconPointStyleConstructor` overload for correctly setting absolute vs relative rotation
* C++ `LineString` and `Polygon` allow for direct access to data buffers
* Micro-optimize `TAK::Engine::Port::String` to avoid heap allocations for small strings


## 0.52.2

* Dispatch focus changed on surface resize as focus point is now managed as offset by the renderer, not by the controller/globe

## 0.52.1

* `GLText2` performs non-printable character check AFTER decode

## 0.52.0

* Refactor `TextFormatParams::fontName` `const char *` -> `TAK::Engine::Port::String` to retain ownership over memory

## 0.51.14

* Avoid potential empty viewport specification for surface rendering

## 0.51.13

* Improve C++ C3DT culling for perspective camera

## 0.51.12

* Update batch point rendering & lollipop behavior to render at terrain instead of altitude 0

## 0.51.11

* Mitigate potential crash when path exceeds max path length

## 0.51.10

* keep enums and the inner interface for RenderSurface (exclude them from obfuscation)

## 0.51.9

* Simplify client initialization of engine for Desktop Java deployment

## 0.51.8

* Support SceneInfo from zip comment for KMZ files containing only one model.

## 0.51.7

* Ensure context is attached in `GLScene` hit-test

## 0.51.6

* In SceneLayer::update, failing to write the zip file comment will not prevent the call to dispatchContentChangedNoSync.
* Revert change from 0.50.0 where OGRFeatureDataStore no longer used RuntimeCachingFeatureDataStore but use a maxRenderQueryLimit of 10K.

## 0.51.5

* Consolidate CMake source file definitions

## 0.51.4

* Initialize texture handle to `GL_NONE` in the event that `glGenTextures` fails


## 0.51.3

* Skip attribute assignment if not defined for shader

## 0.51.2

* Pass correct name to `glInvalidateFramebuffer`

## 0.51.1

* Update model's zip comment before calling dispatchContentChangedNoSync.

## 0.51.0

* Java binding for C++ `DtedElevationSource`

## 0.50.2

* Gracefully handle situation where C++ `std::wstring` to CLI `System::String` fails due to an unsupported character.

## 0.50.1

* `GLBaseMap` tile size dropped to 64 pixels. Resolves issue with texture coordinate precision on MPU5 integrated display unit. This should also mitigate fill rate issues on some devices.
* Make atmosphere enabled state configurable via `ConfigOptions`

## 0.50.0

* Fix `GLBatchGeometryFeatureDataStoreRenderer2::checkSpatialFilter` logic for more than one include/exclude filter.
* Overloaded the `RuntimeCachingFeatureDataStore` constructor to allow clients to specify the maxRenderQueryLimit.
* Changed `ZipFile::setGlobalComment` to return TE_IO if `zipOpen` fails to open the zip file.

## 0.49.1

* Fix copy-paste bug in C++ `AtakMapView::getBounds`

## 0.49.0

* Resolve precision issues with ECEF globe by ditching ECEF emulation in favor of packing ECEF verts into terrain tile using a reserved attribute

## 0.48.0

* Upgrade to TTP-Dist 2.6.1
* Minor modifications to support Linux compilation with GCC 4.8.x

## 0.47.0

* prepare for the removal of direct access to the adapter for both AtakAuthenticationDatabase and AtakCredentialDatabase

## 0.46.0

* Add CLI `MobacMapSource2` to expose tile update interval
* `MobacMapSourceFactory` parses out `<tileUpdate>` tag as refresh interval in milliseconds
* implement automatic refresh monitor

## 0.45.2

* Add support for getting the raw url from `MobacMapSource`

## 0.45.1

* Fix off-by-one issue in C++ `MultiLayer2` that would result in failure to move a child layer to the last position.

## 0.45.0

* Add flag for CLI `GLAsynchronousMapRenderable` to allow for better consistency with legacy globe for image overlay selection

## 0.44.3

* Add Android 12 compatible implementation for the Unsafe allocator

## 0.44.2

* Pass through layer transparency setting to renderer for mobile imagery

## 0.44.1

* Use a recursive mutex to guard `AtakMapView` layers/callbacks to allow re-entry from callback

## 0.44.0

* legacy `GLGdalQuadTileNode` now works with globe surface renderer
* make CLI `GLTiledMapLayer` part of public API
* implement legacy adapter for tile reader/spi

## 0.43.0

* expose GLGlobeBase::release() via C++/CLI bindings
* C++ GLMapView2 orderly destruct offscreen FBO in release() or leak from destructor if not on render thread
* implement sharing of OSMDroidContainer instances to mitigate issues with concurrent writes
* Propagate GLGlobeBase::animationTick to GLGlobeBase::State during render pump
* Better define owner for C++/CLI ITileReader in GLTiledMapLayer to allow for positive release
* Resolve issues with out-of-order destruct

## 0.42.3

* Fix int precision issue in `IO_truncate` causing a failure during Cesium conversion

## 0.42.2

* C++/CLI `GLAbstractDataStoreRasterLayer` honors subject visibility setting

## 0.42.1

* When determining number of resolution levels, continue to bisect until both dimensions are reduced to single tile

## 0.42.0

* Refactor `CameraController` to C++
* C++ `AtakMapView` completely defer state to `MapRenderer2`
* `MapSceneModel2` assignment copies `displayDpi`
* Java `RenderSurface` interop
* Java `GLGlobeBase` interop does static init if necessary
* Collision handling becomes implementation detail of `GLGlobeBase` derivative
* Access `SurfaceRendererControl` via `GLGlobeBase::getControl(...)` rather than specialization

## 0.41.0

* Massage location of embedded font files
* Add new resource file describing embedded font files

## 0.39.1

* Give C++ `HttpProtocolHandler` a default timeout of 10s 

## 0.39.0

* Expose font files as resources via `TAK.Engine` assembly

## 0.38.1

* Avoid potential _null_ dereference in CLI `WFSQuery`

## 0.38.0

* Add support for rendering line segments in `GLBatchLineString`

## 0.37.1

* CLI `GeoMag` static constructor passing through wrong address to magnetic model struct

## 0.37.0

* Introduce spatial filters control for features renderer

## 0.36.0

* Minor modifications to support Linux compilation with GCC 4.8.x

## 0.35.0

* Add pre-generated SDF font representations

## 0.34.1

* Call SceneObjectControl's setLocation in onClampToGroundOffsetComputed.

## 0.34.0

* Marshal Java ModelInfo and native SceneInfo.
* Pix4dGeoreferencer.locate will return ModelInfo from ZipCommentGeoreferencer.locate if available.

## 0.33.1

* Fix typo when computing default stroke style in `GLBatchPolygon`

## 0.33.0

* Optimize LAS -> C3DT PNTS conversion
* Support progress callback during LAS -> C3DT PNTS conversion
* `SceneInfo` supports mask for various supported "capabilities"
* Add per-_scene object_ controls

## 0.32.0

* Add `getContact(String)` method to IContactStore, deprecate `containsContact(Contact)`

## 0.31.1

* Prevent a Playstore Crash: NullPointerException GdalLayerInfo

## 0.31.0

* Refactor shader source out of C++ files

## 0.30.0

* Adds CertificateManager.createSSLContext() to do SSLContext creation and initialization in a uniform way across TAK

## 0.29.0

+ Provide a mechanism to construct a safe PullParser that defends against bombs such as a billion laughs

## 0.28.2

* C++/CLI `Tessellate` gracefully handles zero-length array

## 0.28.1

* Proguard friendly fix for addressing reflection regression in `Unsafe.allocateDirect` with Android 29. 

## 0.28.0

* Add `isAcceptableInFilename(character)` method to FileSystemUtils

## 0.27.0

* Add `withParentContact(IGroupContact)` method to GroupContactBuilder to support `IGroupContact::getParentContact()`

## 0.26.2

* `ElMgrTerrainRenderService` unwinds all `ElevationSource::OnContentChangedListener`'s on destruct

## 0.26.1

* Fix potential crash in OGR parsing involving empty geometries. See ENGINE-459

## 0.26.0

* Introduce `OGR_Content2`, implementing `FeatureDataSource2::Content` directly
* `OGR_Content` defers implementation to `OGR_Content2`
* Update bindings/registration to prefer new class

## 0.25.4

* Add `libLAS` nuget as transitive dependency

## 0.25.3

* Ensure proper size of glyph bitmap returned by `ManagedTextFormat2`

## 0.25.2

* Enable _medium_ terrain tile shader for Android x86 and arm32
* Don't discard VBO/IBO on GLTerrainTile in _visible_ list; _visible_ list may remain unchanged but differ from front/back in the event visible tiles cannot be computed

## 0.25.1

* Add `null` check in `GLBatchPolygon.extrudeGeometry` as unsubstantiated fix for issue that could not be reproduced.

## 0.25.0

* Add `IGroupContact.getParentContact()` (as default)

## 0.24.1

* Prevent error when importing non-archive models.

## 0.24.0

* Add SIMD wrapper for NEON
* Implement SIMD raycast intersect computation
* Apply SIMD raycast for terrain raycasts

## 0.23.2

* Fix envelope calculation in SceneLayer.

## 0.23.1

* Handle potential NPE in `GLQuadTileNode.resolveTexture` for reconstruct from children

## 0.23.0

* Allow association of various cached GL state with `RenderContext` instance (e.g. `GLText`)
* `JOGLGLES` allows re-init, supporting both multi-thread and same thread changes of `GLAutoDrawable` instance
* `JOGLRenderContext` installs `GLEventListener` on `GLAutoDrawable` to re-init `JOGLGLES` at the start of every render pump
* `Interop` supports intern/find mechanism
* deprecate duplicate `Interop`

## 0.22.0

* Add initial support for LAS LiDAR file ingest and render

## 0.21.2

* Fix tile index computation for equirectangular projection in `OSMDroidSQLiteMosaicDatabase`

## 0.21.1

* massage GLSL version 100 to version 120
* bump to `android-port@2.0.1`
* bump to `gles-jogl-c@1.0.3`

## 0.21.0

* Define experimental Chat Service API

## 0.20.5

* `ttp-dist@2.5.1`; stripped `libassimp` and `libjassimp`

## 0.20.4

* KML style parsing for OGR specific strings handles multiple attributes

## 0.20.3

* `Shader` declares version for portability on Mac
* `EngineLibrary.init` assigns `appContext` to eliminate warning in `GLBitmapLoader`
* `controlled` bumps to conan `ttp-dist/2.5.0`
* return value from `CameraChangedForwarder::onCameraChanged` to prevent crash on linux
* normalize latitude/longitude in geoid height retrieval to quiet warnings

## 0.20.2

* Fix ZipComment unit tests.

## 0.20.1

* `GLBitmapLoader.mountArchive` accounts for URL path escaped characters

## 0.20.0

* Introduce the IlluminationControl2 and deprecate IlluminationControl

## 0.19.0

* Add ZipCommentGeoreferencer and ZipCommentInfo for model elevation and positioning.

## 0.18.3

* Attempt to mitigate observed inconsistent loading of `libjawt` on linux hosts.

## 0.18.2

* Update ContactStore to use identity-based collections instead of equality-based collections.
* Remove unneeded overrides from AttributeSet in shared/src/jre
* Reintroduce AssertJ usage in AttributeSetTest

## 0.18.1

* Remove hard-coded density multiplier for windows

## 0.18.0

* Define `IlluminationControl`
* Implement directional light source support in terrain tile renderer, based on topocentric coordinates

## 0.17.0

* Add Contacts API in support of a common contact management system

## 0.16.0

* Adds C++ `MapRenderer2` to reflect Java `MapRenderer3` interface, specifically targeting _camera-changed_ event for inclusion
* `AtakMapView` proxies `onCameraChanged` to `onMapMoved` to capture animated map motion, specifically for startup
* `ElMgrTerrainRenderService` only derives tile if source version is strictly less than derived version.

## 0.15.5

* Handle potential access violation by checking for empty DRW geometries on parse

## 0.15.4

* Invalidate hit-testing vertices when points are updated in `GLBatchLineString`

## 0.15.3

* Update WMM (World Magnetic Model) Coefficient file; 2020-2025.

## 0.15.2

* Change out the expired LetsEncrypt DSTRootCAX3.crt (9/30/21)

## 0.15.1

* Call to getRenderer3 after dispose causes a NullPointerException but should return null.

## 0.15.0

* Fix transitive dependency references in publication
* Add additional dependencies to simplify for clients
  * `takkernel-all` references all _non controlled_ JARs (excludes native runtimes)
  * `takkernel-engine-main` references `takkernel-engine` (classfiles) and all native runtimes
  * `takkernel-rt-<platform>` holds platform specific runtime libraries (drop classifier usage)

## 0.14.0

* Add `IScaleWidget2`; defer scale computation wholly to renderer.

## 0.13.0

* Initial _look-from_ implementation.

## 0.12.0

* Export transitive dependencies for `takkernel-engine` on publish 
* Export transitive dependencies for `takkernel-aar` on publish

## 0.11.1

* Replace calls to deprecated `DistanceCalculations` methods with `GeoCalculations`
* Mark additional methods in `DistanceCalculations` as deprecated

## 0.11.0

* Desktop external native build accepts `cmake.config` property to allow CMake configuration type pass through, enabling `Release` builds for windows (default linkage for Windows is `Debug`)

## 0.10.0

* Update TTP runtime to `2.2.0`
  * Exposes `minizip` API

## 0.9.1

* Update property for `TAK.Kernel.Controlled` nuget assembly install directory to avoid conflict with `TAK.Kernel` nuget

## 0.9.0

* Add `DontObfuscate` annotation
* Annotate classes that should be skipped for obfuscation

## 0.8.0

* `TAK.Kernel` nuget package adds dependencies on `ANGLE` and `TTP-Dist`

## 0.7.0

* Fix request for `uPointSize` _uniform_ handle
* Deprecate `Shader.getAPointSize()`; add `Shader.getUPointSize()`
* `GLES20FixedPipeline` uses color value of `0xFFFFFFFF` for point sprite rendering backwards compatibility

## 0.6.1

* Implement marshal for _portable_ `MotionEvent ` to _platform_ `MotionEvent`

## 0.6.0

* add in documentation for FileSystemUtils
* expand isEmpty to allow for Collections instead of just lists

## 0.5.1

* Handle JOGL instantiations that are `GL4bc` without client-array support

## 0.5.0

* Fix inconsitency in featureset _read-only_ state after insert for `TAK::Engine::Feature::FDB`
* Add overload to `TAK::Engine::Feature::FDB::insertFeatureSet(...)` to allow client to specify initial _read-only_ state 
* Fix package name for `TAK.Kernel.Controlled` nuget publication

## 0.4.3

* Fix memory leak in com.atakmap.map.layer.feature.geometry.Geometry



## 0.4.2

* Address potential `ConcurrentModificationException` with hit-testing machinery

## 0.4.1

* Implement caching for `GLText2_intern(const TextFormatParams &)` to address potential memory leak if client not actively managing instances

## 0.4.0

* Add _hover_ callback to `IMapWidget` (e.g. mouse enter, mouse move, mouse exit)

## 0.3.0

* Add `:takkernel:shared`
* Port widgets framework to `:takkernel:shared`

## 0.2.0

* Enable _warning-as-error_ for Windows `:takkernel:engine` native build
* Handle empty meshes
* Add `GeoBounds` builder utility
* Allow additional level of recursion for `tileset.json` for zipped 3D tiles datasets
* Fix logic for `RuntimeFeatureDataStore` feature name queries with wildcard character not first/last character

## 0.1.1

* Remove Win32 targets for `TAK.Engine` and `TAK.TCM`
* Update include/linker path references for `TAK.Engine` and `TAK.TCM`
* Update source code paths in `TAK.Engine` and `TAK.TCM`
* Add powershell scripts to generate `TAK.Kernel` and `TAK.Kernel.Controlled` Nuget packages

## 0.1.0

* Import TAK Globe sources

## 0.0.1

* Change JOGL dependency version `2.3.2` -> `2.1.5-01`
* Change Guiave version `27.0.1-jre` -> `30.0-jre`

## 0.0.0

* Initial Revision.
