#-------------------------------------------------------------------------
#
# Makefile
#    Makefile for Java JDBC interface
#
# IDENTIFICATION
#    $Header: /cvsroot/jdbc/pgjdbc/Attic/Makefile,v 1.17 2000/03/08 01:58:25 momjian Exp $
#
#-------------------------------------------------------------------------

FIND		= find
IDL2JAVA	= idltojava -fno-cpp -fno-tie
JAR		= jar
JAVA		= java
JAVAC		= javac -g
JAVADOC		= javadoc
RM		= rm -f
TOUCH		= touch

# This defines how to compile a java class
.java.class:
	$(JAVAC) $<

.SUFFIXES:	.class .java
.PHONY:		all clean doc examples msg

# In 6.5, the all rule builds the makeVersion class which then calls make using
# the jdbc1 or jdbc2 rules
all:	
	@echo ------------------------------------------------------------
	@echo Due to problems with some JVMs that dont return a meaningful
	@echo version number, we have had to make the choice of what jdbc
	@echo version is built as a compile time option.
	@echo
	@echo If you are using JDK1.1.x, you will need the JDBC1.2 driver.
	@echo To compile, type:
	@echo "  $(MAKE) jdbc1"
	@echo
	@echo "If you are using JDK1.2 (aka Java2) you need the JDBC2."
	@echo To compile, type:
	@echo "  $(MAKE) jdbc2"
	@echo ------------------------------------------------------------

msg:	
	@echo ------------------------------------------------------------
	@echo The JDBC driver has now been built. To make it available to
	@echo other applications, copy the postgresql.jar file to a public
	@echo "place (under unix this could be /usr/local/lib) and add it"
	@echo to the class path.
	@echo
	@echo Then either add -Djdbc.drivers=postgresql.Driver to the
	@echo commandline when running your application, or edit the
	@echo "properties file for your application (~/.hotjava/properties"
	@echo "under unix for HotJava), and add a line containing"
	@echo jdbc.drivers=postgresql.Driver
	@echo
	@echo More details are in the README file and in the main postgresql
	@echo documentation.
	@echo
	@echo ------------------------------------------------------------
	@echo To build the examples, type:
	@echo "  $(MAKE) examples"
	@echo
	@echo "To build the CORBA example (requires Java2):"
	@echo "  $(MAKE) corba"
	@echo ------------------------------------------------------------
	@echo

dep depend:

# This rule builds the javadoc documentation
doc:
	export CLASSPATH=.;\
		$(JAVADOC) -public \
			postgresql \
			postgresql.fastpath \
			postgresql.largeobject

# These classes form the driver. These, and only these are placed into
# the jar file.
OBJ_COMMON=	postgresql/Connection.class \
		postgresql/Driver.class \
		postgresql/Field.class \
		postgresql/PG_Stream.class \
		postgresql/ResultSet.class \
		postgresql/errors.properties \
		postgresql/errors_fr.properties \
		postgresql/fastpath/Fastpath.class \
		postgresql/fastpath/FastpathArg.class \
		postgresql/geometric/PGbox.class \
		postgresql/geometric/PGcircle.class \
		postgresql/geometric/PGline.class \
		postgresql/geometric/PGlseg.class \
		postgresql/geometric/PGpath.class \
		postgresql/geometric/PGpoint.class \
		postgresql/geometric/PGpolygon.class \
		postgresql/largeobject/LargeObject.class \
		postgresql/largeobject/LargeObjectManager.class \
		postgresql/util/PGmoney.class \
		postgresql/util/PGobject.class \
		postgresql/util/PGtokenizer.class \
		postgresql/util/PSQLException.class \
		postgresql/util/Serialize.class \
		postgresql/util/UnixCrypt.class

# These files are unique to the JDBC 1 (JDK 1.1) driver
OBJ_JDBC1=	postgresql/jdbc1/CallableStatement.class \
		postgresql/jdbc1/Connection.class \
		postgresql/jdbc1/DatabaseMetaData.class \
		postgresql/jdbc1/PreparedStatement.class \
		postgresql/jdbc1/ResultSet.class \
		postgresql/jdbc1/ResultSetMetaData.class \
		postgresql/jdbc1/Statement.class

# These files are unique to the JDBC 2 (JDK 2 nee 1.2) driver
OBJ_JDBC2=	postgresql/jdbc2/ResultSet.class \
		postgresql/jdbc2/PreparedStatement.class \
		postgresql/jdbc2/CallableStatement.class \
		postgresql/jdbc2/Connection.class \
		postgresql/jdbc2/DatabaseMetaData.class \
		postgresql/jdbc2/ResultSetMetaData.class \
		postgresql/jdbc2/Statement.class

# This rule builds the JDBC1 compliant driver
jdbc1:
	(echo "package postgresql;" ;\
	 echo "public class DriverClass {" ;\
	 echo "public static String connectClass=\"postgresql.jdbc1.Connection\";" ;\
	 echo "}" \
	) >postgresql/DriverClass.java
	@$(MAKE) jdbc1real

jdbc1real: postgresql/DriverClass.class \
	$(OBJ_COMMON) $(OBJ_JDBC1) postgresql.jar msg

# This rule builds the JDBC2 compliant driver
jdbc2:	
	(echo "package postgresql;" ;\
	 echo "public class DriverClass {" ;\
	 echo "public static String connectClass=\"postgresql.jdbc2.Connection\";" ;\
	 echo "}" \
	) >postgresql/DriverClass.java
	@$(MAKE) jdbc2real

jdbc2real: postgresql/DriverClass.class \
	$(OBJ_COMMON) $(OBJ_JDBC2) postgresql.jar msg

# If you have problems with this rule, replace the $( ) with ` ` as some
# shells (mainly sh under Solaris) doesn't recognise $( )
#
# Note:	This works by storing all compiled classes under the postgresql
#	directory. We use this later for compiling the dual-mode driver.
#
postgresql.jar: $(OBJ) $(OBJ_COMMON)
	$(JAR) -c0f $@ `$(FIND) postgresql -name "*.class" -print` \
		$(wildcard postgresql/*.properties)

# This rule removes any temporary and compiled files from the source tree.
clean:
	$(FIND) . -name "*~" -exec $(RM) {} \;
	$(FIND) . -name "*.class" -exec $(RM) {} \;
	$(FIND) . -name "*.html" -exec $(RM) {} \;
	-$(RM) -rf stock example/corba/stock.built
	-$(RM) postgresql.jar
	-$(RM) -rf Package-postgresql *output

#######################################################################
# This helps make workout what classes are from what source files
#
# Java is unlike C in that one source file can generate several
# _Different_ file names
#
postgresql/Connection.class:		postgresql/Connection.java
postgresql/DatabaseMetaData.class:	postgresql/DatabaseMetaData.java
postgresql/Driver.class:		postgresql/Driver.java
postgresql/Field.class:			postgresql/Field.java
postgresql/PG_Stream.class:		postgresql/PG_Stream.java
postgresql/PreparedStatement.class:	postgresql/PreparedStatement.java
postgresql/ResultSet.class:		postgresql/ResultSet.java
postgresql/ResultSetMetaData.class:	postgresql/ResultSetMetaData.java
postgresql/Statement.class:		postgresql/Statement.java
postgresql/fastpath/Fastpath.class:	postgresql/fastpath/Fastpath.java
postgresql/fastpath/FastpathArg.class:	postgresql/fastpath/FastpathArg.java
postgresql/geometric/PGbox.class:	postgresql/geometric/PGbox.java
postgresql/geometric/PGcircle.class:	postgresql/geometric/PGcircle.java
postgresql/geometric/PGlseg.class:	postgresql/geometric/PGlseg.java
postgresql/geometric/PGpath.class:	postgresql/geometric/PGpath.java
postgresql/geometric/PGpoint.class:	postgresql/geometric/PGpoint.java
postgresql/geometric/PGpolygon.class:	postgresql/geometric/PGpolygon.java
postgresql/largeobject/LargeObject.class: postgresql/largeobject/LargeObject.java
postgresql/largeobject/LargeObjectManager.class: postgresql/largeobject/LargeObjectManager.java
postgresql/util/PGmoney.class:		postgresql/util/PGmoney.java
postgresql/util/PGobject.class:		postgresql/util/PGobject.java
postgresql/util/PGtokenizer.class:	postgresql/util/PGtokenizer.java
postgresql/util/Serialize.class:	postgresql/util/Serialize.java
postgresql/util/UnixCrypt.class:	postgresql/util/UnixCrypt.java

#######################################################################
# These classes are in the example directory, and form the examples
EX=	example/basic.class \
	example/blobtest.class \
	example/datestyle.class \
	example/psql.class \
	example/ImageViewer.class \
	example/metadata.class \
	example/threadsafe.class
#	example/Objects.class

# This rule builds the examples
examples:	postgresql.jar $(EX)
	@echo ------------------------------------------------------------
	@echo The examples have been built.
	@echo
	@echo For instructions on how to use them, simply run them. For example:
	@echo
	@echo "  java example.blobtest"
	@echo
	@echo This would display instructions on how to run the example.
	@echo ------------------------------------------------------------
	@echo Available examples:
	@echo
	@echo "  example.basic        Basic JDBC useage"
	@echo "  example.blobtest     Binary Large Object tests"
	@echo "  example.datestyle    Shows how datestyles are handled"
	@echo "  example.ImageViewer  Example application storing images"
	@echo "  example.psql         Simple java implementation of psql"
	@echo "  example.Objects      Demonstrates Object Serialisation"
	@echo " "
	@echo These are not really examples, but tests various parts of the driver
	@echo "  example.metadata     Tests various metadata methods"
	@echo "  example.threadsafe   Tests the driver's thread safety"
	@echo ------------------------------------------------------------
	@echo

example/basic.class:			example/basic.java
example/blobtest.class:			example/blobtest.java
example/datestyle.class:		example/datestyle.java
example/psql.class:			example/psql.java
example/ImageViewer.class:		example/ImageViewer.java
example/threadsafe.class:		example/threadsafe.java
example/metadata.class:			example/metadata.java

#######################################################################
#
# CORBA		This extensive example shows how to integrate PostgreSQL
#		JDBC & CORBA.

CORBASRC = $(wildcard example/corba/*.java)
CORBAOBJ = $(subst .java,.class,$(CORBASRC))

corba: jdbc2 example/corba/stock.built $(CORBAOBJ)
	@echo -------------------------------------------------------
	@echo The corba example has been built. Before running, you
	@echo will need to read the example/corba/readme file on how
	@echo to run the example.
	@echo

#
# This compiles our idl file and the stubs
#
# Note: The idl file is in example/corba, but it builds a directory under
# the current one. For safety, we delete that directory before running
# idltojava
#
example/corba/stock.built: example/corba/stock.idl
	-rm -rf stock
	$(IDL2JAVA) $<
	$(JAVAC) stock/*.java
	$(TOUCH) $@

# tip: we cant use $(wildcard stock/*.java) in the above rule as a race
#      condition occurs, where javac is passed no arguments
#######################################################################
