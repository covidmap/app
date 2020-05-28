
#
# COVIDMAP: Makefile
#

VERSION ?= v1d

ARGS ?=
CI ?= no
APP ?= //:app
ACTION ?= replace
API_TARGET ?= //src:covidmap_proto_pkg
API_PACKAGE ?= dist/bin/src/covidmap_proto_pkg.tar
IMAGE_TARGET ?= //src:CovidMapServer-image
TARGETS ?= $(APP)
VERBOSE ?= no
QUIET ?= yes
CACHE ?= no
STRICT ?= no
COVERAGE ?= no
BASE_ARGS ?= --define=covidmap_release_tag=$(VERSION)
TEST_ARGS ?= $(BASE_ARGS)
BAZELISK_ARGS ?=
CACHE_KEY ?= CovidMap
RELEASE ?= no

PROJECT ?= bloom-sandbox
RBE_INSTANCE ?= default_instance
IMAGE_PROJECT ?= covid-impact-map

COVERABLE ?= //javatests:suite
COVERAGE_REPORT ?= $(REPORTS)/coverage
COVERAGE_ARGS ?= --function-coverage \
                 --branch-coverage \
                 --highlight \
                 --demangle-cpp \
                 --show-details \
                 --title "$(PROJECT_NAME)" \
                 --precision 2 \
                 --legend \
                 --rc genhtml_med_limit=60 \
                 --rc genhtml_hi_limit=90

BAZEL_BUILD_FLAGS ?= --protocopt=--include_imports --protocopt=--include_source_info
TEST_ARGS_WITH_COVERAGE ?= --combined_report=lcov --nocache_test_results

# Flag: `COVERAGE`
ifeq ($(COVERAGE),yes)
TEST_COMMAND ?= coverage
TEST_ARGS += $(TEST_ARGS_WITH_COVERAGE)
else
TEST_COMMAND ?= test
endif

# Flag: `RELEASE`
ifeq ($(RELEASE),yes)
ARGS += --compilation_mode=opt $(BAZEL_BUILD_FLAGS)
else
ARGS += --compilation_mode=dbg $(BAZEL_BUILD_FLAGS)
endif

# Flag: `CI`
ifeq ($(CI),yes)
TAG += --config=ci
_DEFAULT_JAVA_HOME = $(shell echo $$JAVA_HOME_12_X64)
BASE_ARGS += --define=ZULUBASE=$(_DEFAULT_JAVA_HOME) --define=jdk=zulu
BAZELISK ?= /bin/bazelisk
GENHTML ?= /bin/genhtml
else
TAG += --config=dev
IBAZEL ?= $(shell which ibazel)
BAZELISK ?= $(shell which bazelisk)
GENHTML ?= $(shell which genhtml)
endif

GCLOUD ?= $(shell which gcloud)

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
BASE_ARGS += --sandbox_debug --test_output=errors
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
else
BASE_ARGS += --test_output=errors
endif
endif


all: build


run:  ## Run the app locally.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(BASE_ARGS) $(ARGS) -- $(APP)

build:  ## Build the app.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(BASE_ARGS) $(ARGS) -- $(TARGETS)

dev:  ## Build the app, start it up, and auto-reload with changes.
	$(_RULE)$(IBAZEL) run $(ARGS) $(APP)

gateway:  ## Build and push the gateway (Envoy) container image.
	$(_RULE)docker build -t us.gcr.io/covid-impact-map/gateway:$(VERSION) src/config/gateway && \
		docker push us.gcr.io/covid-impact-map/gateway:$(VERSION)

image:  ## Build a container image for the app, locally.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(BASE_ARGS) $(ARGS) -- $(IMAGE_TARGET)

push:  ## Build and publish a container image for the app, tagged with the current source tree hash.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(BASE_ARGS) $(ARGS) -- $(IMAGE_TARGET)-push

deploy:  ## Deploy the app to production.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(BASE_ARGS) $(ARGS) -- //src/config:app.$(ACTION)

test:  ## Run any testsuites.
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) $(TEST_COMMAND) $(TEST_ARGS) $(ARGS) -- //javatests:suite

api:  ## Build and deploy the API service endpoint.
	@echo "Building API binary..."
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) build $(BASE_ARGS) $(ARGS) -- $(API_TARGET)
	@echo "Extracting API info..."
	$(_RULE)rm -f src/config/api/covidmap.pb
	$(_RULE)tar -C src/config/api $(POSIX_FLAGS) -xzf $(API_PACKAGE) && mv src/config/api/covidmap_proto-descriptor-set.proto.bin src/config/api/covidmap.pb
	@echo "Deploying API configuration..."
	$(_RULE)$(GCLOUD) --project=$(IMAGE_PROJECT) endpoints services deploy src/config/api/covidmap.pb src/config/api/covidmap.yml

update:  ## Update Java and Git submodule dependencies via Bazel pins.
	$(_RULE)git submodule update --remote ui
	$(_RULE)$(BAZELISK) $(BAZELISK_ARGS) run $(BASE_ARGS) $(ARGS) -- @unpinned_jdeps//:pin

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


.PHONY: all build run help clean distclean forceclean test report-coverage report-tests update

