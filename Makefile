#-------------------------------------------------------------------------
#
# Makefile for src/interfaces
#
# Copyright (c) 1994, Regents of the University of California
#
# $Header: /cvsroot/jdbc/pgjdbc/Attic/Makefile,v 1.31 2001/05/25 14:28:58 momjian Exp $
#
#-------------------------------------------------------------------------

subdir = src/interfaces/jdbc
top_builddir = ../../..
include $(top_builddir)/src/Makefile.global

majorversion := $(shell echo $(VERSION) | sed 's/^\([0-9][0-9]*\)\..*$$/\1/')
minorversion := $(shell echo $(VERSION) | sed 's/^[0-9][0-9]*\.\([0-9][0-9]*\).*$$/\1/')

properties := -Dmajor=$(majorversion) -Dminor=$(minorversion) \
		-Dfullversion=$(VERSION) \
		-Ddef_pgport=$(DEF_PGPORT)

all:
	$(ANT) -buildfile $(top_srcdir)/build.xml $(properties)

install: installdirs
	$(ANT) -Dinstall.directory=$(javadir) \
		-buildfile $(top_srcdir)/build.xml \
		install $(properties)

installdirs:
	$(mkinstalldirs) $(DESTDIR)$(datadir)/java

uninstall:
	$(ANT) -Dinstall.directory=$(DESTDIR)$(datadir)/java \
		-buildfile $(top_srcdir)/build.xml \
		uninstall

clean distclean maintainer-clean:
	$(ANT) -buildfile $(top_srcdir)/build.xml clean
