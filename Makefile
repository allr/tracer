# Enable verbose compilation with "make V=1"
ifdef V
 Q :=
 E := @:
else
 Q := @
 E := @echo
endif

TRACER_JAR=tracer-0.0.1-SNAPSHOT-jar-with-dependencies.jar

# default target does nothing =)
all:
	@echo "No compilation required =)"

clean:

# allow install only if PREFIX is set
ifdef PREFIX
install: real-install
REALPREFIX := $(abspath $(PREFIX))
else
install: error
endif

real-install:
# create the target directory
	$(E) "  MKDIR   $(PREFIX)"
	$(Q)mkdir -p $(REALPREFIX)
# copy script
	$(E) "  CP      tracer.pl"
	$(Q)cp tracer.pl $(REALPREFIX)/tracer.pl
	$(Q)chmod +x $(REALPREFIX)/tracer.pl
# copy default config file (if not already there)
	$(E) "  CP      tracer.conf"
	$(Q)if [ ! -e $(REALPREFIX)/tracer.conf ]; then \
	      cp tracer.conf $(REALPREFIX)/tracer.conf; \
	    fi
# create symlink for backwards compatibility
	$(E) "  LN      tracer.sh"
	$(Q)ln -sf $(REALPREFIX)/tracer.pl $(REALPREFIX)/tracer.sh
# remove old version of traceR
	$(Q)rm -f $(REALPREFIX)/tracer.jar
# copy sample queries (and remove the old ones)
	$(E) "  CP      queries"
	$(Q)mkdir -p $(REALPREFIX)/queries
	$(Q)rm -f $(REALPREFIX)/queries/000_symbols.sql
	$(Q)rm -f $(REALPREFIX)/queries/010_pivots.sql
	$(Q)rm -f $(REALPREFIX)/queries/020_analyses.sql
	$(Q)rm -f $(REALPREFIX)/queries/tutorial_queries.sql
	$(Q)rm -f $(REALPREFIX)/queries/z_final.sql
	$(Q)rm -f $(REALPREFIX)/queries/pivots.sql
	$(Q)cp queries/* $(REALPREFIX)/queries
# copy sample programs
	$(E) "  CP      demos"
	$(Q)mkdir -p $(REALPREFIX)/demos
	$(Q)cp demos/*.* $(REALPREFIX)/demos
# copy sample outputs
	$(E) "  CP      demos/sample_plots"
	$(Q)mkdir -p $(REALPREFIX)/demos/sample_plots
	$(Q)cp demos/sample_plots/*.* $(REALPREFIX)/demos/sample_plots
# copy scripts
	$(E) "  CP      scripts"
	$(Q)mkdir -p $(REALPREFIX)/scripts
	$(Q)cp scripts/* $(REALPREFIX)/scripts
# fix install path in shell scripts
	$(E) "  FIXPATH"
	$(Q)./fixpath.sh $(REALPREFIX)/demos/rundemos.sh $(REALPREFIX)
	$(Q)./fixpath.sh $(REALPREFIX)/scripts/plotall.sh $(REALPREFIX)
# fix permissions for non-fixpath scripts
	$(Q)chmod +x $(REALPREFIX)/scripts/plotcsv.pl

# error message if no PREFIX is specified
error:
	@echo ERROR: No target directory has been specified!
	@echo ""
	@echo Please use \"$(MAKE) install PREFIX=/where/you/want/it/installed\"
	@echo to specify a target directory.
#	@echo ""
#	@echo You can also use \"make help\" to see a list of
#	@echo variables available.

.PHONY : all clean install real-install error
