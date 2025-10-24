include mk/nghttp2-common.mk

ifeq ($(and $(nghttp2_CFLAGS)),)
    $(error Required var not set)
endif

nghttp2_src_lib=$(OUTDIR)/$(nghttp2_srcdir)/$(nghttp2_libfile)

$(nghttp2_configtouchfile): $(nghttp2_srctouchfile)
	cd $(OUTDIR)/$(nghttp2_srcdir) &&                       \
		CFLAGS="$(nghttp2_CFLAGS)"                      \
		LDFLAGS="$(nghttp2_LDFLAGS)"                    \
		CC="$(CC)"                                      \
		CPP="$(CPP)"                                    \
		CXX="$(CXX)"                                    \
		$(if $(nghttp2_LIBS),LIBS="$(nghttp2_LIBS)",)      \
		./configure                                     \
		$(CONFIGURE_TARGET)                             \
		$(CONFIGURE_$(BUILD_TYPE))                      \
                --enable-lib-only                               \
		--disable-shared                                \
                $(nghttp2_EXTRACONFIG)                          \
		--prefix=$(OUTDIR_CYGSAFE)
	touch $@

# This is phony because we always want to be invoking nghttp2 make to be sure
# the files are up to date;  it knows if anything needs to be done
.PHONY: nghttp2_build
nghttp2_build: $(nghttp2_configtouchfile)
	$(MAKE) -C $(OUTDIR)/$(nghttp2_srcdir)

$(nghttp2_src_lib): nghttp2_build
	@echo "nghttp2 built"

$(nghttp2_out_lib): $(nghttp2_src_lib)
	$(MAKE) -C $(OUTDIR)/$(nghttp2_srcdir) install
	cd $(OUTDIR)/lib && ( test "`echo *.la`" = "*.la" && true || cd $(OUTDIR)/lib && for i in *.la ; do dos2unix $$i ; done )

