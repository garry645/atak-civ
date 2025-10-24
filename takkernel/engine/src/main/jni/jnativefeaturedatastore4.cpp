#include "com_atakmap_map_layer_feature_NativeFeatureDataStore4.h"

#include <list>

#include <feature/Feature2.h>
#include <interop/feature/Interop.h>
#include <feature/FeatureCursor2.h>
#include <feature/FeatureDataStore3.h>
#include <feature/FeatureDefinition2.h>
#include <feature/FeatureSetCursor2.h>
#include <feature/Geometry.h>
#include <feature/Geometry2.h>
#include <feature/GeometryFactory.h>
#include <feature/LegacyAdapters.h>
#include <feature/ParseGeometry.h>
#include <feature/Style.h>
#include <util/AttributeSet.h>
#include <util/Memory.h>

#include "common.h"
#include "interop/JNIByteArray.h"
#include "interop/JNIDoubleArray.h"
#include "interop/JNIIntArray.h"
#include "interop/JNILongArray.h"
#include "interop/JNIStringUTF.h"
#include "interop/JNINotifyCallback.h"
#include "interop/Pointer.h"
#include "interop/feature/ManagedFeatureDataStore2.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

using namespace atakmap::feature;
using namespace atakmap::util;

using namespace TAKEngineJNI::Interop;

namespace
{
    TAKErr AttributeSet_merge(AttributeSet &target, const AttributeSet &update)
    {
        try {
            std::vector<const char *> keys = update.getAttributeNames();
            for(std::size_t i = 0u; i < keys.size(); i++) {
                const char *key = keys[i];
                switch(update.getAttributeType(key)) {
                    case AttributeSet::INT :
                        target.setInt(key, update.getInt(key));
                        break;
                    case AttributeSet::LONG :
                        target.setLong(key, update.getLong(key));
                        break;
                    case AttributeSet::DOUBLE :
                        target.setDouble(key, update.getDouble(key));
                        break;
                    case AttributeSet::STRING :
                        target.setString(key, update.getString(key));
                        break;
                    case AttributeSet::BLOB :
                        target.setBlob(key, update.getBlob(key));
                        break;
                    case AttributeSet::INT_ARRAY :
                        target.setIntArray(key, update.getIntArray(key));
                        break;
                    case AttributeSet::LONG_ARRAY :
                        target.setLongArray(key, update.getLongArray(key));
                        break;
                    case AttributeSet::DOUBLE_ARRAY :
                        target.setDoubleArray(key, update.getDoubleArray(key));
                        break;
                    case AttributeSet::STRING_ARRAY :
                        target.setStringArray(key, update.getStringArray(key));
                        break;
                    case AttributeSet::BLOB_ARRAY :
                        target.setBlobArray(key, update.getBlobArray(key));
                        break;
                    case AttributeSet::ATTRIBUTE_SET :
                    {
                        AttributeSet nested(target.getAttributeSet(key));
                        TAKErr code = AttributeSet_merge(nested, update.getAttributeSet(key));
                        TE_CHECKRETURN_CODE(code);
                        target.setAttributeSet(key, nested);
                    }
                        break;
                    default :
                        return TE_InvalidArg;
                }
            }
            return TE_Ok;
        } catch(...) {
            return TE_Err;
        }
    }

    TAKErr FeatureDataStore3_updateFeature(FeatureDataStore2 &dataStore, const int64_t fid, const int fields, const char *name, const Geometry *geom, const Style *style, const AttributeSet *attrs, const int attrUpdateType, Traits traits)
    {
        switch(fields) {
            case 0 :
                return TE_Ok;
            case FeatureDataStore2::FeatureQueryParameters::GeometryField :
                if(!geom)
                    return TE_InvalidArg;
                return dataStore.updateFeature(fid, *geom);
            case FeatureDataStore2::FeatureQueryParameters::StyleField :
                return dataStore.updateFeature(fid, style);
            case FeatureDataStore2::FeatureQueryParameters::AttributesField :
            {
                std::unique_ptr<AttributeSet> attrsCleaner;
                if(!attrs) {
                    // add/replace with empty is identity
                    if(attrUpdateType == FeatureDataStore2::UPDATE_ATTRIBUTESET_ADD_OR_REPLACE)
                        return TE_Ok;
                    attrsCleaner.reset(new AttributeSet());
                    attrs = attrsCleaner.get();
                }
                if(attrUpdateType == FeatureDataStore2::UPDATE_ATTRIBUTESET_SET) {
                    return dataStore.updateFeature(fid, *attrs);
                } else if(attrUpdateType == FeatureDataStore2::UPDATE_ATTRIBUTESET_ADD_OR_REPLACE) {
                    // get source attributes
                    FeatureDataStore2::FeatureQueryParameters params;
                    TAKErr code = params.featureIds->add(fid);
                    TE_CHECKRETURN_CODE(code);
                    params.ignoredFields = FeatureDataStore2::FeatureQueryParameters::NameField|FeatureDataStore2::FeatureQueryParameters::GeometryField|FeatureDataStore2::FeatureQueryParameters::StyleField;
                    params.limit = 1;

                    FeatureCursorPtr result(NULL, NULL);
                    code = dataStore.queryFeatures(result, params);
                    TE_CHECKRETURN_CODE(code);

                    code = result->moveToNext();
                    if(code == TE_Done)
                        return TE_InvalidArg;
                    TE_CHECKRETURN_CODE(code);

                    //  merge attributes
                    const AttributeSet *src;
                    code = result->getAttributes(&src);
                    TE_CHECKRETURN_CODE(code);

                    // add or replace with empty source data
                    if(!src)
                        return dataStore.updateFeature(fid, *attrs);

                    AttributeSet mergeResult(*src);
                    code = AttributeSet_merge(mergeResult, *attrs);
                    TE_CHECKRETURN_CODE(code);

                    return dataStore.updateFeature(fid, mergeResult);
                } else {
                    return TE_InvalidArg;
                }
            }
            case FeatureDataStore2::FeatureQueryParameters::NameField :
                return dataStore.updateFeature(fid, name);
            case FeatureDataStore2::FeatureQueryParameters::GeometryField|FeatureDataStore2::FeatureQueryParameters::StyleField|FeatureDataStore2::FeatureQueryParameters::AttributesField|FeatureDataStore2::FeatureQueryParameters::NameField :
            {
                if(!geom)
                    return TE_InvalidArg;

                std::unique_ptr<AttributeSet> attrsCleaner;
                if(!attrs) {
                    attrsCleaner.reset(new AttributeSet());
                    attrs = attrsCleaner.get();
                }
                return dataStore.updateFeature(fid, name, *geom, style, *attrs);
            }
            default :
            {
                // make sure there are no unrecognized flags
                if((~(FeatureDataStore2::FeatureQueryParameters::GeometryField|FeatureDataStore2::FeatureQueryParameters::StyleField|FeatureDataStore2::FeatureQueryParameters::AttributesField|FeatureDataStore2::FeatureQueryParameters::NameField))&fields)
                    return TE_InvalidArg;

                // XXX - not efficient -- mask off each field
                TAKErr code(TE_Ok);
                code = FeatureDataStore3_updateFeature(dataStore, fid, fields&FeatureDataStore2::FeatureQueryParameters::GeometryField, name, geom, style, attrs, attrUpdateType, traits);
                TE_CHECKRETURN_CODE(code);
                code = FeatureDataStore3_updateFeature(dataStore, fid, fields&FeatureDataStore2::FeatureQueryParameters::StyleField, name, geom, style, attrs, attrUpdateType, traits);
                TE_CHECKRETURN_CODE(code);
                code = FeatureDataStore3_updateFeature(dataStore, fid, fields&FeatureDataStore2::FeatureQueryParameters::AttributesField, name, geom, style, attrs, attrUpdateType, traits);
                TE_CHECKRETURN_CODE(code);
                code = FeatureDataStore3_updateFeature(dataStore, fid, fields&FeatureDataStore2::FeatureQueryParameters::NameField, name, geom, style, attrs, attrUpdateType, traits);
                TE_CHECKRETURN_CODE(code);

                return code;
            }
        }
    }
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDataStore4_updateFeature
  (JNIEnv *env, jclass clazz, jlong ptr, jlong fid, jint fields, jstring jname, jlong geomPtr, jlong stylePtr, jlong attrsPtr, jint attrUpdateType, jobject mtraits)
{
    FeatureDataStore3 *dataStore = JLONG_TO_INTPTR(FeatureDataStore3, ptr);
    if(!dataStore) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
    }

    TAKErr code(TE_Ok);

    JNIStringUTF name(*env, jname);

    // REMEMBER: Java always passes in Geometry2
    GeometryPtr_const cgeom(NULL, NULL);
    if(geomPtr) {
        Geometry2 *cgeom2 = JLONG_TO_INTPTR(Geometry2, geomPtr);
        code = LegacyAdapters_adapt(cgeom, *cgeom2);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return;
    }

    Traits ctraits;
    code = TAKEngineJNI::Interop::Feature::Interop_marshal(ctraits, *env, mtraits);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    code = FeatureDataStore3_updateFeature(*dataStore, fid, fields, name, cgeom.get(), JLONG_TO_INTPTR(Style, stylePtr), JLONG_TO_INTPTR(AttributeSet, attrsPtr), attrUpdateType, ctraits);
    ATAKMapEngineJNI_checkOrThrow(env, code);
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDataStore4_wrap
        (JNIEnv *env, jclass clazz, jobject mdatastore)
{
    FeatureDataStore3Ptr cdatastore(new TAKEngineJNI::Interop::Feature::ManagedFeatureDataStore2(*env, mdatastore), Memory_deleter_const<FeatureDataStore3, TAKEngineJNI::Interop::Feature::ManagedFeatureDataStore2>);
    return NewPointer(env, std::move(cdatastore));
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDataStore4_destruct
        (JNIEnv *env, jclass clazz, jobject jpointer)
{
    if(!jpointer) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    Pointer_destruct_iface<FeatureDataStore3>(env, jpointer);
}

