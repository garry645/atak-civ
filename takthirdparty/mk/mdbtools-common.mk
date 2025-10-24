.PHONY: mdbtools mdbtools_clean

mdbtools_libfile=$(LIB_PREFIX)mdb.$(LIB_STATICSUFFIX)
mdbtools_src=$(DISTFILESDIR)/mdbtools.tar.gz
mdbtools_srcdir=mdbtools
mdbtools_srctouchfile=$(OUTDIR)/$(mdbtools_srcdir)/.unpacked
mdbtools_configtouchfile=$(OUTDIR)/$(mdbtools_srcdir)/.configured
mdbtools_out_lib=$(OUTDIR)/lib/$(mdbtools_libfile)
mdbtools_src_lib=$(OUTDIR)/$(mdbtools_srcdir)/$(mdbtools_libfile)
mdbtools_patch=$(DISTFILESDIR)/mdbtools-pgsc.patch

$(mdbtools_srctouchfile): $(mdbtools_src)
	rm -rf $(OUTDIR)/$(mdbtools_srcdir)
	tar -x -z -f $< -C $(OUTDIR)
	cd $(OUTDIR) && mv mdbtools-* $(mdbtools_srcdir)
	patch -p0 -d $(OUTDIR) < $(mdbtools_patch)
	cd $(OUTDIR)/$(mdbtools_srcdir) && chmod 755 configure
	touch $@

mdbtools: $(mdbtools_out_lib)

mdbtools_clean:
	rm -rf $(OUTDIR)/$(mdbtools_srcdir)
	rm -f $(mdbtools_out_lib)
