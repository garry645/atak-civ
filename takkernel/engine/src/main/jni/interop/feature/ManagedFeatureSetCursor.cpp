#include "interop/feature/ManagedFeatureSetCursor.h"

#include "interop/JNIStringUTF.h"
#include "interop/db/Interop.h"
#include "interop/java/JNILocalRef.h"

using namespace TAKEngineJNI::Interop::Feature;

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI;

namespace
{
    struct
    {
        jclass id;
        jmethodID get;
        jmethodID getId;
        jmethodID getType;
        jmethodID getProvider;
        jmethodID getName;
        jmethodID getMinResolution;
        jmethodID getMaxResolution;
    } FeatureSetCursor_class;

    bool FeatureSetCursor_init(JNIEnv &env) NOTHROWS;
}

ManagedFeatureSetCursor::ManagedFeatureSetCursor(JNIEnv &env_, jobject impl_) NOTHROWS :
    impl(env_.NewGlobalRef(impl_)),
    row(nullptr, nullptr)
{
    static bool clinit = FeatureSetCursor_init(env_);
}
ManagedFeatureSetCursor::~ManagedFeatureSetCursor() NOTHROWS
{
    if(impl) {
        LocalJNIEnv env;
        TAKEngineJNI::Interop::DB::RowIterator_close(env, impl);
        env->DeleteGlobalRef(impl);
        impl = nullptr;
    }
}
TAKErr ManagedFeatureSetCursor::get(const FeatureSet2 **featureSet) NOTHROWS
{
    if(!row) {
        LocalJNIEnv env;
        const int64_t id = env->CallLongMethod(impl, FeatureSetCursor_class.getId);
        TAK::Engine::Port::String cprovider;
        Interop::Java::JNILocalRef mprovider(*env, env->CallObjectMethod(impl, FeatureSetCursor_class.getProvider));
        Interop::JNIStringUTF_get(cprovider, *env, mprovider);
        TAK::Engine::Port::String ctype;
        Interop::Java::JNILocalRef mtype(*env, env->CallObjectMethod(impl, FeatureSetCursor_class.getType));
        Interop::JNIStringUTF_get(ctype, *env, mtype);
        TAK::Engine::Port::String cname;
        Interop::Java::JNILocalRef mname(*env, env->CallObjectMethod(impl, FeatureSetCursor_class.getName));
        Interop::JNIStringUTF_get(cname, *env, mname);
        const double minGsd = env->CallDoubleMethod(impl, FeatureSetCursor_class.getMinResolution);
        const double maxGsd = env->CallDoubleMethod(impl, FeatureSetCursor_class.getMaxResolution);
        // XXX -
        const int64_t version = 1LL;
        row = FeatureSetPtr(new FeatureSet2(id, cprovider, ctype, cname, minGsd, maxGsd, version), Memory_deleter_const<FeatureSet2>);
    }
    *featureSet = row.get();
    return TE_Ok;
}
TAKErr ManagedFeatureSetCursor::moveToNext() NOTHROWS
{
    row.reset();
    LocalJNIEnv env;
    return TAKEngineJNI::Interop::DB::RowIterator_moveToNext(env, impl);
}

namespace
{
    bool FeatureSetCursor_init(JNIEnv &env) NOTHROWS
    {
        FeatureSetCursor_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureSetCursor");
        FeatureSetCursor_class.getId = env.GetMethodID(FeatureSetCursor_class.id, "getId", "()J");
        FeatureSetCursor_class.getProvider = env.GetMethodID(FeatureSetCursor_class.id, "getProvider", "()Ljava/lang/String;");
        FeatureSetCursor_class.getType = env.GetMethodID(FeatureSetCursor_class.id, "getType", "()Ljava/lang/String;");
        FeatureSetCursor_class.getName = env.GetMethodID(FeatureSetCursor_class.id, "getName", "()Ljava/lang/String;");
        FeatureSetCursor_class.getMinResolution = env.GetMethodID(FeatureSetCursor_class.id, "getMinResolution", "()D");
        FeatureSetCursor_class.getMaxResolution = env.GetMethodID(FeatureSetCursor_class.id, "getMaxResolution", "()D");

        return true;
    }
}
