#ifndef ATAKMAP_FEATURE_KMLDRIVERDEFINITION2_H_INCLUDED
#define ATAKMAP_FEATURE_KMLDRIVERDEFINITION2_H_INCLUDED

#include "feature/DefaultDriverDefinition2.h"
#include "port/Platform.h"
#include <libxml/xmlreader.h>
#include <math.h>

namespace TAK {
    namespace Engine {
        namespace Feature {
            class ENGINE_API KMLDriverDefinition2 : public DefaultDriverDefinition2
            {
            private :
                typedef std::map<std::string, std::string> StyleMap;
                typedef std::map<std::string, std::map<std::string, std::string>> PairsMap;
            public :
                class ENGINE_API Spi;
            public:
                KMLDriverDefinition2(const char *path) NOTHROWS;
            public :
                Util::TAKErr getStyle(Port::String &value, const OGRFeature&, const OGRGeometry&) NOTHROWS override;
                /** @deprecated since = 5.5, forRemoval = true, removeAt = 5.8*/
				virtual Util::TAKErr getStyle(Port::String &value) NOTHROWS;
                bool layerNameIsPath() const NOTHROWS override;
            private :
                Util::TAKErr getStyleImpl(Port::String &value, const bool isPoint) NOTHROWS;
            private:
                xmlTextReaderPtr getXmlReader(TAK::Engine::Util::array_ptr<char>&, const std::size_t len) const NOTHROWS;
                Util::TAKErr createDefaultLineStringStyle(Port::String &value) const NOTHROWS override;
                Util::TAKErr createDefaultPointStyle(Port::String &value) const NOTHROWS override;
                Util::TAKErr createDefaultPolygonStyle(Port::String &value) const NOTHROWS override;
                Util::TAKErr parseRegion(xmlTextReaderPtr parser) NOTHROWS;
                Util::TAKErr parseRegion() NOTHROWS;
            private :
                Port::String filePath;
                bool styleParsed;

                struct Region
                {
                    double north = NAN, south = NAN, east = NAN, west = NAN, minAltitude = NAN, maxAltitude = NAN;
                    double minLodPixels = NAN, maxLodPixels = NAN, minFadeExtent = NAN, maxFadeExtent = NAN;
                    std::size_t minLodTile, maxLodTile;

                    bool regionParsed = false;
                } region;
                PairsMap styleMaps;
                StyleMap styles;
            };

            class ENGINE_API KMLDriverDefinition2::Spi : public OGRDriverDefinition2Spi
            {
            public :
                virtual Util::TAKErr create(OGRDriverDefinition2Ptr &value, const char *path) NOTHROWS;
                virtual const char *getType() const NOTHROWS;
            };
        }
    }
}

#endif  // #ifndef ATAKMAP_FEATURE_KMLDRIVERDEFINITION2_H_INCLUDED
