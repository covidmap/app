
load(
    "@gust//defs/toolchain:backend.bzl",
    java_library = "jdk_library",
)


exports_files([
    "maxmind.mmdb",
])

java_library(
    name = "maxmind",
    resources = [":maxmind.mmdb"],
)
