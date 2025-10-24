
#include "interop/raster/tilematrix/Interop.h"
#include "interop/raster/tilematrix/ManagedTileClientSpi.h"
#include "interop/raster/tilematrix/ManagedTileContainerSpi.h"
#include "interop/raster/tilematrix/ManagedTileMatrix.h"
#include "interop/raster/tilematrix/ManagedTileClient.h"
#include "interop/raster/tilematrix/ManagedTileContainer.h"

#include "interop/InterfaceMarshalContext.h"
#include "interop/ImplementationMarshalContext.h"

#include "interop/JNIStringUTF.h"

using namespace TAKEngineJNI::Interop;
using namespace TAK::Engine::Util;

namespace {
    struct {
        jclass id;
        jfieldID pointer;
        jmethodID ctor;
    } NativeTileClientSpi_class;

    struct {
        jclass id;
        jfieldID pointer;
        jmethodID ctor;
    } NativeTileContainerSpi_class;

    struct {
        jclass id;
        jfieldID pointer;
        jmethodID ctor;
    } NativeTileMatrix_class;

    struct {
        jclass id;
        jfieldID pointer;
        jmethodID ctor;
    } NativeTileClient_class;

    struct {
        jclass id;
        jfieldID pointer;
        jmethodID ctor;
    } NativeTileContainer_class;

    struct {
        jclass id;
        jfieldID pointer;
        jmethodID ctor;
    } NativeCacheRequestListener_class;

    InterfaceMarshalContext<TAK::Engine::Raster::TileMatrix::CacheRequestListener> CacheRequestListener_interop;
    InterfaceMarshalContext<TAK::Engine::Raster::TileMatrix::TileClientSpi> TileClientSpi_interop;
    InterfaceMarshalContext<TAK::Engine::Raster::TileMatrix::TileContainerSpi> TileContainerSpi_interop;
    InterfaceMarshalContext<TAK::Engine::Raster::TileMatrix::TileMatrix> TileMatrix_interop;
    InterfaceMarshalContext<TAK::Engine::Raster::TileMatrix::TileClient> TileClient_interop;
    InterfaceMarshalContext<TAK::Engine::Raster::TileMatrix::TileContainer> TileContainer_interop;

    struct {
        jclass id;
        jmethodID onRequestStartedID;
        jmethodID onRequestCompleteID;
        jmethodID onRequestProgressID;
        jmethodID onRequestErrorID;
        jmethodID onRequestCanceledID;
    } CacheRequestListener_class;

    bool CacheRequestListener_class_init(JNIEnv* env) {
        CacheRequestListener_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/contentservices/CacheRequestListener");
        CacheRequestListener_class.onRequestStartedID = env->GetMethodID(CacheRequestListener_class.id, "onRequestStarted", "()V");
        CacheRequestListener_class.onRequestCompleteID = env->GetMethodID(CacheRequestListener_class.id, "onRequestComplete", "()V");
        CacheRequestListener_class.onRequestProgressID = env->GetMethodID(CacheRequestListener_class.id, "onRequestProgress", "(IIIIII)V");
        CacheRequestListener_class.onRequestErrorID = env->GetMethodID(CacheRequestListener_class.id, "onRequestError", "(Ljava/lang/Throwable;Ljava/lang/String;Z)Z");
        CacheRequestListener_class.onRequestCanceledID = env->GetMethodID(CacheRequestListener_class.id, "onRequestCanceled", "()V");
        return true;
    }

    bool Interop_class_init(JNIEnv &env) NOTHROWS {

        NativeCacheRequestListener_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/contentservices/NativeCacheRequestListener");
        NativeCacheRequestListener_class.pointer = env.GetFieldID(NativeCacheRequestListener_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeCacheRequestListener_class.ctor = env.GetMethodID(NativeCacheRequestListener_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
        CacheRequestListener_interop.init(env, NativeCacheRequestListener_class.id, NativeCacheRequestListener_class.pointer, NativeCacheRequestListener_class.ctor);

        NativeTileClientSpi_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/NativeTileClientSpi");
        NativeTileClientSpi_class.pointer = env.GetFieldID(NativeTileClientSpi_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeTileClientSpi_class.ctor = env.GetMethodID(NativeTileClientSpi_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");

        NativeTileContainerSpi_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/NativeTileContainerSpi");
        NativeTileContainerSpi_class.pointer = env.GetFieldID(NativeTileContainerSpi_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeTileContainerSpi_class.ctor = env.GetMethodID(NativeTileContainerSpi_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");

        TileClientSpi_interop.init(env, NativeTileClientSpi_class.id, NativeTileClientSpi_class.pointer, NativeTileClientSpi_class.ctor);
        TileContainerSpi_interop.init(env, NativeTileContainerSpi_class.id, NativeTileContainerSpi_class.pointer, NativeTileContainerSpi_class.ctor);

        NativeTileMatrix_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/NativeTileMatrix");
        NativeTileMatrix_class.pointer = env.GetFieldID(NativeTileClientSpi_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeTileMatrix_class.ctor = env.GetMethodID(NativeTileClientSpi_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
        TileMatrix_interop.init(env, NativeTileMatrix_class.id, NativeTileMatrix_class.pointer, NativeTileMatrix_class.ctor);

        NativeTileClient_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/NativeTileClient");
        NativeTileClient_class.pointer = env.GetFieldID(NativeTileClient_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeTileClient_class.ctor = env.GetMethodID(NativeTileClient_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
        TileClient_interop.init(env, NativeTileClient_class.id, NativeTileClient_class.pointer, NativeTileClient_class.ctor);

        NativeTileContainer_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/NativeTileContainer");
        NativeTileContainer_class.pointer = env.GetFieldID(NativeTileContainer_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeTileContainer_class.ctor = env.GetMethodID(NativeTileContainer_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
        TileContainer_interop.init(env, NativeTileContainer_class.id, NativeTileContainer_class.pointer, NativeTileContainer_class.ctor);

        return true;
    }

    bool checkInit(JNIEnv &env) NOTHROWS {
        static bool v = Interop_class_init(env) &&
                CacheRequestListener_class_init(&env);
        return v;
    }

    class ManagedCacheRequestListener : public TAK::Engine::Raster::TileMatrix::CacheRequestListener {
    public:
        ManagedCacheRequestListener(JNIEnv& env, jobject impl) : impl(env.NewGlobalRef(impl)) {
            checkInit(env);
        }

        ~ManagedCacheRequestListener() NOTHROWS {
            if(impl) {
                LocalJNIEnv env;
                env->DeleteGlobalRef(impl);
                impl = nullptr;
            }
        }

        TAKErr onRequestStarted() NOTHROWS {
            LocalJNIEnv env;
            env->CallVoidMethod(impl, CacheRequestListener_class.onRequestStartedID);
            return TE_Ok;
        }

        TAKErr onRequestComplete() NOTHROWS {
            LocalJNIEnv env;
            env->CallVoidMethod(impl, CacheRequestListener_class.onRequestCompleteID);
            return TE_Ok;
        }

        TAKErr onRequestProgress(int taskNum, int numTasks, int taskProgress, int maxTaskProgress, int totalProgress, int maxTotalProgress) NOTHROWS {
            LocalJNIEnv env;
            env->CallVoidMethod(impl, CacheRequestListener_class.onRequestProgressID,
                                taskNum,
                                numTasks,
                                taskProgress,
                                maxTaskProgress,
                                totalProgress,
                                maxTotalProgress);
            return TE_Ok;
        }
        TAKErr onRequestError(const char *message, bool fatal) NOTHROWS {
            LocalJNIEnv env;
            Java::JNILocalRef strRef(*env, env->NewStringUTF(message));
            jboolean v = env->CallBooleanMethod(impl, CacheRequestListener_class.onRequestErrorID,
                                                strRef.get(),
                                                fatal);
            return v ? TE_Ok : TE_EOF;
        }
        TAKErr onRequestCanceled() NOTHROWS {
            LocalJNIEnv env;
            env->CallVoidMethod(impl, CacheRequestListener_class.onRequestCanceledID);
            return TE_Ok;
        }

        jobject impl;
    };
}

//
// TileClientSpi
//

template<> TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileClientSpi>(bool *value, JNIEnv& env, jobject mspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileClientSpi_interop.isWrapper(env, mspi);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(bool *value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileClientSpi &cspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileClientSpi_interop.isWrapper<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClientSpi>(cspi);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClientSpi> &value, JNIEnv &env, jobject mspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClientSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClientSpi>(value, env, mspi);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(TAK::Engine::Raster::TileMatrix::TileClientSpiPtr &value, JNIEnv &env, jobject mspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClientSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClientSpi>(value, env, mspi);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClientSpi> &cspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClientSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClientSpi>(value, env, cspi);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileClientSpi &cspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClientSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClientSpi>(value, env, cspi);
}

//
// TileContainerSpi
//

template<> TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileContainerSpi>(bool *value, JNIEnv& env, jobject mspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileContainerSpi_interop.isWrapper(env, mspi);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(bool *value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileContainerSpi &cspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileContainerSpi_interop.isWrapper<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainerSpi>(cspi);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi> &value, JNIEnv &env, jobject mspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainerSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainerSpi>(value, env, mspi);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(TAK::Engine::Raster::TileMatrix::TileContainerSpiPtr &value, JNIEnv &env, jobject mspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainerSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainerSpi>(value, env, mspi);
}


TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi> &cspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainerSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainerSpi>(value, env, cspi);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileContainerSpi &cspi) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainerSpi_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainerSpi>(value, env, cspi);
}

//
// TileMatrix
//

template<> TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileMatrix>(bool *value, JNIEnv& env, jobject mtmatrix) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileMatrix_interop.isWrapper(env, mtmatrix);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(bool *value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileMatrix &ctmatrix) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileMatrix_interop.isWrapper<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileMatrix>(ctmatrix);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileMatrix> &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileMatrix_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileMatrix>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(TAK::Engine::Raster::TileMatrix::TileMatrixPtr &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileMatrix_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileMatrix>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileMatrix> &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileMatrix_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileMatrix>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileMatrix &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileMatrix_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileMatrix>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::TileMatrixPtr &&tmptr) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileMatrix_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileMatrix>(value, env, std::move(tmptr));
}

//
// TileClient
//

template<> TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileClient>(bool *value, JNIEnv& env, jobject mtc) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileClient_interop.isWrapper(env, mtc);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(bool *value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileClient &ctmatrix) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileClient_interop.isWrapper<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClient>(ctmatrix);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClient> &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClient_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClient>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(TAK::Engine::Raster::TileMatrix::TileClientPtr &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClient_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClient>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClient> &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClient_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClient>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileClient &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClient_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClient>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::TileClientPtr &&tmptr) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileClient_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileClient>(value, env, std::move(tmptr));
}

//
// TileContainer
//

template<> TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileContainer>(bool *value, JNIEnv& env, jobject mtc) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileContainer_interop.isWrapper(env, mtc);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(bool *value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileContainer &ctmatrix) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = TileContainer_interop.isWrapper<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainer>(ctmatrix);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainer> &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainer_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainer>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(TAK::Engine::Raster::TileMatrix::TileContainerPtr &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainer_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainer>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainer> &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainer_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainer>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileContainer &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainer_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainer>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::TileContainerPtr &&tmptr) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return TileContainer_interop.marshal<TAKEngineJNI::Interop::Raster::TileMatrix::ManagedTileContainer>(value, env, std::move(tmptr));
}


//
// CacheRequestListener
//

template<> TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::CacheRequestListener>(bool *value, JNIEnv& env, jobject mtc) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = CacheRequestListener_interop.isWrapper(env, mtc);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(bool *value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::CacheRequestListener &ctmatrix) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    *value = CacheRequestListener_interop.isWrapper<ManagedCacheRequestListener>(ctmatrix);
    return TE_Ok;
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::CacheRequestListener> &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return CacheRequestListener_interop.marshal<ManagedCacheRequestListener>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(TAK::Engine::Raster::TileMatrix::CacheRequestListenerPtr &value, JNIEnv &env, jobject ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return CacheRequestListener_interop.marshal<ManagedCacheRequestListener>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::CacheRequestListener> &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return CacheRequestListener_interop.marshal<ManagedCacheRequestListener>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::CacheRequestListener &ctm) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return CacheRequestListener_interop.marshal<ManagedCacheRequestListener>(value, env, ctm);
}

TAK::Engine::Util::TAKErr
TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::CacheRequestListenerPtr &&tmptr) NOTHROWS {
    if(!checkInit(env))
        return TE_IllegalState;
    return CacheRequestListener_interop.marshal<ManagedCacheRequestListener>(value, env, std::move(tmptr));
}

