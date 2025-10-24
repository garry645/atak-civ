#ifndef TAKENGINEJNI_INTEROP_JAVA_JNICOLLECTION_H_INCLUDED
#define TAKENGINEJNI_INTEROP_JAVA_JNICOLLECTION_H_INCLUDED

#include <jni.h>

#include <functional>

#include <port/Collection.h>
#include <port/Platform.h>
#include <port/String.h>
#include <util/Error.h>

#include "interop/java/JNIIterator.h"
#include "interop/java/JNILocalRef.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Java {
            enum JNICollectionClass
            {
                ArrayList,
                LinkedList,
                HashSet,
            };

            JNILocalRef JNICollection_create(JNIEnv &env, const JNICollectionClass type);
            TAK::Engine::Util::TAKErr JNICollection_add(JNIEnv &env, jobject collection, jobject element) NOTHROWS;
            TAK::Engine::Util::TAKErr JNICollection_remove(JNIEnv &env, jobject collection, jobject element) NOTHROWS;
            TAK::Engine::Util::TAKErr JNICollection_clear(JNIEnv &env, jobject collection) NOTHROWS;
            TAK::Engine::Util::TAKErr JNICollection_iterator(JNILocalRef &iterator, JNIEnv &env, jobject collection) NOTHROWS;
            TAK::Engine::Util::TAKErr JNICollection_size(std::size_t &size, JNIEnv &env, jobject collection) NOTHROWS;

            template<class T>
            TAK::Engine::Util::TAKErr JNICollection_addAll(jobject mvalues, JNIEnv &env, const TAK::Engine::Port::Collection<T> &cvalues, const std::function<TAK::Engine::Util::TAKErr(JNILocalRef &, JNIEnv &, const T &)> &marshal) NOTHROWS
            {
                TAK::Engine::Util::TAKErr code(TAK::Engine::Util::TE_Ok);
                if(const_cast<TAK::Engine::Port::Collection<T> &>(cvalues).empty())
                    return TAK::Engine::Util::TE_Ok;
                typename TAK::Engine::Port::Collection<T>::IteratorPtr cit(nullptr, nullptr);
                code = const_cast<TAK::Engine::Port::Collection<T> &>(cvalues).iterator(cit);
                do {
                    T celem;
                    code = cit->get(celem);
                    TE_CHECKBREAK_CODE(code);
                    JNILocalRef melem(env, nullptr);
                    code = marshal(melem, env, celem);
                    TE_CHECKBREAK_CODE(code);
                    code = JNICollection_add(env, mvalues, melem.get());
                    TE_CHECKBREAK_CODE(code);
                    code = cit->next();
                    if(code == TAK::Engine::Util::TE_Done)
                        break;
                    TE_CHECKBREAK_CODE(code);
                } while(true);
                if(code == TAK::Engine::Util::TE_Done)
                    code = TAK::Engine::Util::TE_Ok;
                return code;
            }
            template<class T>
            TAK::Engine::Util::TAKErr JNICollection_addAll(TAK::Engine::Port::Collection<T> *cvalues, JNIEnv &env, jobject mvalues, const std::function<T(JNIEnv &, jobject)> &marshal) NOTHROWS
            {
                TAK::Engine::Util::TAKErr code(TAK::Engine::Util::TE_Ok);
                if(!cvalues || !mvalues)
                    return TAK::Engine::Util::TE_InvalidArg;
                JNILocalRef mit(env, nullptr);
                code = JNICollection_iterator(mit, env, mvalues);
                TE_CHECKRETURN_CODE(code);
                do {
                    bool hasNext = false;
                    code = JNIIterator_hasNext(hasNext, env, mit);
                    TE_CHECKBREAK_CODE(code);
                    if(!hasNext)
                        break;
                    JNILocalRef melem(env, nullptr);
                    code = JNIIterator_next(melem, env, mit);
                    TE_CHECKBREAK_CODE(code);
                    T celem = marshal(env, melem);
                    code = cvalues->add(celem);
                    TE_CHECKBREAK_CODE(code);
                } while(true);
                return code;
            }

            TAK::Engine::Util::TAKErr JNICollection_addAll(TAK::Engine::Port::Collection<TAK::Engine::Port::String> *cvalues, JNIEnv &env, jobject mvalues) NOTHROWS;
            TAK::Engine::Util::TAKErr JNICollection_addAll(jobject mvalues, JNIEnv &env, const TAK::Engine::Port::Collection<TAK::Engine::Port::String> &cvalues) NOTHROWS;

            TAK::Engine::Util::TAKErr JNICollection_addAll(TAK::Engine::Port::Collection<int64_t> *cvalues, JNIEnv &env, jobject mvalues) NOTHROWS;
            TAK::Engine::Util::TAKErr JNICollection_addAll(jobject mvalues, JNIEnv &env, const TAK::Engine::Port::Collection<int64_t> &cvalues) NOTHROWS;
        }
    }
}

#endif
