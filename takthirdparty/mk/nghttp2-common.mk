.PHONY: nghttp2 nghttp2_clean

nghttp2_libfile=$(LIB_PREFIX)nghttp2.$(LIB_STATICSUFFIX)
nghttp2_src=$(DISTFILESDIR)/nghttp2.tar.bz2
nghttp2_srcdir=nghttp2
nghttp2_srctouchfile=$(OUTDIR)/$(nghttp2_srcdir)/.unpacked
nghttp2_configtouchfile=$(OUTDIR)/$(nghttp2_srcdir)/.configured
nghttp2_out_lib=$(OUTDIR)/lib/$(nghttp2_libfile)
#nghttp2_patch=$(DISTFILESDIR)/nghttp2-pgsc.patch

$(nghttp2_srctouchfile): $(nghttp2_src)
	rm -rf $(OUTDIR)/$(nghttp2_srcdir)
	tar -x -j -f $< -C $(OUTDIR)
	cd $(OUTDIR) && mv nghttp2-* $(nghttp2_srcdir)
	# patch -p0 -d $(OUTDIR) < $(nghttp2_patch)
	cd $(OUTDIR)/$(nghttp2_srcdir) && chmod 755 configure
	touch $@

nghttp2: $(nghttp2_out_lib)

nghttp2_clean:
	rm -rf $(OUTDIR)/$(nghttp2_srcdir)
	rm -f $(nghttp2_out_lib)
