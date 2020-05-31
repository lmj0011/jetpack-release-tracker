package name.lmj0011.jetpackreleasetracker.helpers

data class AndroidXLibrary (
    var groupIndexUrl: String,

    var releasePageUrl: String,

    var packageName: String,

    var artifactNames: List<String>
)