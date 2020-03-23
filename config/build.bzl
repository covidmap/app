

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
        "forceLocal": True,
        "local": "/workspace/covidmap/ui",
        "target": "dfb8ac90e7ecadd04c081ebe390375bd99416775",
        "overlay": "covidmap_ui.bzl",
        "seal": "d14b4aeb40691382b505813c4c8adf89a93ac678cc09ae3ba0e0254e9de9ac6e"},
}



def _install_dependencies(local):

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS, local)

install_dependencies = _install_dependencies
