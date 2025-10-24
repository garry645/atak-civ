include mk/ngtcp2-common.mk

ifeq ($(and $(ngtcp2_CFLAGS)),)
    $(error Required var not set)
endif

$(ngtcp2_configtouchfile): $(ngtcp2_srctouchfile)
	cd $(OUTDIR)/$(ngtcp2_srcdir) &&                       \
		CFLAGS="$(ngtcp2_CFLAGS)"                      \
		LDFLAGS="$(ngtcp2_LDFLAGS)"                    \
		CC="$(CC)"                                     \
		CPP="$(CPP)"                                   \
		CXX="$(CXX)"                                   \
                PKG_CONFIG_PATH="$(OUTDIR)/lib/pkgconfig"      \
		$(if $(ngtcp2_LIBS),LIBS="$(ngtcp2_LIBS)",)    \
		./configure                                    \
		$(CONFIGURE_TARGET)                            \
		$(CONFIGURE_$(BUILD_TYPE))                     \
                --with-openssl                                 \
                --disable-shared                               \
                $(ngtcp2_EXTRACONFIG)                          \
		--prefix=$(OUTDIR_CYGSAFE)
	touch $@

# This is phony because we always want to be invoking ngtcp2 make to be sure
# the files are up to date;  it knows if anything needs to be done
.PHONY: ngtcp2_build
ngtcp2_build: $(ngtcp2_configtouchfile)
	$(MAKE) -C $(OUTDIR)/$(ngtcp2_srcdir)

$(ngtcp2_src_lib): ngtcp2_build
	@echo "ngtcp2 built"

$(ngtcp2_out_lib): $(ngtcp2_src_lib)
	$(MAKE) -C $(OUTDIR)/$(ngtcp2_srcdir) install
	cd $(OUTDIR)/lib && ( test "`echo *.la`" = "*.la" && true || cd $(OUTDIR)/lib && for i in *.la ; do dos2unix $$i ; done )

