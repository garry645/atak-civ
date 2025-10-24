#include "interop/feature/Interop.h"

#include <feature/LegacyAdapters.h>
#include <util/AttributeSet.h>
#include <util/Memory.h>

#include "com_atakmap_map_layer_feature_FeatureDataStore2.h"

#include "common.h"
#include "interop/InterfaceMarshalContext.h"
#include "interop/Pointer.h"
#include "interop/core/Interop.h"
#include "interop/feature/ManagedFeatureDataStore2.h"
#include "interop/java/JNICollection.h"
#include "interop/java/JNIEnum.h"
#include "interop/java/JNIIterator.h"
#include "interop/java/JNIPrimitive.h"

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

using namespace atakmap::feature;
using namespace atakmap::util;

using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Java;

namespace
{
    template<class T>
    class InteropImpl
    {
    public :
        InteropImpl(JNIEnv &env, const char *classname, TAKErr (*cloneFn_)(std::unique_ptr<T, void(*)(const T *)> &, const T &) NOTHROWS) NOTHROWS;
    public :
        TAKErr create(std::unique_ptr<T, void(*)(const T *)> &value, JNIEnv &env, jobject managed) NOTHROWS;
        TAKErr get(std::shared_ptr<T> &value, JNIEnv &env, jobject managed) NOTHROWS;
        TAKErr get(T **value, JNIEnv &env, jobject managed) NOTHROWS;
    private :
        jfieldID pointerFieldId;
        TAKErr (*cloneFn)(std::unique_ptr<T, void(*)(const T *)> &, const T &) NOTHROWS;
    };

    struct
    {
        jclass id;
        jfieldID pointer;
        jmethodID ctor__LPointer;
    } Feature_class;

    struct
    {
        jclass id;
        jfieldID fsid;
        jfieldID provider;
        jfieldID type;
        jfieldID name;
        jfieldID minGsd;
        jfieldID maxGsd;
        jfieldID version;
        jmethodID ctor__JLStringLStringLStringDDJ;
    } FeatureSet_class;

    struct
    {
        jclass id;
        jfieldID featureSetFilter; // FeatureDataStore2$FeatureSetQueryParameters
        jfieldID ids; // Set
        jfieldID names; // Set
        jfieldID geometryTypes; // Set
        jfieldID attributeFilters; // Set
        jfieldID visibleOnly; // Z
        jfieldID spatialFilter; // Geometry
        jfieldID minimumTimestamp; // J
        jfieldID maximumTimestamp; // J
        jfieldID altitudeMode; // Feature$AltitudeMode
        jfieldID extrudedOnly; // Z
        jfieldID ignoredFeatureProperties; // I
        jfieldID spatialOps; // Collection
        jfieldID order; // Collection
        jfieldID limit; // I
        jfieldID offset; // I
        jfieldID timeout; // J
        jmethodID ctor;

        struct
        {
            jclass id;
            struct
            {
                jclass id;
                jmethodID ctor;
            } Name_class;
            struct
            {
                jclass id;
                jmethodID ctor;
            } ID_class;
            struct
            {
                jclass id;
                jfieldID point; // LGeoPoint
                jmethodID ctor__LGeoPoint;
            } Distance_class;
        } Order_class;

        struct
        {
            jclass id;
            struct
            {
                jclass id;
                jfieldID distance; // D
                jmethodID ctor__D;
            } Buffer_class;
            struct
            {
                jclass id;
                jfieldID distance; // D
                jmethodID ctor__D;
            } Simplify_class;
        } SpatialOp_class;
    } FeatureQueryParameters_class;

    struct
    {
        jclass id;
        jfieldID ids;
        jfieldID names;
        jfieldID types;
        jfieldID providers;
        jfieldID minResolution;
        jfieldID maxResolution;
        jfieldID visibleOnly;
        jfieldID limit;
        jfieldID offset;
        jmethodID ctor;
    } FeatureSetQueryParameters_class;

    struct
    {
        jclass id;
    } Point_class;
    struct
    {
        jclass id;
    } LineString_class;
    struct
    {
        jclass id;
    } Polygon_class;
    struct
    {
        jclass id;
    } GeometryCollection_class;

    struct
    {
        jclass id;
        jfieldID minX;
        jfieldID minY;
        jfieldID minZ;
        jfieldID maxX;
        jfieldID maxY;
        jfieldID maxZ;
        jmethodID ctor__DDDDDD;
    } Envelope_class;

    struct
    {
        jobject ClampToGround;
        jobject Relative;
        jobject Absolute;
    } AltitudeMode_enum;

    struct
    {
        jclass id;
        jfieldID labelsEnabled;
        jfieldID filter;
    } GLBatchGeometryFeatureDataStoreRenderer2_Options_class;

    struct
    {
        jobject Rhumb;
        jobject GreatCircle;
    } LineMode_enum;

    struct
    {
        jclass id;
        jmethodID ctor;
        jfieldID altitudeMode;
        jfieldID extrude;
        jfieldID lineMode;
    } Feature_Traits_class;

    InterfaceMarshalContext<FeatureDataStore2> FeatureDataStore2_interop;

    TAKErr geometry_clone(Geometry2Ptr &value, const Geometry2 &geom) NOTHROWS;
    TAKErr style_clone(TAK::Engine::Feature::StylePtr &value, const Style &style) NOTHROWS;
    TAKErr attributeset_clone(AttributeSetPtr &value, const AttributeSet &attrs) NOTHROWS;
    InteropImpl<Geometry2> &geometryInterop(JNIEnv &env) NOTHROWS;
    InteropImpl<Style> &styleInterop(JNIEnv &env) NOTHROWS;
    InteropImpl<AttributeSet> &attributeSetInterop(JNIEnv &env) NOTHROWS;
    TAKErr addAllIfNotNull(TAK::Engine::Port::Collection<TAK::Engine::Port::String> &c, JNIEnv &env, jobject o, jfieldID f) NOTHROWS
    {
        Java::JNILocalRef mf(env, env.GetObjectField(o, f));
        if(!mf)
            return TE_Ok;
        return Java::JNICollection_addAll(&c, env, mf);
    }
    TAKErr addAllIfNotNull(TAK::Engine::Port::Collection<int64_t> &c, JNIEnv &env, jobject o, jfieldID f) NOTHROWS
    {
        Java::JNILocalRef mf(env, env.GetObjectField(o, f));
        if(!mf)
            return TE_Ok;
        return Java::JNICollection_addAll(&c, env, mf);
    }
    TAKErr addAllIfNotEmpty(jobject o, JNIEnv &env, jfieldID f, const TAK::Engine::Port::Collection<TAK::Engine::Port::String> &c) NOTHROWS
    {
        if(const_cast<TAK::Engine::Port::Collection<TAK::Engine::Port::String> &>(c).empty())
            return TE_Ok;
        Java::JNILocalRef mf = Java::JNICollection_create(env, Java::JNICollectionClass::HashSet);
        env.SetObjectField(o, f, mf);
        return Java::JNICollection_addAll(mf, env, c);
    }
    TAKErr addAllIfNotEmpty(jobject o, JNIEnv &env, jfieldID f, const TAK::Engine::Port::Collection<int64_t> &c) NOTHROWS
    {
        if(const_cast<TAK::Engine::Port::Collection<int64_t> &>(c).empty())
            return TE_Ok;
        Java::JNILocalRef mf = Java::JNICollection_create(env, Java::JNICollectionClass::HashSet);
        env.SetObjectField(o, f, mf.get());
        return Java::JNICollection_addAll(mf, env, c);
    }


    bool checkInit(JNIEnv &env) NOTHROWS;
    bool Feature_interop_init(JNIEnv &env) NOTHROWS;
}

#define INTEROP_FN_DEFNS(pclass, interopfn) \
    TAKErr TAKEngineJNI::Interop::Feature::Interop_create(std::unique_ptr<pclass, void(*)(const pclass *)> &value, JNIEnv *env, jobject managed) NOTHROWS \
    { \
        InteropImpl<pclass> &impl = interopfn(*env); \
        return impl.create(value, *env, managed); \
    } \
    TAKErr TAKEngineJNI::Interop::Feature::Interop_create(std::unique_ptr<const pclass, void(*)(const pclass *)> &value, JNIEnv *env, jobject managed) NOTHROWS \
    { \
        TAKErr code(TE_Ok); \
        std::unique_ptr<pclass, void(*)(const pclass *)> native(NULL, NULL); \
        code = Interop_create(native, env, managed); \
        TE_CHECKRETURN_CODE(code); \
        value = std::unique_ptr<const pclass, void(*)(const pclass *)>(native.release(), native.get_deleter()); \
        return code; \
    } \
    TAKErr TAKEngineJNI::Interop::Feature::Interop_get(pclass **value, JNIEnv *env, jobject managed) NOTHROWS \
    { \
        InteropImpl<pclass> &impl = interopfn(*env); \
        return impl.get(value, *env, managed); \
    } \
    TAKErr TAKEngineJNI::Interop::Feature::Interop_get(const pclass **value, JNIEnv *env, jobject managed) NOTHROWS \
    { \
        TAKErr code(TE_Ok); \
        pclass *native; \
        code = Interop_get(&native, env, managed); \
        TE_CHECKRETURN_CODE(code); \
        *value = native; \
        return code; \
    } \
    TAKErr TAKEngineJNI::Interop::Feature::Interop_get(std::shared_ptr<pclass> &value, JNIEnv *env, jobject managed) NOTHROWS \
    { \
        InteropImpl<pclass> &impl = interopfn(*env); \
        return impl.get(value, *env, managed); \
    } \
    TAKErr TAKEngineJNI::Interop::Feature::Interop_get(std::shared_ptr<const pclass> &value, JNIEnv *env, jobject managed) NOTHROWS \
    { \
        TAKErr code(TE_Ok); \
        std::shared_ptr<pclass> svalue; \
        code = Interop_get(svalue, env, managed); \
        TE_CHECKRETURN_CODE(code); \
        value = std::const_pointer_cast<const pclass>(svalue); \
        return code; \
    }

INTEROP_FN_DEFNS(Geometry2, geometryInterop)
INTEROP_FN_DEFNS(Style, styleInterop)
INTEROP_FN_DEFNS(AttributeSet, attributeSetInterop)

#undef INTEROP_FN_DEFNS

jobject TAKEngineJNI::Interop::Feature::Interop_create(JNIEnv *env, const Geometry2 &cgeom) NOTHROWS
{
    Geometry2Ptr retval(NULL, NULL);
    if(Geometry_clone(retval, cgeom) != TE_Ok)
        return NULL;
    Java::JNILocalRef mgeom(*env, nullptr);
    Interop_marshal(mgeom, *env, std::move(retval));
    return mgeom.release();
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mgeom, JNIEnv &env, Geometry2Ptr &&cgeom) NOTHROWS
{
    jclass Interop_class = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/Interop");
    jmethodID Interop_createGeometry = env.GetStaticMethodID(Interop_class, "createGeometry", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)Lcom/atakmap/map/layer/feature/geometry/Geometry;");
    Java::JNILocalRef mgeomPointer(env, NewPointer(&env, std::move(cgeom)));
    mgeom = Java::JNILocalRef(env, env.CallStaticObjectMethod(Interop_class, Interop_createGeometry, mgeomPointer.get(), NULL));
    return TE_Ok;
}
jobject TAKEngineJNI::Interop::Feature::Interop_create(JNIEnv *env, const Style &cstyle) NOTHROWS
{
    Java::JNILocalRef mstyle(*env, nullptr);
    Interop_marshal(mstyle, *env, cstyle);
    return mstyle.release();
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mstyle, JNIEnv &env, const Style &cstyle) NOTHROWS
{
    jclass Interop_class = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/Interop");
    jmethodID Interop_createStyle = env.GetStaticMethodID(Interop_class, "createStyle", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)Lcom/atakmap/map/layer/feature/style/Style;");
    TAK::Engine::Feature::StylePtr retval(cstyle.clone(), Style::destructStyle);
    Java::JNILocalRef mstylePtr(env, NewPointer(&env, std::move(retval)));
    mstyle = Java::JNILocalRef(env, env.CallStaticObjectMethod(Interop_class, Interop_createStyle, mstylePtr.get(), NULL));
    return TE_Ok;
}
jobject TAKEngineJNI::Interop::Feature::Interop_create(JNIEnv *env, const AttributeSet &cattr) NOTHROWS
{
    Java::JNILocalRef mattr(*env, nullptr);
    Interop_marshal(mattr, *env, cattr);
    return mattr.release();
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mattrs, JNIEnv &env, const AttributeSet &cattr) NOTHROWS
{
    jclass AttributeSet_class = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/AttributeSet");
    jmethodID AttributeSet_ctor = env.GetMethodID(AttributeSet_class, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
    AttributeSetPtr retval(new AttributeSet(cattr), Memory_deleter_const<AttributeSet>);
    Java::JNILocalRef mattrsPtr(env, NewPointer(&env, std::move(retval)));
    mattrs = Java::JNILocalRef(env, env.NewObject(AttributeSet_class, AttributeSet_ctor, mattrsPtr.get(), nullptr));
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Feature::Interop_copy(Envelope2 *value, JNIEnv *env, jobject jenvelope) NOTHROWS
{
    if(!value)
        return TE_InvalidArg;
    if(!env)
        return TE_InvalidArg;
    if(!jenvelope)
        return TE_InvalidArg;
    if(!checkInit(*env))
        return TE_IllegalState;

    value->minX = env->GetDoubleField(jenvelope, Envelope_class.minX);
    value->minY = env->GetDoubleField(jenvelope, Envelope_class.minY);
    value->minZ = env->GetDoubleField(jenvelope, Envelope_class.minZ);
    value->maxX = env->GetDoubleField(jenvelope, Envelope_class.maxX);
    value->maxY = env->GetDoubleField(jenvelope, Envelope_class.maxY);
    value->maxZ = env->GetDoubleField(jenvelope, Envelope_class.maxZ);

    return TE_Ok;
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(jobject value, JNIEnv &env, const TAK::Engine::Feature::Envelope2 &cenvelope) NOTHROWS
{
    if(!value)
        return TE_InvalidArg;
    if(!checkInit(env))
        return TE_IllegalState;

    env.SetDoubleField(value, Envelope_class.minX, cenvelope.minX);
    env.SetDoubleField(value, Envelope_class.minY, cenvelope.minY);
    env.SetDoubleField(value, Envelope_class.minZ, cenvelope.minZ);
    env.SetDoubleField(value, Envelope_class.maxX, cenvelope.maxX);
    env.SetDoubleField(value, Envelope_class.maxY, cenvelope.maxY);
    env.SetDoubleField(value, Envelope_class.maxZ, cenvelope.maxZ);

    return TE_Ok;
}
TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Feature::Envelope2 &cenvelope) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    value = JNILocalRef(env, env.NewObject(Envelope_class.id, Envelope_class.ctor__DDDDDD, cenvelope.minX, cenvelope.minY, cenvelope.minZ, cenvelope.maxX, cenvelope.maxY, cenvelope.maxZ));
    return TE_Ok;
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(JNILocalRef &maltMode, JNIEnv &env, const TAK::Engine::Feature::AltitudeMode &caltMode) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(caltMode == TAK::Engine::Feature::AltitudeMode::TEAM_ClampToGround)
        maltMode = JNILocalRef(env, AltitudeMode_enum.ClampToGround);
    else if(caltMode == TAK::Engine::Feature::AltitudeMode::TEAM_Relative)
        maltMode = JNILocalRef(env, AltitudeMode_enum.Relative);
    else if(caltMode == TAK::Engine::Feature::AltitudeMode::TEAM_Absolute)
        maltMode = JNILocalRef(env, AltitudeMode_enum.Absolute);
    else
        return TE_InvalidArg;
    return TE_Ok;
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(TAK::Engine::Feature::AltitudeMode &caltMode, JNIEnv &env, jobject maltMode) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(maltMode == nullptr)
        return TE_InvalidArg;
    else if(env.IsSameObject(maltMode, AltitudeMode_enum.ClampToGround))
        caltMode = TAK::Engine::Feature::AltitudeMode::TEAM_ClampToGround;
    else if(env.IsSameObject(maltMode, AltitudeMode_enum.Relative))
        caltMode = TAK::Engine::Feature::AltitudeMode::TEAM_Relative;
    else if(env.IsSameObject(maltMode, AltitudeMode_enum.Absolute))
        caltMode = TAK::Engine::Feature::AltitudeMode::TEAM_Absolute;
    else
        return TE_InvalidArg;
    return TE_Ok;
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(JNILocalRef &mlineMode, JNIEnv &env, const TAK::Engine::Feature::LineMode &clineMode) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(clineMode == TAK::Engine::Feature::LineMode::TELM_Rhumb)
        mlineMode = JNILocalRef(env, LineMode_enum.Rhumb);
    else if(clineMode == TAK::Engine::Feature::LineMode::TELM_GreatCircle)
        mlineMode = JNILocalRef(env, LineMode_enum.GreatCircle);
    else
        return TE_InvalidArg;
    return TE_Ok;
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(TAK::Engine::Feature::LineMode &clineMode, JNIEnv &env, jobject mlineMode) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(mlineMode == nullptr)
        return TE_InvalidArg;
    else if(env.IsSameObject(mlineMode, LineMode_enum.Rhumb))
        clineMode = TAK::Engine::Feature::LineMode::TELM_Rhumb;
    else if(env.IsSameObject(mlineMode, LineMode_enum.GreatCircle))
        clineMode = TAK::Engine::Feature::LineMode::TELM_GreatCircle;
    else
        return TE_InvalidArg;
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mfeature, JNIEnv &env, const Feature2 &cfeature) NOTHROWS
{
    return Interop_marshal(mfeature, env, FeaturePtr(new Feature2(cfeature), Memory_deleter_const<Feature2>));
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mfeature, JNIEnv &env, FeaturePtr &&cfeature) NOTHROWS
{
    checkInit(env);
    Java::JNILocalRef mfeaturePtr(env, NewPointer(&env, std::move(cfeature)));
    mfeature = Java::JNILocalRef(env, env.NewObject(Feature_class.id, Feature_class.ctor__LPointer, mfeaturePtr.get(), nullptr));
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeaturePtr &cfeature, JNIEnv &env, jobject mfeature) NOTHROWS
{
    if(!mfeature)
        return TE_InvalidArg;
    checkInit(env);
    Java::JNILocalRef mpointer(env, env.GetObjectField(mfeature, Feature_class.pointer));
    if(!mpointer)
        return TE_IllegalState;
    auto f = Pointer_get<Feature2>(env, mpointer);
    if(!f)
        return TE_IllegalState;
    cfeature = FeaturePtr(new Feature2(*f), Memory_deleter_const<Feature2>);
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeaturePtr_const &cfeature, JNIEnv &env, jobject mfeature) NOTHROWS
{
    FeaturePtr retval(nullptr, nullptr);
    const auto code = Interop_marshal(retval, env, mfeature);
    if(code != TE_Ok)
        return code;
    cfeature = FeaturePtr_const(retval.release(), retval.get_deleter());
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mfeatureset, JNIEnv &env, const FeatureSet2 &cfeatureset) NOTHROWS
{
    checkInit(env);
    int64_t fsid = cfeatureset.getId();
    if(fsid == FeatureDataStore2::FEATURESET_ID_NONE)
        fsid = com_atakmap_map_layer_feature_FeatureDataStore2_FEATURESET_ID_NONE;
    Java::JNILocalRef mprovider(env, env.NewStringUTF(cfeatureset.getProvider()));
    Java::JNILocalRef mtype(env, env.NewStringUTF(cfeatureset.getType()));
    Java::JNILocalRef mname(env, env.NewStringUTF(cfeatureset.getName()));
    int64_t version = cfeatureset.getVersion();
    if(version == FeatureDataStore2::FEATURESET_VERSION_NONE)
        version = com_atakmap_map_layer_feature_FeatureDataStore2_FEATURESET_VERSION_NONE;
    mfeatureset = Java::JNILocalRef(env,
    env.NewObject(FeatureSet_class.id,
                  FeatureSet_class.ctor__JLStringLStringLStringDDJ,
                  (jlong)fsid,
                  mprovider.get(),
                  mtype.get(),
                  mname.get(),
                  (jdouble)cfeatureset.getMinResolution(),
                  (jdouble)cfeatureset.getMaxResolution(),
                  (jlong)version));
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mfeatureset, JNIEnv &env, FeatureSetPtr &&cfeatureset) NOTHROWS
{
    return Interop_marshal(mfeatureset, env, *cfeatureset);
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeatureSetPtr &cfeatureset, JNIEnv &env, jobject mfeatureset) NOTHROWS
{
    if(!mfeatureset)
        return TE_InvalidArg;
    checkInit(env);
    jlong fsid = env.GetLongField(mfeatureset, FeatureSet_class.fsid);
    if(fsid == com_atakmap_map_layer_feature_FeatureDataStore2_FEATURESET_ID_NONE)
        fsid = FeatureDataStore2::FEATURESET_ID_NONE;
    TAK::Engine::Port::String cprovider;
    ATAKMapEngineJNI_GetStringField(cprovider, env, mfeatureset, FeatureSet_class.provider);
    TAK::Engine::Port::String ctype;
    ATAKMapEngineJNI_GetStringField(ctype, env, mfeatureset, FeatureSet_class.type);
    TAK::Engine::Port::String cname;
    ATAKMapEngineJNI_GetStringField(cname, env, mfeatureset, FeatureSet_class.name);
    jlong version = env.GetLongField(mfeatureset, FeatureSet_class.version);
    if(version == com_atakmap_map_layer_feature_FeatureDataStore2_FEATURESET_VERSION_NONE)
        version = FeatureDataStore2::FEATURESET_VERSION_NONE;
    cfeatureset = FeatureSetPtr(
            new FeatureSet2(
                    fsid,
                    cprovider,
                    ctype,
                    cname,
                    env.GetDoubleField(mfeatureset, FeatureSet_class.minGsd),
                    env.GetDoubleField(mfeatureset, FeatureSet_class.maxGsd),
                    version),
           Memory_deleter_const<FeatureSet2>);
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeatureSetPtr_const &cfeatureset, JNIEnv &env, jobject mfeatureset) NOTHROWS
{
    FeatureSetPtr retval(nullptr, nullptr);
    const auto code = Interop_marshal(retval, env, mfeatureset);
    if(code != TE_Ok)
        return code;
    cfeatureset = FeatureSetPtr_const(retval.release(), retval.get_deleter());
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mparams, JNIEnv &env, const FeatureDataStore2::FeatureQueryParameters &cparams) NOTHROWS
{
    checkInit(env);
    mparams = Java::JNILocalRef(env, env.NewObject(FeatureQueryParameters_class.id, FeatureQueryParameters_class.ctor));
    if(!cparams.featureSetIds->empty() || !cparams.featureSets->empty() || !cparams.providers->empty() || !cparams.types->empty() || !TE_ISNAN(cparams.minResolution) || !TE_ISNAN(cparams.maxResolution)) {
        Java::JNILocalRef mfeatureSetFilter(env, env.NewObject(FeatureSetQueryParameters_class.id, FeatureSetQueryParameters_class.ctor));
        env.SetObjectField(mparams, FeatureQueryParameters_class.featureSetFilter, mfeatureSetFilter);

        addAllIfNotEmpty(mfeatureSetFilter, env, FeatureSetQueryParameters_class.ids, *cparams.featureSetIds);
        addAllIfNotEmpty(mfeatureSetFilter, env, FeatureSetQueryParameters_class.names, *cparams.featureSets);
        addAllIfNotEmpty(mfeatureSetFilter, env, FeatureSetQueryParameters_class.providers, *cparams.providers);
        addAllIfNotEmpty(mfeatureSetFilter, env, FeatureSetQueryParameters_class.types, *cparams.types);
        env.SetDoubleField(mfeatureSetFilter, FeatureSetQueryParameters_class.minResolution, cparams.minResolution);
        env.SetDoubleField(mfeatureSetFilter, FeatureSetQueryParameters_class.maxResolution, cparams.maxResolution);
    }

    addAllIfNotEmpty(mparams, env, FeatureQueryParameters_class.ids, *cparams.featureIds);
    addAllIfNotEmpty(mparams, env, FeatureQueryParameters_class.names, *cparams.featureNames);

    if(!cparams.geometryTypes->empty()) {
        Java::JNILocalRef mgeometryTypes = Java::JNICollection_create(env, Java::JNICollectionClass::HashSet);
        Java::JNICollection_addAll<atakmap::feature::Geometry::Type>(mgeometryTypes, env, *cparams.geometryTypes,
         [](Java::JNILocalRef &melem, JNIEnv &env, const atakmap::feature::Geometry::Type &celem)
         {
             if(celem == atakmap::feature::Geometry::Type::POINT)
                 melem = Java::JNILocalRef(env, env.NewLocalRef(Point_class.id));
             else if(celem == atakmap::feature::Geometry::Type::LINESTRING)
                 melem = Java::JNILocalRef(env, env.NewLocalRef(LineString_class.id));
             else if(celem == atakmap::feature::Geometry::Type::POLYGON)
                 melem = Java::JNILocalRef(env, env.NewLocalRef(Polygon_class.id));
             else if(celem == atakmap::feature::Geometry::Type::COLLECTION)
                 melem = Java::JNILocalRef(env, env.NewLocalRef(GeometryCollection_class.id));
             return TE_Ok;
         });
        env.SetObjectField(mparams, FeatureQueryParameters_class.geometryTypes, mgeometryTypes);
    }

    env.SetBooleanField(mparams, FeatureQueryParameters_class.visibleOnly, cparams.visibleOnly);
//jfieldID spatialFilter; // Geometry

    if(cparams.spatialFilter) {
        Geometry2Ptr cspatialFilter(nullptr, nullptr);
        LegacyAdapters_adapt(cspatialFilter, *cparams.spatialFilter);
        Java::JNILocalRef mspatialFilter(env, Interop_create(&env, *cspatialFilter));
        env.SetObjectField(mparams, FeatureQueryParameters_class.spatialFilter, mspatialFilter);
    }


    jint mignoredFeatureProperties = 0;
    if(cparams.ignoredFields&FeatureDataStore2::FeatureQueryParameters::IgnoreFields::NameField)
        mignoredFeatureProperties |= com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_NAME;
    if(cparams.ignoredFields&FeatureDataStore2::FeatureQueryParameters::IgnoreFields::StyleField)
        mignoredFeatureProperties |= com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_STYLE;
    if(cparams.ignoredFields&FeatureDataStore2::FeatureQueryParameters::IgnoreFields::AttributesField)
        mignoredFeatureProperties |= com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_ATTRIBUTES;
    if(cparams.ignoredFields&FeatureDataStore2::FeatureQueryParameters::IgnoreFields::GeometryField)
        mignoredFeatureProperties |= com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_GEOMETRY;
    env.SetIntField(mparams, FeatureQueryParameters_class.ignoredFeatureProperties, mignoredFeatureProperties);

    if(!cparams.ops->empty()) {
        Java::JNILocalRef mspatialOps = Java::JNICollection_create(env, Java::JNICollectionClass::LinkedList);
        Java::JNICollection_addAll<FeatureDataStore2::FeatureQueryParameters::SpatialOp>(mspatialOps, env, *cparams.ops,
         [](Java::JNILocalRef &melem, JNIEnv &env, const FeatureDataStore2::FeatureQueryParameters::SpatialOp &celem)
         {
             if(celem.type == TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::SpatialOp::Simplify) {
                 melem = Java::JNILocalRef(env, env.NewObject(FeatureQueryParameters_class.SpatialOp_class.Simplify_class.id, FeatureQueryParameters_class.SpatialOp_class.Simplify_class.ctor__D, celem.args.simplify.distance));
             } else if(celem.type == TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::SpatialOp::Buffer) {
                 melem = Java::JNILocalRef(env, env.NewObject(FeatureQueryParameters_class.SpatialOp_class.Buffer_class.id, FeatureQueryParameters_class.SpatialOp_class.Buffer_class.ctor__D, celem.args.buffer.distance));
             }
             return TE_Ok;
         });
        env.SetObjectField(mparams, FeatureQueryParameters_class.spatialOps, mspatialOps);
    }

    if(!cparams.order->empty()) {
        Java::JNILocalRef morder = Java::JNICollection_create(env, Java::JNICollectionClass::LinkedList);
        Java::JNICollection_addAll<FeatureDataStore2::FeatureQueryParameters::Order>(morder, env, *cparams.order,
        [](Java::JNILocalRef &melem, JNIEnv &env, const FeatureDataStore2::FeatureQueryParameters::Order &celem)
        {
            if(celem.type == TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::Order::FeatureName) {
                melem = Java::JNILocalRef(env, env.NewObject(FeatureQueryParameters_class.Order_class.Name_class.id, FeatureQueryParameters_class.Order_class.Name_class.ctor));
            } else if(celem.type == TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::Order::FeatureId) {
                melem = Java::JNILocalRef(env, env.NewObject(FeatureQueryParameters_class.Order_class.ID_class.id, FeatureQueryParameters_class.Order_class.ID_class.ctor));
            } else if(celem.type == TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::Order::Distance) {
                Java::JNILocalRef mpoint(env, Core::Interop_create(&env, TAK::Engine::Core::GeoPoint2(celem.args.distance.y, celem.args.distance.x)));
                melem = Java::JNILocalRef(env, env.NewObject(FeatureQueryParameters_class.Order_class.Distance_class.id, FeatureQueryParameters_class.Order_class.Distance_class.ctor__LGeoPoint, mpoint.get()));
            }
            return TE_Ok;
        });
        env.SetObjectField(mparams, FeatureQueryParameters_class.order, morder);
    }

    env.SetIntField(mparams, FeatureQueryParameters_class.limit, cparams.limit);
    env.SetIntField(mparams, FeatureQueryParameters_class.offset, cparams.offset);


    // XXX - the following fields have no equivalent
    //jfieldID timeout; // J
    //jfieldID attributeFilters; // Set
    //jfieldID minimumTimestamp; // J
    //jfieldID maximumTimestamp; // J
    //jfieldID altitudeMode; // Feature$AltitudeMode
    //jfieldID extrudedOnly; // Z

    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeatureDataStore2::FeatureQueryParameters *cparams, JNIEnv &env, jobject mparams) NOTHROWS
{
    if(!mparams)
        return TE_Ok;
    checkInit(env);
    Java::JNILocalRef mfeatureSetFilter(env, env.GetObjectField(mparams, FeatureQueryParameters_class.featureSetFilter));
    if(mfeatureSetFilter) {
        addAllIfNotNull(*cparams->featureSetIds, env, mfeatureSetFilter, FeatureSetQueryParameters_class.ids);
        addAllIfNotNull(*cparams->featureSets, env, mfeatureSetFilter, FeatureSetQueryParameters_class.names);
        addAllIfNotNull(*cparams->types, env, mfeatureSetFilter, FeatureSetQueryParameters_class.types);
        addAllIfNotNull(*cparams->providers, env, mfeatureSetFilter, FeatureSetQueryParameters_class.providers);
        cparams->minResolution = env.GetDoubleField(mfeatureSetFilter, FeatureSetQueryParameters_class.minResolution);
        cparams->maxResolution = env.GetDoubleField(mfeatureSetFilter, FeatureSetQueryParameters_class.maxResolution);
    }
    addAllIfNotNull(*cparams->featureIds, env, mparams, FeatureQueryParameters_class.ids);
    addAllIfNotNull(*cparams->featureNames, env, mparams, FeatureQueryParameters_class.names);

    Java::JNILocalRef mgeometryTypes(env, env.GetObjectField(mparams, FeatureQueryParameters_class.geometryTypes));
    if(mgeometryTypes) {
        Java::JNICollection_addAll<atakmap::feature::Geometry::Type>(cparams->geometryTypes, env, mgeometryTypes,
        [](JNIEnv &env, jobject melem)
        {
            if(ATAKMapEngineJNI_equals(&env, melem, Point_class.id))
                return atakmap::feature::Geometry::POINT;
            else if(ATAKMapEngineJNI_equals(&env, melem, LineString_class.id))
                return atakmap::feature::Geometry::LINESTRING;
            else if(ATAKMapEngineJNI_equals(&env, melem, Polygon_class.id))
                return atakmap::feature::Geometry::POLYGON;
            else if(ATAKMapEngineJNI_equals(&env, melem, GeometryCollection_class.id))
                return atakmap::feature::Geometry::COLLECTION;
            else
                // XXX -
                return (atakmap::feature::Geometry::Type)-1;
        });
    }

    const auto mignoredFields = env.GetIntField(mparams, FeatureQueryParameters_class.ignoredFeatureProperties);
    cparams->ignoredFields = 0u;
    if(mignoredFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_GEOMETRY)
        cparams->ignoredFields |= FeatureDataStore2::FeatureQueryParameters::GeometryField;
    if(mignoredFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_ATTRIBUTES)
        cparams->ignoredFields |= FeatureDataStore2::FeatureQueryParameters::AttributesField;
    if(mignoredFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_STYLE)
        cparams->ignoredFields |= FeatureDataStore2::FeatureQueryParameters::StyleField;
    if(mignoredFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_NAME)
        cparams->ignoredFields |= FeatureDataStore2::FeatureQueryParameters::NameField;

    Java::JNILocalRef mspatialOps(env, env.GetObjectField(mparams, FeatureQueryParameters_class.spatialOps));
    if(mspatialOps) {
        Java::JNICollection_addAll<FeatureDataStore2::FeatureQueryParameters::SpatialOp>(cparams->ops, env, mspatialOps,
         [](JNIEnv &env, jobject melem)
         {
             FeatureDataStore2::FeatureQueryParameters::SpatialOp cop;
             Java::JNILocalRef melem_class(env, env.GetObjectClass(melem));
             if(ATAKMapEngineJNI_equals(&env, melem_class, FeatureQueryParameters_class.SpatialOp_class.Simplify_class.id)) {
                 cop.type = TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::SpatialOp::Simplify;
                 cop.args.simplify.distance = env.GetDoubleField(melem, FeatureQueryParameters_class.SpatialOp_class.Simplify_class.distance);
             } else if(ATAKMapEngineJNI_equals(&env, melem_class, FeatureQueryParameters_class.SpatialOp_class.Buffer_class.id)) {
                 cop.type = TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::SpatialOp::Buffer;
                 cop.args.buffer.distance = env.GetDoubleField(melem, FeatureQueryParameters_class.SpatialOp_class.Buffer_class.distance);
             }
             return cop;
         });
    }

    Java::JNILocalRef morder(env, env.GetObjectField(mparams, FeatureQueryParameters_class.order));
    if(morder) {
        Java::JNICollection_addAll<FeatureDataStore2::FeatureQueryParameters::Order>(cparams->order, env, morder,
         [](JNIEnv &env, jobject melem)
         {
            FeatureDataStore2::FeatureQueryParameters::Order corder;
            Java::JNILocalRef melem_class(env, env.GetObjectClass(melem));
            if(ATAKMapEngineJNI_equals(&env, melem_class, FeatureQueryParameters_class.Order_class.Distance_class.id)) {
                TAK::Engine::Core::GeoPoint2 cpoint;
                Java::JNILocalRef mpoint(env, env.GetObjectField(melem, FeatureQueryParameters_class.Order_class.Distance_class.point));
                Core::Interop_copy(&cpoint, &env, mpoint);
                corder.type = TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::Order::Distance;
                corder.args.distance.x = cpoint.longitude;
                corder.args.distance.y = cpoint.latitude;
            } else if(ATAKMapEngineJNI_equals(&env, melem_class, FeatureQueryParameters_class.Order_class.ID_class.id)) {
                corder.type = TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::Order::FeatureId;
            } else if(ATAKMapEngineJNI_equals(&env, melem_class, FeatureQueryParameters_class.Order_class.Name_class.id)) {
                corder.type = TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters::Order::FeatureName;
            }
            return corder;
         });
    }

    Java::JNILocalRef mspatialFilter(env, env.GetObjectField(mparams, FeatureQueryParameters_class.spatialFilter));
    if(mspatialFilter) {
        const Geometry2 *cspatialFilter;
        Interop_get(&cspatialFilter, &env, mspatialFilter);
        LegacyAdapters_adapt(cparams->spatialFilter, *cspatialFilter);
    }

    cparams->visibleOnly = env.GetBooleanField(mparams, FeatureQueryParameters_class.visibleOnly);
    cparams->offset = env.GetIntField(mparams, FeatureQueryParameters_class.offset);
    cparams->limit = env.GetIntField(mparams, FeatureQueryParameters_class.limit);

    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mparams, JNIEnv &env, const FeatureDataStore2::FeatureSetQueryParameters &cparams) NOTHROWS
{
    mparams = Java::JNILocalRef(env, env.NewObject(FeatureSetQueryParameters_class.id, FeatureSetQueryParameters_class.ctor));
    if(!cparams.ids->empty()) {
        Java::JNILocalRef mids = Java::JNICollection_create(env, Java::JNICollectionClass::HashSet);
        Java::JNICollection_addAll(mids, env, *cparams.ids);
        env.SetObjectField(mparams, FeatureSetQueryParameters_class.ids, mids.get());
    }
    if(!cparams.providers->empty()) {
        Java::JNILocalRef mproviders = Java::JNICollection_create(env, Java::JNICollectionClass::HashSet);
        Java::JNICollection_addAll(mproviders, env, *cparams.providers);
        env.SetObjectField(mparams, FeatureSetQueryParameters_class.providers, mproviders.get());
    }
    if(!cparams.types->empty()) {
        Java::JNILocalRef mtypes = Java::JNICollection_create(env, Java::JNICollectionClass::HashSet);
        Java::JNICollection_addAll(mtypes, env, *cparams.types);
        env.SetObjectField(mparams, FeatureSetQueryParameters_class.types, mtypes.get());
    }
    if(!cparams.names->empty()) {
        Java::JNILocalRef mnames = Java::JNICollection_create(env, Java::JNICollectionClass::HashSet);
        Java::JNICollection_addAll(mnames, env, *cparams.names);
        env.SetObjectField(mparams, FeatureSetQueryParameters_class.names, mnames.get());
    }
    env.SetBooleanField(mparams, FeatureSetQueryParameters_class.visibleOnly, cparams.visibleOnly);
    env.SetIntField(mparams, FeatureSetQueryParameters_class.offset, cparams.offset);
    env.SetIntField(mparams, FeatureSetQueryParameters_class.limit, cparams.limit);
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeatureDataStore2::FeatureSetQueryParameters *cparams, JNIEnv &env, jobject mparams) NOTHROWS
{
    if(!mparams)
        return TE_Ok;
    checkInit(env);
    Java::JNILocalRef mids(env, env.GetObjectField(mparams, FeatureSetQueryParameters_class.ids));
    if(mids)
        Java::JNICollection_addAll(cparams->ids, env, mids);
    Java::JNILocalRef mnames(env, env.GetObjectField(mparams, FeatureSetQueryParameters_class.names));
    if(mnames)
        Java::JNICollection_addAll(cparams->names, env, mnames);
    Java::JNILocalRef mproviders(env, env.GetObjectField(mparams, FeatureSetQueryParameters_class.providers));
    if(mproviders)
        Java::JNICollection_addAll(cparams->providers, env, mproviders);
    Java::JNILocalRef mtypes(env, env.GetObjectField(mparams, FeatureSetQueryParameters_class.types));
    if(mtypes)
        Java::JNICollection_addAll(cparams->types, env, mtypes);
    cparams->visibleOnly = env.GetBooleanField(mparams, FeatureSetQueryParameters_class.visibleOnly);
    cparams->offset = env.GetIntField(mparams, FeatureSetQueryParameters_class.offset);
    cparams->limit = env.GetIntField(mparams, FeatureSetQueryParameters_class.limit);
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeatureDataStore2 **cdatastore, JNIEnv &env, jobject mdatastore) NOTHROWS
{
    checkInit(env);
    TAKErr code(TE_Ok);
    const auto isWrapper = FeatureDataStore2_interop.isWrapper(env, mdatastore);
    if(!isWrapper) {
        *cdatastore = nullptr;
        return TE_InvalidArg;
    }
    std::shared_ptr<FeatureDataStore2> unwrapped;
    code = FeatureDataStore2_interop.marshal<ManagedFeatureDataStore2>(unwrapped, env, mdatastore);
    TE_CHECKRETURN_CODE(code);
    *cdatastore = unwrapped.get();
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(std::shared_ptr<FeatureDataStore2> &cdatastore, JNIEnv &env, jobject mdatastore) NOTHROWS
{
    checkInit(env);
    return FeatureDataStore2_interop.marshal<ManagedFeatureDataStore2>(cdatastore, env, mdatastore);
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(FeatureDataStore2Ptr &cdatastore, JNIEnv &env, jobject mdatastore) NOTHROWS
{
    checkInit(env);
    return FeatureDataStore2_interop.marshal<ManagedFeatureDataStore2>(cdatastore, env, mdatastore);
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mdatastore, JNIEnv &env, const FeatureDataStore2 &cdatastore) NOTHROWS
{
    checkInit(env);
    return FeatureDataStore2_interop.marshal<ManagedFeatureDataStore2>(mdatastore, env, cdatastore);
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mdatastore, JNIEnv &env, const std::shared_ptr<FeatureDataStore2> &cdatastore) NOTHROWS
{
    checkInit(env);
    return FeatureDataStore2_interop.marshal<ManagedFeatureDataStore2>(mdatastore, env, cdatastore);
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mdatastore, JNIEnv &env, FeatureDataStore2Ptr &&cdatastore) NOTHROWS
{
    checkInit(env);
    return FeatureDataStore2_interop.marshal<ManagedFeatureDataStore2>(mdatastore, env, std::move(cdatastore));
}
TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Traits &ctraits, JNIEnv &env, jobject mtraits) NOTHROWS
{
    if(!mtraits)
        return TE_InvalidArg;
    checkInit(env);
    Java::JNILocalRef maltitudeMode(env, env.GetObjectField(mtraits, Feature_Traits_class.altitudeMode));
    TAKErr code = Interop_marshal(ctraits.altitudeMode, env, maltitudeMode);
    TE_CHECKRETURN_CODE(code)

    ctraits.extrude = env.GetDoubleField(mtraits, Feature_Traits_class.extrude);

    Java::JNILocalRef mlineMode(env, env.GetObjectField(mtraits, Feature_Traits_class.lineMode));
    code = Interop_marshal(ctraits.lineMode, env, mlineMode);
    TE_CHECKRETURN_CODE(code)
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(Java::JNILocalRef &mtraits, JNIEnv &env, const TAK::Engine::Feature::Traits &ctraits) NOTHROWS
{
    checkInit(env);
    mtraits = Java::JNILocalRef(env, env.NewObject(Feature_Traits_class.id, Feature_Traits_class.ctor));
    Java::JNILocalRef maltitudeMode(env, nullptr);
    Interop_marshal(maltitudeMode, env, ctraits.altitudeMode);
    env.SetObjectField(mtraits, Feature_Traits_class.altitudeMode, maltitudeMode.release());

    env.SetDoubleField(mtraits, Feature_Traits_class.extrude, ctraits.extrude);

    Java::JNILocalRef mlineMode(env, nullptr);
    Interop_marshal(mlineMode, env, ctraits.lineMode);
    env.SetObjectField(mtraits, Feature_Traits_class.lineMode, mlineMode.release());
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Feature::Interop_marshal(TAK::Engine::Renderer::Feature::GLBatchGeometryFeatureDataStoreRenderer3::Options &coptions, JNIEnv &env, jobject moptions) NOTHROWS
{
    if(!moptions)
        return TE_InvalidArg;
    checkInit(env);
    coptions.labelsEnabled = env.GetBooleanField(moptions, GLBatchGeometryFeatureDataStoreRenderer2_Options_class.labelsEnabled);
    Java::JNILocalRef mfilter(env, env.GetObjectField(moptions, GLBatchGeometryFeatureDataStoreRenderer2_Options_class.filter));

    if (mfilter) {
        TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters cparams;
        TAKErr code = Interop_marshal(&cparams, env, mfilter);
        TE_CHECKRETURN_CODE(code)
        coptions.filter = cparams;
    }
    return TE_Ok;
}

namespace
{
    template<class T>
    InteropImpl<T>::InteropImpl(JNIEnv &env, const char *classname, TAKErr (*cloneFn_)(std::unique_ptr<T, void(*)(const T *)> &, const T &) NOTHROWS) NOTHROWS :
        cloneFn(cloneFn_),
        pointerFieldId(0)
    {
        do {
            jclass clazz = ATAKMapEngineJNI_findClass(&env, classname);
            if(!clazz)
                break;
            pointerFieldId = env.GetFieldID(clazz, "pointer", "Lcom/atakmap/interop/Pointer;");
            if(!pointerFieldId)
                break;
        } while(false);
    }
    template<class T>
    TAKErr InteropImpl<T>::create(std::unique_ptr<T, void(*)(const T *)> &value, JNIEnv &env, jobject managed) NOTHROWS
    {
        TAKErr code(TE_Ok);
        T *pvalue;
        code = get(&pvalue, env, managed);
        TE_CHECKRETURN_CODE(code);
        code = cloneFn(value, *pvalue);
        TE_CHECKRETURN_CODE(code);

        return code;
    }
    template<class T>
    TAKErr InteropImpl<T>::get(std::shared_ptr<T> &value, JNIEnv &env, jobject managed) NOTHROWS
    {
        if(!pointerFieldId)
            return TE_InvalidArg;
        TAKErr code(TE_Ok);
        jobject jpointer = env.GetObjectField(managed, pointerFieldId);
        if(!jpointer)
            return TE_Err;
        if(env.GetIntField(jpointer, Pointer_class.type) == com_atakmap_interop_Pointer_SHARED) {
            jlong pointer = env.GetLongField(jpointer, Pointer_class.value);
            std::shared_ptr<T> *ptr = JLONG_TO_INTPTR(std::shared_ptr<T>, pointer);
            value = std::shared_ptr<T>(*ptr);
        } else {
            std::unique_ptr<T, void(*)(const T *)> uvalue(NULL, NULL);
            code = create(uvalue, env, managed);
            TE_CHECKRETURN_CODE(code);
            value = std::move(uvalue);
        }
        env.DeleteLocalRef(jpointer);
        return code;
    }
    template<class T>
    TAKErr InteropImpl<T>::get(T **value, JNIEnv &env, jobject managed) NOTHROWS
    {
        if(!pointerFieldId)
            return TE_InvalidArg;
        jobject jpointer = env.GetObjectField(managed, pointerFieldId);
        if(!jpointer)
            return TE_Err;
        *value = Pointer_get<T>(env, jpointer);
        env.DeleteLocalRef(jpointer);
        return TE_Ok;
    }

    TAKErr geometry_clone(Geometry2Ptr &value, const Geometry2 &geom) NOTHROWS
    {
        return Geometry_clone(value, geom);
    }
    TAKErr style_clone(TAK::Engine::Feature::StylePtr &value, const Style &style) NOTHROWS
    {
        value = TAK::Engine::Feature::StylePtr(style.clone(), Style::destructStyle);
        return TE_Ok;
    }
    TAKErr attributeset_clone(AttributeSetPtr &value, const AttributeSet &attrs) NOTHROWS
    {
        value = AttributeSetPtr(new AttributeSet(attrs), Memory_deleter_const<AttributeSet>);
        return TE_Ok;
    }

    InteropImpl<Geometry2> &geometryInterop(JNIEnv &env) NOTHROWS
    {
        static InteropImpl<Geometry2> geom(env, "com/atakmap/map/layer/feature/geometry/Geometry", geometry_clone);
        return geom;
    }
    InteropImpl<Style> &styleInterop(JNIEnv &env) NOTHROWS
    {
        static InteropImpl<Style> style(env, "com/atakmap/map/layer/feature/style/Style", style_clone);
        return style;
    }
    InteropImpl<AttributeSet> &attributeSetInterop(JNIEnv &env) NOTHROWS
    {
        static InteropImpl<AttributeSet> attr(env, "com/atakmap/map/layer/feature/AttributeSet", attributeset_clone);
        return attr;
    }

    bool checkInit(JNIEnv &env) NOTHROWS
    {
        static bool clinit = Feature_interop_init(env);
        return clinit;
    }
    bool Feature_interop_init(JNIEnv &env) NOTHROWS
    {
        Feature_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/Feature");
        Feature_class.pointer = env.GetFieldID(Feature_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        Feature_class.ctor__LPointer = env.GetMethodID(Feature_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;)V");

        FeatureSet_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureSet");
        FeatureSet_class.fsid = env.GetFieldID(FeatureSet_class.id, "id", "J");
        FeatureSet_class.provider = env.GetFieldID(FeatureSet_class.id, "provider", "Ljava/lang/String;");
        FeatureSet_class.type = env.GetFieldID(FeatureSet_class.id, "type", "Ljava/lang/String;");
        FeatureSet_class.name = env.GetFieldID(FeatureSet_class.id, "name", "Ljava/lang/String;");
        FeatureSet_class.minGsd = env.GetFieldID(FeatureSet_class.id, "minGsd", "D");
        FeatureSet_class.maxGsd = env.GetFieldID(FeatureSet_class.id, "maxGsd", "D");
        FeatureSet_class.version = env.GetFieldID(FeatureSet_class.id, "version", "J");
        FeatureSet_class.ctor__JLStringLStringLStringDDJ = env.GetMethodID(FeatureSet_class.id, "<init>", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;DDJ)V");

        FeatureQueryParameters_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters");
        FeatureQueryParameters_class.featureSetFilter = env.GetFieldID(FeatureQueryParameters_class.id, "featureSetFilter", "Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureSetQueryParameters;");
        FeatureQueryParameters_class.ids = env.GetFieldID(FeatureQueryParameters_class.id, "ids", "Ljava/util/Set;");
        FeatureQueryParameters_class.names = env.GetFieldID(FeatureQueryParameters_class.id, "names", "Ljava/util/Set;");
        FeatureQueryParameters_class.geometryTypes = env.GetFieldID(FeatureQueryParameters_class.id, "geometryTypes", "Ljava/util/Set;");
        FeatureQueryParameters_class.attributeFilters = env.GetFieldID(FeatureQueryParameters_class.id, "attributeFilters", "Ljava/util/Set;");
        FeatureQueryParameters_class.visibleOnly = env.GetFieldID(FeatureQueryParameters_class.id, "visibleOnly", "Z");
        FeatureQueryParameters_class.spatialFilter = env.GetFieldID(FeatureQueryParameters_class.id, "spatialFilter", "Lcom/atakmap/map/layer/feature/geometry/Geometry;");
        FeatureQueryParameters_class.minimumTimestamp = env.GetFieldID(FeatureQueryParameters_class.id, "minimumTimestamp", "J");
        FeatureQueryParameters_class.maximumTimestamp = env.GetFieldID(FeatureQueryParameters_class.id, "maximumTimestamp", "J");
        FeatureQueryParameters_class.altitudeMode = env.GetFieldID(FeatureQueryParameters_class.id, "altitudeMode", "Lcom/atakmap/map/layer/feature/Feature$AltitudeMode;");
        FeatureQueryParameters_class.extrudedOnly = env.GetFieldID(FeatureQueryParameters_class.id, "extrudedOnly", "Z");
        FeatureQueryParameters_class.ignoredFeatureProperties = env.GetFieldID(FeatureQueryParameters_class.id, "ignoredFeatureProperties", "I");
        FeatureQueryParameters_class.spatialOps = env.GetFieldID(FeatureQueryParameters_class.id, "spatialOps", "Ljava/util/Collection;");
        FeatureQueryParameters_class.order = env.GetFieldID(FeatureQueryParameters_class.id, "order", "Ljava/util/Collection;");
        FeatureQueryParameters_class.limit = env.GetFieldID(FeatureQueryParameters_class.id, "limit", "I");
        FeatureQueryParameters_class.offset = env.GetFieldID(FeatureQueryParameters_class.id, "offset", "I");
        FeatureQueryParameters_class.timeout = env.GetFieldID(FeatureQueryParameters_class.id, "timeout", "J");
        FeatureQueryParameters_class.ctor = env.GetMethodID(FeatureQueryParameters_class.id, "<init>", "()V");

        FeatureQueryParameters_class.Order_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters$Order");
        FeatureQueryParameters_class.Order_class.Name_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters$Order$Name");
        FeatureQueryParameters_class.Order_class.Name_class.ctor = env.GetMethodID(FeatureQueryParameters_class.Order_class.Name_class.id, "<init>", "()V");
        FeatureQueryParameters_class.Order_class.ID_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters$Order$ID");
        FeatureQueryParameters_class.Order_class.ID_class.ctor = env.GetMethodID(FeatureQueryParameters_class.Order_class.ID_class.id, "<init>", "()V");
        FeatureQueryParameters_class.Order_class.Distance_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters$Order$Distance");
        FeatureQueryParameters_class.Order_class.Distance_class.point = env.GetFieldID(FeatureQueryParameters_class.Order_class.Distance_class.id, "point", "Lcom/atakmap/coremap/maps/coords/GeoPoint;");
        FeatureQueryParameters_class.Order_class.Distance_class.ctor__LGeoPoint = env.GetMethodID(FeatureQueryParameters_class.Order_class.Distance_class.id, "<init>", "(Lcom/atakmap/coremap/maps/coords/GeoPoint;)V");

        FeatureQueryParameters_class.SpatialOp_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters$SpatialOp");
        FeatureQueryParameters_class.SpatialOp_class.Buffer_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters$SpatialOp$Buffer");
        FeatureQueryParameters_class.SpatialOp_class.Buffer_class.distance = env.GetFieldID(FeatureQueryParameters_class.SpatialOp_class.Buffer_class.id, "distance", "D");
        FeatureQueryParameters_class.SpatialOp_class.Buffer_class.ctor__D = env.GetMethodID(FeatureQueryParameters_class.SpatialOp_class.Buffer_class.id, "<init>", "(D)V");
        FeatureQueryParameters_class.SpatialOp_class.Simplify_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters$SpatialOp$Simplify");
        FeatureQueryParameters_class.SpatialOp_class.Simplify_class.distance = env.GetFieldID(FeatureQueryParameters_class.SpatialOp_class.Simplify_class.id, "distance", "D");
        FeatureQueryParameters_class.SpatialOp_class.Simplify_class.ctor__D = env.GetMethodID(FeatureQueryParameters_class.SpatialOp_class.Simplify_class.id, "<init>", "(D)V");

        FeatureSetQueryParameters_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2$FeatureSetQueryParameters");
        FeatureSetQueryParameters_class.ids = env.GetFieldID(FeatureSetQueryParameters_class.id, "ids", "Ljava/util/Set;");
        FeatureSetQueryParameters_class.names = env.GetFieldID(FeatureSetQueryParameters_class.id, "names", "Ljava/util/Set;");
        FeatureSetQueryParameters_class.types = env.GetFieldID(FeatureSetQueryParameters_class.id, "types", "Ljava/util/Set;");
        FeatureSetQueryParameters_class.providers = env.GetFieldID(FeatureSetQueryParameters_class.id, "providers", "Ljava/util/Set;");
        FeatureSetQueryParameters_class.minResolution = env.GetFieldID(FeatureSetQueryParameters_class.id, "minResolution", "D");
        FeatureSetQueryParameters_class.maxResolution = env.GetFieldID(FeatureSetQueryParameters_class.id, "maxResolution", "D");
        FeatureSetQueryParameters_class.visibleOnly = env.GetFieldID(FeatureSetQueryParameters_class.id, "visibleOnly", "Z");
        FeatureSetQueryParameters_class.limit = env.GetFieldID(FeatureSetQueryParameters_class.id, "limit", "I");
        FeatureSetQueryParameters_class.offset = env.GetFieldID(FeatureSetQueryParameters_class.id, "offset", "I");
        FeatureSetQueryParameters_class.ctor = env.GetMethodID(FeatureSetQueryParameters_class.id, "<init>", "()V");

        Point_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/geometry/Point");
        LineString_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/geometry/LineString");
        Polygon_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/geometry/Polygon");
        GeometryCollection_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/geometry/GeometryCollection");

        Envelope_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/geometry/Envelope");
        Envelope_class.minX = env.GetFieldID(Envelope_class.id, "minX", "D");
        Envelope_class.minY = env.GetFieldID(Envelope_class.id, "minY", "D");
        Envelope_class.minZ = env.GetFieldID(Envelope_class.id, "minZ", "D");
        Envelope_class.maxX = env.GetFieldID(Envelope_class.id, "maxX", "D");
        Envelope_class.maxY = env.GetFieldID(Envelope_class.id, "maxY", "D");
        Envelope_class.maxZ = env.GetFieldID(Envelope_class.id, "maxZ", "D");
        Envelope_class.ctor__DDDDDD = env.GetMethodID(Envelope_class.id, "<init>", "(DDDDDD)V");

        Feature_Traits_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/Feature$Traits");
        Feature_Traits_class.ctor = env.GetMethodID(Feature_Traits_class.id, "<init>", "()V");
        Feature_Traits_class.altitudeMode = env.GetFieldID(Feature_Traits_class.id, "altitudeMode", "Lcom/atakmap/map/layer/feature/Feature$AltitudeMode;");
        Feature_Traits_class.extrude = env.GetFieldID(Feature_Traits_class.id, "extrude", "D");
        Feature_Traits_class.lineMode = env.GetFieldID(Feature_Traits_class.id, "lineMode", "Lcom/atakmap/map/layer/feature/Feature$LineMode;");

        {
            Java::JNILocalRef enumValue(env, nullptr);
            const char enumClass[] = "com/atakmap/map/layer/feature/Feature$AltitudeMode";
            if(Java::JNIEnum_value(enumValue, env, enumClass, "ClampToGround") != TE_Ok)
                return false;
            AltitudeMode_enum.ClampToGround = env.NewWeakGlobalRef(enumValue);
            if(Java::JNIEnum_value(enumValue, env, enumClass, "Relative") != TE_Ok)
                return false;
            AltitudeMode_enum.Relative = env.NewWeakGlobalRef(enumValue);
            if(Java::JNIEnum_value(enumValue, env, enumClass, "Absolute") != TE_Ok)
                return false;
            AltitudeMode_enum.Absolute = env.NewWeakGlobalRef(enumValue);
        }

        {
            Java::JNILocalRef enumValue(env, nullptr);
            const char enumClass[] = "com/atakmap/map/layer/feature/Feature$LineMode";
            if(Java::JNIEnum_value(enumValue, env, enumClass, "Rhumb") != TE_Ok)
                return false;
            LineMode_enum.Rhumb = env.NewWeakGlobalRef(enumValue);
            if(Java::JNIEnum_value(enumValue, env, enumClass, "GreatCircle") != TE_Ok)
                return false;
            LineMode_enum.GreatCircle = env.NewWeakGlobalRef(enumValue);
        }

        GLBatchGeometryFeatureDataStoreRenderer2_Options_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureLayer3$Options");
        GLBatchGeometryFeatureDataStoreRenderer2_Options_class.labelsEnabled = env.GetFieldID(GLBatchGeometryFeatureDataStoreRenderer2_Options_class.id, "labelsEnabled", "Z");
        GLBatchGeometryFeatureDataStoreRenderer2_Options_class.filter = env.GetFieldID(GLBatchGeometryFeatureDataStoreRenderer2_Options_class.id, "filter", "Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters;");

        struct
        {
            jclass id;
            jfieldID pointer;
            jmethodID ctor;
        } NativeFeatureDataStore2_class;

        NativeFeatureDataStore2_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/NativeFeatureDataStore2");
        NativeFeatureDataStore2_class.pointer = env.GetFieldID(NativeFeatureDataStore2_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeFeatureDataStore2_class.ctor = env.GetMethodID(NativeFeatureDataStore2_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");

        FeatureDataStore2_interop.init(env, NativeFeatureDataStore2_class.id, NativeFeatureDataStore2_class.pointer, NativeFeatureDataStore2_class.ctor);
        return true;
    }
}