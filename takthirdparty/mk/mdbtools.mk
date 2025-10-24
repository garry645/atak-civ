include mk/mdbtools-common.mk

ifeq ($(and $(mdbtools_CFLAGS)),)
    $(error Required var not set)
endif

$(mdbtools_configtouchfile): $(mdbtools_srctouchfile)
	cd $(OUTDIR)/$(mdbtools_srcdir) &&                   \
		CFLAGS="-I$(OUTDIR_CYGSAFE)/include $(mdbtools_CFLAGS)"                  \
		LDFLAGS="-L$(OUTDIR_CYGSAFE)/lib $(mdbtools_LDFLAGS)" \
		CC="$(CC)"                                   \
		CPP="$(CPP)"                                 \
		CXX="$(CXX)"                                 \
		$(if $(mdbtools_LIBS),LIBS="$(mdbtools_LIBS)",)      \
		./configure                                  \
		$(CONFIGURE_TARGET)                          \
		$(CONFIGURE_$(BUILD_TYPE))                   \
                --disable-glib                               \
		--with-libiconv-prefix=$(OUTDIR_CYGSAFE)     \
		--disable-shared                             \
		--disable-man                                \
                --enable-iconv                               \
		--prefix=$(OUTDIR_CYGSAFE)
	touch $@

# This is phony because we always want to be invoking mdbtools make to be sure
# the files are up to date;  it knows if anything needs to be done
.PHONY: mdbtools_build
mdbtools_build: $(mdbtools_configtouchfile)
	$(MAKE) -C $(OUTDIR)/$(mdbtools_srcdir)

$(mdbtools_src_lib): mdbtools_build
	@echo "mdbtools built"

$(mdbtools_out_lib): $(mdbtools_src_lib)
	$(MAKE) -C $(OUTDIR)/$(mdbtools_srcdir) install
	cd $(OUTDIR)/lib && ( test "`echo *.la`" = "*.la" && true || cd $(OUTDIR)/lib && for i in *.la ; do dos2unix $$i ; done )

