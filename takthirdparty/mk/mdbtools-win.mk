ifeq ($(and $(mdbtools_win_arch)),)
    $(error Required var not set)
endif

include mk/mdbtools-common.mk

mdbtools_win32_dir=$(OUTDIR)/$(mdbtools_srcdir)/msvc/$(mdbtools_win_arch)/$(BUILD_TYPE)
mdbtools_src_lib=$(mdbtools_win32_dir)/libmdb.lib
mdbtools_projfile=mdbtools.sln
mdbtools_common_flags="/t:build"                                              \
                   "/p:Configuration=$(BUILD_TYPE)"                           \
                   "/p:Platform=$(mdbtools_win_arch)"

# This is phony because we always want to be invoking mdbtools make to be sure
# the files are up to date;  it knows if anything needs to be done
.PHONY: mdbtools_build
mdbtools_build: $(mdbtools_srctouchfile)
	cd $(OUTDIR)/$(mdbtools_srcdir)/msvc && \
	    $(VS_SETUP) msbuild $(mdbtools_common_flags) $(mdbtools_projfile)

$(mdbtools_src_lib): mdbtools_build
	@echo "mdbtools built"

$(mdbtools_out_lib): $(mdbtools_src_lib)
	$(CP) $< $@
	$(CP) $(mdbtools_win32_dir)/mdb-sql.exe $(OUTDIR)/bin/
	$(CP) $(mdbtools_win32_dir)/libmdb.dll $(OUTDIR)/bin/
	$(CP) $(mdbtools_win32_dir)/libmdbsql.dll $(OUTDIR)/bin/
	$(CP) $(mdbtools_win32_dir)/libmdbsql.lib $(OUTDIR)/lib/
	$(CP) $(OUTDIR)/$(mdbtools_srcdir)/include/mdbtools.h             \
	      $(OUTDIR)/$(mdbtools_srcdir)/include/mdbsql.h               \
	      $(OUTDIR)/$(mdbtools_srcdir)/include/mdbfakeglib.h          \
	      $(OUTDIR)/include/


