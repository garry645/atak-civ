include mk/nghttp2-common.mk

bdir=bdir-$(BUILD_TYPE)
nghttp2_src_lib=$(OUTDIR)/$(nghttp2_srcdir)/$(bdir)/lib/$(nghttp2_libfile)

# This is phony because we always want to be invoking nghttp2 make to be sure
# the files are up to date;  it knows if anything needs to be done
.PHONY: nghttp2_build
nghttp2_build: $(nghttp2_srctouchfile) cmake_check
	cd $(OUTDIR)/$(nghttp2_srcdir) && mkdir -p bdir-$(BUILD_TYPE)
	cd $(OUTDIR)/$(nghttp2_srcdir)/bdir-$(BUILD_TYPE) &&              \
	    $(VS_SETUP) \"`cygpath -m $(CMAKE)`\"                         \
		-G \"NMake Makefiles\"                                    \
		-DENABLE_LIB_ONLY=ON -DCMAKE_BUILD_TYPE=$(BUILD_TYPE)     \
                -DCMAKE_INSTALL_PREFIX=../../                             \
                ../
	cd $(OUTDIR)/$(nghttp2_srcdir)/bdir-$(BUILD_TYPE) &&              \
	    $(VS_SETUP) nmake

$(nghttp2_src_lib): nghttp2_build
	@echo "nghttp2 built"

$(nghttp2_out_lib): $(nghttp2_src_lib)
	cd $(OUTDIR)/$(nghttp2_srcdir)/bdir-$(BUILD_TYPE) &&              \
	    $(VS_SETUP) nmake install


