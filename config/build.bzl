

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
        "target": "9b9e1f2160ed82fedc9fcf76b057188f58c9c580",
        "overlay": "covidmap_ui.bzl",
        "seal": "a58e45b08d2459f792dd75af290ba72bd75794088414584786f08debf70528bc"},
}



def _install_dependencies():

    """ Install all dependencies into the current WORKSPACE. """

    dependencies(DEPS)

install_dependencies = _install_dependencies
