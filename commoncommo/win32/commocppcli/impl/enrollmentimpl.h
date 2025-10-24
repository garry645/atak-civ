#ifndef ENROLLMENTIMPL_H_
#define ENROLLMENTIMPL_H_

#include "enrollment.h"
#include "enrollment_cli.h"
#include <msclr/marshal.h>

namespace TAK {
    namespace Commo {
        namespace impl {

            class EnrollmentIOImpl : public atakmap::commoncommo::EnrollmentIO {
            public:
                EnrollmentIOImpl(IEnrollmentIO ^io);
                ~EnrollmentIOImpl();

                virtual void enrollmentUpdate(const atakmap::commoncommo::EnrollmentIOUpdate *update);

                static EnrollmentStep nativeToCLI(atakmap::commoncommo::EnrollmentStep step);

            private:
                gcroot<IEnrollmentIO ^> ioCLI;
            };
        }
    }
}



#endif
