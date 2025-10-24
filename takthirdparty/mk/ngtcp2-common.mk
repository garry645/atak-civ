.PHONY: ngtcp2 ngtcp2_clean

ngtcp2_libfile=$(LIB_PREFIX)ngtcp2.$(LIB_STATICSUFFIX)
ngtcp2_src=$(DISTFILESDIR)/ngtcp2.tar.bz2
ngtcp2_srcdir=ngtcp2
ngtcp2_srctouchfile=$(OUTDIR)/$(ngtcp2_srcdir)/.unpacked
ngtcp2_configtouchfile=$(OUTDIR)/$(ngtcp2_srcdir)/.configured
ngtcp2_out_lib=$(OUTDIR)/lib/$(ngtcp2_libfile)
ngtcp2_src_lib=$(OUTDIR)/$(ngtcp2_srcdir)/$(ngtcp2_libfile)
# ngtcp2_patch=$(DISTFILESDIR)/ngtcp2-pgsc.patch

$(ngtcp2_srctouchfile): $(ngtcp2_src)
	rm -rf $(OUTDIR)/$(ngtcp2_srcdir)
	tar -x -j -f $< -C $(OUTDIR)
	cd $(OUTDIR) && mv ngtcp2-* $(ngtcp2_srcdir)
	# patch -p0 -d $(OUTDIR) < $(ngtcp2_patch)
	cd $(OUTDIR)/$(ngtcp2_srcdir) && chmod 755 configure
	touch $@

ngtcp2: $(ngtcp2_out_lib)

ngtcp2_clean:
	rm -rf $(OUTDIR)/$(ngtcp2_srcdir)
	rm -f $(ngtcp2_out_lib)
