

load(
    "@gust//defs:tools.bzl",
    "dependencies",
    "http_archive",
    "git_repository",
)

load(
    "@gust//defs/toolchain:deps.bzl",
    "maven",
)

DEPS = {
    # COVID Map: UI
    "covidmap_ui": {
        "type": "github",
        "repo": "covidmap/ui",
        "local": "./ui",
        "forceLocal": False,  # flip to `True` to build against local submodule
        "target": "5353775fa9b5be60a529c80d3f1067ff414a8a38",
        "overlay": "covidmap_ui.bzl",
        "seal": "b3f852a296ac1296a7da4af5db9a4b7b50725ae2263957078cc8d81eb7743e2f"},

    # MaxMind GeoIP Database
    "maxmind": {
        "type": "archive",
        "overlay": "maxmind.bzl",
        "target": "https://storage.googleapis.com/covid-impact-backend/geoip/20200324.tar.gz",
        "seal": "15dfac42fa233714960e02db5498b3b02a8c32afcb4e5ee2fc4b73011f3a07a3"},

    # Rules: External JVM
    "rules_jvm_external": {
        "type": "github",
        "repo": "bazelbuild/rules_jvm_external",
        "target": "4489ffeaaa9adcad4fa2657c1ca2eeb7b1ad4b84",
        "seal": "6be1e2c4ad81cb00851df5ec7dcc2547cdd5cb555d829c5b665470f3d4d3229b"},
}



def _install_dependencies(local):

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS, local)

install_dependencies = _install_dependencies
