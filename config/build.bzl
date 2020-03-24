

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
        "local": "/workspace/covidmap/ui",
        "target": "d4c26761b2634f923e5ae0f53acaf0c2f02bf85f",
        "overlay": "covidmap_ui.bzl",
        "seal": "a84471c25a6e5a261214b2f8af90e651c41dba280bea66dcd793ee75ee41b92c"},
}



def _install_dependencies(local):

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS, local)

install_dependencies = _install_dependencies
