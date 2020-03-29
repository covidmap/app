
package(
    default_visibility = ["//visibility:public"],
)

load(
    "@gust//defs/toolchain/ts:tsc.bzl",
    "ts_module",
)

load(
    "@gust//defs/toolchain/style:rules.bzl",
    "style_binary",
    "style_library",
)

load(
    "@npm_bazel_rollup//:index.bzl",
    "rollup_bundle",
)

load(
    "@npm_bazel_terser//:index.bzl",
    "terser_minified",
)

load(
    "@npm_bazel_typescript//:index.bzl",
    "ts_config",
)


#
# Styles
#

style_library(
    name = "app-main",
    srcs = ["css/appMain.css"],
)

style_library(
    name = "global",
    srcs = ["css/global.css"],
)

style_library(
    name = "hospital-map",
    srcs = ["css/hospitalMap.css"],
)

style_library(
    name = "hospital-raw-output",
    srcs = ["css/hospitalRawOutput.css"],
)

style_library(
    name = "loading-cover",
    srcs = ["css/loadingCover.css"],
)

style_library(
    name = "menu-bar",
    srcs = ["css/menuBar.css"],
)

style_library(
    name = "single-hospital",
    srcs = ["css/singleHospitalDetails.css"],
)


style_binary(
    name = "ui",
    optimize = True,
    deps = [
        ":app-main",
        ":global",
        ":hospital-map",
        ":hospital-raw-output",
        ":loading-cover",
        ":menu-bar",
        ":single-hospital",
    ],
)


#
# TypeScript
#

ts_config(
    name = "tsconfig",
    src = "@covidmap//src/config:tsconfig.json",
    deps = [],
)

ts_module(
    name = "ts",
    runtime = "browser",
    tsconfig = ":tsconfig",
    srcs = glob(
        include = ["src/**/*.ts"],
        exclude = ["src/**/test/*.*"],
    ),
    deps = [
        "@npm//@types/node",
        "@npm//crypto-random-string",
        "@npm//eventemitter3",
        "@npm//tsickle",
        "@npm//rxjs",
    ],
)

rollup_bundle(
    name = "bundle.dev",
    config_file = "@covidmap//src/config:rollup.config.js",
    entry_point = ":src/bootstrap/bootstrapper.ts",
    format = "iife",
    sourcemap = "true",
    deps = [
        ":ts",
        "@npm//:node_modules",
        "@npm//rollup-plugin-commonjs",
        "@npm//rollup-plugin-node-resolve",
    ],
)

rollup_bundle(
    name = "bundle.prod",
    config_file = "@covidmap//src/config:rollup.config.prod.js",
    entry_point = ":src/bootstrap/bootstrapper.ts",
    format = "iife",
    sourcemap = "true",
    deps = [
        ":ts",
        "@npm//:node_modules",
        "@npm//rollup-plugin-commonjs",
        "@npm//rollup-plugin-node-resolve",
    ],
)

terser_minified(
    name = "bundle.min",
    src = ":bundle.prod.js",
)

alias(
    name = "ui.js",
    actual = select({
        "@gust//defs/config:release": ":bundle.min.js",
        "//conditions:default": ":bundle.dev.js",
    }),
)

alias(
    name = "ui.js.map",
    actual = select({
        "@gust//defs/config:release": ":bundle.min.js.map",
        "//conditions:default": ":bundle.dev.js.map",
    }),
)
