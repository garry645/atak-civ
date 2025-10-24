ifeq ($(and $(ngtcp2_win_arch)),)
    $(error Required var not set)
endif

include mk/ngtcp2-common.mk

# This is phony because we always want to be invoking ngtcp2 make to be sure
# the files are up to date;  it knows if anything needs to be done
.PHONY: ngtcp2_build
ngtcp2_build: $(ngtcp2_srctouchfile) cmake_check
	cd $(OUTDIR)/$(ngtcp2_srcdir) && mkdir -p bdir-$(BUILD_TYPE)
	cd $(OUTDIR)/$(ngtcp2_srcdir)/bdir-$(BUILD_TYPE) &&              \
	    $(VS_SETUP) \"`cygpath -m $(CMAKE)`\"                        \
		-G \"NMake Makefiles\"                                   \
		-DCMAKE_BUILD_TYPE=$(BUILD_TYPE)                         \
		-DENABLE_OPENSSL=ON                                      \
		-DENABLE_STATIC_LIB=OFF                                  \
                -DCMAKE_INSTALL_PREFIX=../../                            \
                ../
	cd $(OUTDIR)/$(ngtcp2_srcdir)/bdir-$(BUILD_TYPE) &&              \
	    $(VS_SETUP) nmake


$(ngtcp2_src_lib): ngtcp2_build
	@echo "ngtcp2 built"

$(ngtcp2_out_lib): $(ngtcp2_src_lib)
	cd $(OUTDIR)/$(ngtcp2_srcdir)/bdir-$(BUILD_TYPE) &&              \
	    $(VS_SETUP) nmake install

