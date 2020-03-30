
load(
    "@gust//defs/toolchain/java:repos.bzl",
    "REPOSITORIES",
    "FETCH_SOURCES",
    "STRICT_DEPENDENCIES",
    "gust_java_repositories",
)

load(
    "@rules_jvm_external//:defs.bzl",
    "maven_install",
)

load(
    "@rules_jvm_external//:specs.bzl",
    "maven",
)



def java_repositories():

    """ Prep Java repositories. """

    gust_java_repositories()

    maven_install(
        name = "jdeps",
        repositories = REPOSITORIES,
        fetch_sources = FETCH_SOURCES,
        maven_install_json = "@covidmap//:jdeps_install.json",
        generate_compat_repositories = True,
        strict_visibility = STRICT_DEPENDENCIES,
        artifacts = [
            "ch.hsr:geohash:1.4.0",
            "com.google.code.gson:gson:2.8.6",
            "com.googlecode.libphonenumber:libphonenumber:8.12.0",
            "com.google.cloud:google-cloud-bigquery:1.110.0",
            "com.maxmind.db:maxmind-db:1.3.1",
            "com.maxmind.geoip2:geoip2:2.13.1",
        ],
    )
