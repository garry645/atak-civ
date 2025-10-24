#include "pch.h"

#include "feature/KMLParser.h"
#include "util/IO2.h"
#include "feature/ShapefileDriverDefinition2.h"
#include "feature/ShapefileDriverDefinition.h"

using namespace TAK::Engine::Util;
using namespace TAK::Engine::Feature;

namespace takenginetests {
    using TAK::Engine::Port::String;


    class ShapefileDriverDefinition2Test : public TAK::Engine::Feature::ShapefileDriverDefinition2
    {
    public:
        ShapefileDriverDefinition2Test() {}
        virtual ~ShapefileDriverDefinition2Test() override {}
        const char* getDefaultLineStringStylePublic()
        {
            return getDefaultLineStringStyle();
        }
        const char* getDefaultPointStylePublic()
        {
            return getDefaultPointStyle();
        }
        const char* getDefaultPolygonStylePublic()
        {
            return getDefaultPolygonStyle();
        }

    };
    TEST(ShapefileDriverDefinition2Tests, testDefault) {

        ShapefileDriverDefinition2Test def2;

        ASSERT_STREQ(def2.getDefaultLineStringStylePublic(), "PEN(c:#FFFFFFFF,w:2px)");
        ASSERT_STREQ(def2.getDefaultPointStylePublic(), "SYMBOL(c:#FFFFFFFF,s:64px)");
        ASSERT_STREQ(def2.getDefaultPolygonStylePublic(), "PEN(c:#FFFFFFFF,w:2px)");
    }

}