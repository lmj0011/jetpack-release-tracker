package name.lmj0011.jetpackreleasetracker.helpers

import android.util.Xml
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.Semver.SemverType
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class AndroidXReleasePuller {
    private lateinit var xLibrary: AndroidXLibrary
    private val ns: String? = null

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        val url = URL(urlString)
        return (url.openConnection() as? HttpURLConnection)?.run {
            readTimeout = 10000
            connectTimeout = 15000
            requestMethod = "GET"
            doInput = true
            // Starts the query
            connect()
            inputStream
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): MutableMap<String, Map<String,String>> {
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): MutableMap<String, Map<String,String>> {
        var allArtifactsMap = mutableMapOf<String, Map<String,String>>() //sorry. :(

        parser.require(XmlPullParser.START_TAG, ns, xLibrary.packageName)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the packageName tag
            when (parser.name) {
                in xLibrary.artifactNames -> {
                    val verMap = readArtifact(parser)
                    allArtifactsMap.put("${xLibrary.packageName}:${parser.name}", verMap)
                }
                else -> {
                    skip(parser)
                }
            }
        }

        return allArtifactsMap
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readArtifact(parser: XmlPullParser): Map<String, String> {
        parser.require(XmlPullParser.START_TAG, ns, parser.name)

        var versions = parser.getAttributeValue(null, "versions").split(',').toTypedArray()
        parser.next()
        return sortVersions(versions)
    }

    // skips tags we're not interested in
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    // returns a Map of the latest version and "Stable" version
    private fun sortVersions(versions: Array<String>): Map<String, String> {
        val stableVersions = versions.filter {
            Semver(it, SemverType.LOOSE).isStable
        }.toTypedArray()

        val allVersions = versions.clone()

        stableVersions.sortWith(Comparator { s1, s2 ->
            when {
                Semver(s1, SemverType.LOOSE).isGreaterThan(s2) -> 1
                Semver(s1, SemverType.LOOSE).isEqualTo(s2) -> 0
                else -> -1
            }
        })

        allVersions.sortWith(Comparator { s1, s2 ->
          when {
               Semver(s1, SemverType.LOOSE).isGreaterThan(s2) -> 1
               Semver(s1, SemverType.LOOSE).isEqualTo(s2) -> 0
               else -> -1
          }
        })

        var latestStableVersion = ""
        var latestVersion = ""

        stableVersions.lastOrNull()?.apply {
            latestStableVersion = this
        }

        allVersions.lastOrNull()?.apply {
            latestVersion = this
        }

        return mapOf(
            "latestStableVersion" to latestStableVersion,
            "latestVersion" to latestVersion
        )
    }

    // parse the xml feed
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseFeed(lib: AndroidXLibrary): MutableMap<String, Map<String,String>>? {
        xLibrary = lib

        return downloadUrl(xLibrary.groupIndexUrl)?.use { stream ->
            // Instantiate the parser
            this.parse(stream)
        }
    }
}