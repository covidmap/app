
#
# COVIDMAP: Makefile
#

ARGS ?= --config=dev
CI ?= no
APP ?= //:app
TARGETS ?= $(APP)
BAZELISK ?= $(shell which bazelisk)
VERBOSE ?= no
QUIET ?= yes
CACHE ?= no
STRICT ?= no
BASE_ARGS ?=
BAZELISK_ARGS ?=
CACHE_KEY ?= CovidMap

PROJECT ?= bloom-sandbox
RBE_INSTANCE ?= default_instance
IMAGE_PROJECT ?= covid-impact-map

# Flag: `STRICT`.
ifeq ($(STRICT),yes)
BAZELISK_ARGS += --strict
endif

# Flag: `CACHE`.
ifeq ($(CACHE),yes)
BASE_ARGS += --remote_cache=grpcs://remotebuildexecution.googleapis.com \
	     --google_default_credentials=true \
             --remote_instance_name=projects/$(PROJECT)/instances/$(RBE_INSTANCE) \
	     --host_platform_remote_properties_override='properties:{name:"cache-silo-key" value:"$(CACHE_KEY)"}'
endif

# Flag: `DEBUG`
ifeq ($(DEBUG),yes)
VERBOSE = yes
QUIET = no
BASE_ARGS += --sandbox_debug
endif

# Flag: `VERBOSE`
ifeq ($(VERBOSE),yes)
BASE_ARGS += -s --verbose_failures
POSIX_FLAGS += -v
_RULE =
else

# Flag: `QUIET`
ifeq ($(QUIET),yes)
_RULE = @
endif
endif


all: build


run:  ## Run the app locally.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(BASE_ARGS) $(ARGS) -- $(APP)

build:  ## Build the app.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(BASE_ARGS) $(ARGS) -- $(TARGETS)

test:  ## Run any testsuites.
	@echo "No tests yet."

report-coverage:
	@echo "No coverage reporting yet."

report-tests:
	@echo "No test reporting yet."

help:  ## Show this help text.
	$(info COVID Impact Map:)
	@grep -E '^[a-z1-9A-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

clean:  ## Clean ephemeral build targets.
	$(info Cleaning targets...)
	$(_RULE)$(RM) -f $(POSIX_FLAGS) bazel-out release/*
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean

distclean: clean  ## Clean ephemeral targets and dependencies.
	$(info Cleaning dependencies...)
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) clean --expunge

forceclean: distclean  ## DANGEROUS: Wipe all local changes and force-reset codebase.
	$(info Force-resetting codebase...)
	@git reset --hard
	@git clean -xdf


.PHONY: all build run help clean distclean forceclean test report-coverage report-tests

