

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
    "covidmap_ui": {
        "type": "github",
        "repo": "covidmap/ui",
        "target": "a8627a9d2293569273efc08316464c1aa2e9e6cc",
        "overlay": "covidmap_ui.bzl",
        "seal": "018f36145940761fe987c3360f090010e8a9fa2c0884a91c6d3d040f3487f752"},
}



def _install_dependencies():

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS)

install_dependencies = _install_dependencies
