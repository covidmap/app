
workspace(
  name = "covidmap",
  managed_directories = {"@npm": ["node_modules"]})

load(
    "@bazel_tools//tools/build_defs/repo:http.bzl",
    "http_archive",
)


LOCAL = False
GUST_VERSION = "2e9281cc0d4e7bf90d3adedc12c2d57b3da9f46e"
GUST_FINGERPRINT = "bba9f28e273952fac9fd2110fe6a388a4ecb53490b4a378fcab83b197d9ce546"

(local_repository(
    name = "gust",
    path = "/workspace/GUST",
) if LOCAL else http_archive(
    name = "gust",
    urls = ["https://github.com/sgammon/gust/archive/%s.tar.gz" % GUST_VERSION],
    strip_prefix = "GUST-%s" % GUST_VERSION,
    sha256 = GUST_FINGERPRINT,
))


load("@gust//defs:build.bzl", "install_dependencies")
load("@//config:build.bzl", app_dependencies = "install_dependencies")
load("@gust//defs:config.bzl", "CHROMIUM", "FIREFOX", "SAUCE", "GRAALVM_VERSION", "GRAALVM_JDK_VERSION", "K8S_VERSION")
install_dependencies(LOCAL)
app_dependencies(LOCAL)

load("@gust//defs:workspace.bzl", "setup_workspace")
setup_workspace()

#
# Apple Platforms
#

load(
    "@build_bazel_rules_swift//swift:repositories.bzl",
    "swift_rules_dependencies",
)

swift_rules_dependencies()

load(
    "@build_bazel_apple_support//lib:repositories.bzl",
    "apple_support_dependencies",
)

apple_support_dependencies()


#
# Protobuf
#

load(
    "@com_google_protobuf//:protobuf_deps.bzl",
    "protobuf_deps",
)

protobuf_deps()


#
# Extensions
#

## NodeJS
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "build_bazel_rules_nodejs",
    sha256 = "2eca5b934dee47b5ff304f502ae187c40ec4e33e12bcbce872a2eeb786e23269",
    urls = ["https://github.com/bazelbuild/rules_nodejs/releases/download/1.4.1/rules_nodejs-1.4.1.tar.gz"],
)

load("@build_bazel_rules_nodejs//:index.bzl",
     "node_repositories",
     "yarn_install")

node_repositories(
    package_json = ["//:package.json"],
    node_version = "10.13.0",
    yarn_version = "1.12.1")

yarn_install(
    name = "npm",
    package_json = "//:package.json",
    yarn_lock = "//:yarn.lock")

load("@npm//:install_bazel_dependencies.bzl",
     "install_bazel_dependencies")

install_bazel_dependencies()

# Setup TypeScript toolchain
load("@npm_bazel_typescript//:index.bzl", "ts_setup_workspace")
ts_setup_workspace()

## SASS
load("@io_bazel_rules_sass//:package.bzl", "rules_sass_dependencies")
rules_sass_dependencies()

load("@io_bazel_rules_sass//:defs.bzl", "sass_repositories")
sass_repositories()

## Kotlin
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
register_toolchains("@gust//defs/toolchain/kt:kotlin_toolchain")

## GraalVM
load("@rules_graal//graal:graal_bindist.bzl", "graal_bindist_repository")

graal_bindist_repository(
  name = "graal",
  version = GRAALVM_VERSION,
  java_version = GRAALVM_JDK_VERSION,
)

## Java Repos/Deps
load("//config:jdeps.bzl", java_setup = "java_repositories")
java_setup()

load("@maven//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

load("@jdeps//:defs.bzl", app_maven_install = "pinned_maven_install")
app_maven_install()

## Web Testing
load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")
web_test_repositories()

load("@io_bazel_rules_webtesting//web:go_repositories.bzl", "go_repositories", "go_internal_repositories")
go_repositories()
go_internal_repositories()

load("@io_bazel_rules_webtesting//web:java_repositories.bzl", "java_repositories")
java_repositories()

load("@io_bazel_rules_webtesting//web:py_repositories.bzl", "py_repositories")
py_repositories()

load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.2.bzl", "browser_repositories")
browser_repositories(chromium=CHROMIUM, firefox=FIREFOX, sauce=SAUCE)

## Docker
load("@io_bazel_rules_docker//repositories:repositories.bzl", container_repositories = "repositories")
load("@io_bazel_rules_docker//java:image.bzl", _java_image_repos = "repositories")
load("@io_bazel_rules_docker//go:image.bzl", _go_image_repos = "repositories")
load("@io_bazel_rules_docker//nodejs:image.bzl", _nodejs_image_repos = "repositories")
load("@io_bazel_rules_docker//python:image.bzl", _py_image_repos = "repositories")

load("@io_bazel_rules_docker//repositories:deps.bzl",
     container_deps = "deps")

container_repositories()
container_deps()
_go_image_repos()
_py_image_repos()
_java_image_repos()
_nodejs_image_repos()

## JS Interop
load("@com_google_jsinterop_generator//build_defs:rules.bzl", "setup_jsinterop_generator_workspace")
load("@com_google_jsinterop_generator//build_defs:repository.bzl", "load_jsinterop_generator_repo_deps")
load_jsinterop_generator_repo_deps()
setup_jsinterop_generator_workspace()

## API Codegen
load("@com_google_api_codegen//rules_gapic/java:java_gapic_repositories.bzl", "java_gapic_repositories")
java_gapic_repositories()

load("@com_google_api//:repository_rules.bzl", "switched_rules_by_language")
switched_rules_by_language(
    name = "com_google_googleapis_imports",
    grpc = True)

## Karma Setup
load("@npm_bazel_karma//:package.bzl", "npm_bazel_karma_dependencies")
npm_bazel_karma_dependencies()

## Web Testing Setup
load("@io_bazel_rules_webtesting//web:repositories.bzl", "web_test_repositories")
 
web_test_repositories()
 
load("@io_bazel_rules_webtesting//web/versioned:browsers-0.3.2.bzl", "browser_repositories")
 
browser_repositories(
    chromium = True,
    firefox = True,
)

## Toolchains (RBE)
load("@bazel_toolchains//rules:rbe_repo.bzl", "rbe_autoconfig")
rbe_autoconfig(name = "rbe_default")

## Stardoc
load("@io_bazel_stardoc//:setup.bzl", "stardoc_repositories")
stardoc_repositories()

## Python
load("@rules_python//python:repositories.bzl", "py_repositories")
py_repositories()

load("@rules_python//python:pip.bzl", "pip_repositories")
pip_repositories()

load("@rules_python//python:pip.bzl", pip_import = "pip3_import")

pip_import(
    name = "protobuf_py",
    requirements = "@build_stack_rules_proto//python/requirements:protobuf.txt")

load("@protobuf_py//:requirements.bzl", proto_pip_install = "pip_install")
proto_pip_install()

pip_import(
    name = "py",
    requirements = "@gust//defs/toolchain/python:requirements_base.txt")

pip_import(
    name = "werkzeug",
    requirements = "@gust//defs/toolchain/python:requirements_werkzeug.txt")

pip_import(
    name = "grpc_python_dependencies",
    requirements = "@com_github_grpc_grpc//:requirements.bazel.txt")

load("@grpc_python_dependencies//:requirements.bzl", grpc_pip_install="pip_install")
grpc_pip_install()

load("@gust//defs/toolchain/python:repos.bzl", "gust_python_repositories")
gust_python_repositories()

load("@build_stack_rules_proto//python:deps.bzl", "python_proto_compile")
python_proto_compile()

## Swift
load("@build_stack_rules_proto//swift:deps.bzl", "swift_proto_library")
swift_proto_library()

## gRPC Core
load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", "grpc_deps", "grpc_test_only_deps")
grpc_deps()
grpc_test_only_deps()

load("@com_github_grpc_grpc//bazel:grpc_extra_deps.bzl", "grpc_extra_deps")
grpc_extra_deps()

## gRPC Java
load("@io_grpc_java//:repositories.bzl", "grpc_java_repositories")
grpc_java_repositories()

load("@com_google_protobuf//:protobuf_deps.bzl", "protobuf_deps")
protobuf_deps()

load("@io_bazel_rules_k8s//k8s:k8s_go_deps.bzl", k8s_go_deps = "deps")
k8s_go_deps()

## Java Containers
load("@io_bazel_rules_docker//container:container.bzl", "container_pull")

container_pull(
    name = "java_base",
    registry = "gcr.io",
    repository = "distroless/java",
    digest = "sha256:0ce06c40e99e0dce26bdbcec30afe7a890a57bbd250777bd31ff2d1b798c7809",
)

container_pull(
    name = "native_base",
    registry = "us.gcr.io",
    repository = "elide-tools/base/alpine",
    digest = "sha256:decbf1b8ba41c556941f2fbd82811822f7b9622cbd3a17d5d4041cb5438bae2d",
)

container_pull(
    name = "node_base",
    registry = "us.gcr.io",
    repository = "elide-tools/base/node",
    digest = "sha256:76b64868e73d27361e294fd346b72aa6c50ad4e669bd9c2684fbdda7e839ea39",
)

container_pull(
    name = "envoy_base",
    registry = "index.docker.io",
    repository = "envoyproxy/envoy-alpine",
    digest = "sha256:19f3b361450e31f68b46f891b0c8726041739f44ab9b90aecbca5f426c0d2eaf",
)

## K8S Setup
load("@io_bazel_rules_k8s//toolchains/kubectl:kubectl_configure.bzl", "kubectl_configure")
kubectl_configure(
    name="k8s_config",
    build_srcs = True,
    k8s_commit = "v%s" % K8S_VERSION,
    k8s_prefix = "kubernetes-%s" % K8S_VERSION,
    k8s_sha256 = "e091944229641c5b2b2a6ac57767802548b50830cf6710bc676e851bd8233f74",
    k8s_repo_tools_commit = "df02ded38f9506e5bbcbf21702034b4fef815f2f",
    k8s_repo_tools_prefix = "repo-infra-df02ded38f9506e5bbcbf21702034b4fef815f2f",
    k8s_repo_tools_sha = "4a8384320fba401cbf21fef177aa113ed8fe35952ace98e00b796cac87ae7868",
)

load("@io_bazel_rules_k8s//k8s:k8s.bzl", "k8s_repositories")
k8s_repositories()

load("@io_bazel_rules_k8s//k8s:k8s.bzl", "k8s_defaults")
k8s_defaults(
  name = "k9",
  kind = "deployment",
  cluster = "$(cluster)",
)

## Brotli Setup
load("@org_brotli//java:repositories.bzl", "load_brotli_repositories")
load_brotli_repositories()

