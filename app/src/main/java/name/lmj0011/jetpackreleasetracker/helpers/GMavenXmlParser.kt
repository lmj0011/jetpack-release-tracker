package name.lmj0011.jetpackreleasetracker.helpers

import android.util.Xml
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.Semver.SemverType
import com.vdurmont.semver4j.SemverException
import name.lmj0011.jetpackreleasetracker.database.AndroidXArtifact
import org.w3c.dom.Document
import org.w3c.dom.Node
import timber.log.Timber
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class GMavenXmlParser {
    val MAVEN_BASE_URL = "https://dl.google.com/android/maven2/"
    val RELEASE_PAGE_BASE_URL = "https://developer.android.com/jetpack/androidx/releases/"

    val builderFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = builderFactory.newDocumentBuilder()

    fun loadArtifacts(): List<AndroidXArtifact> {
        // root Element
        val metaData = loadMasterIndex().documentElement
        val list = mutableListOf<AndroidXArtifact>()

        for(idx in 0 until metaData.childNodes.length){
            val childNode = metaData.childNodes.item(idx)
            if (childNode.nodeType == Node.ELEMENT_NODE) {
                // We're only concern with gathering Androidx artifacts
                if(childNode.nodeName.substring(0..7) == "androidx") {
                    list.addAll(loadGroup(childNode.nodeName))
                }
            }
        }

        return list
    }

    fun loadMasterIndex(): Document {
        val url = URL("${MAVEN_BASE_URL}master-index.xml")

        val inStream = (url.openConnection() as? HttpURLConnection)?.run {
            readTimeout = 10000
            connectTimeout = 15000
            requestMethod = "GET"
            doInput = true
            // Starts the query
            connect()
            inputStream
        } as InputStream

        return docBuilder.parse(inStream)
    }

    fun loadGroup(groupId: String): List<AndroidXArtifact> {
        val url = URL("${MAVEN_BASE_URL}${groupId.replace('.','/')}/group-index.xml")
        val list = mutableListOf<AndroidXArtifact>()

        val inStream = (url.openConnection() as? HttpURLConnection)?.run {
            readTimeout = 10000
            connectTimeout = 15000
            requestMethod = "GET"
            doInput = true
            // Starts the query
            connect()
            inputStream
        } as InputStream

        val doc = docBuilder.parse(inStream)

        // root Element
        val groupIdElement = doc.documentElement

        for(idx in 0 until groupIdElement.childNodes.length){
            try {
                if (groupIdElement.childNodes.item(idx).nodeType == Node.ELEMENT_NODE) {
                    val latestVerMap = filterLatestVersions(groupIdElement.childNodes.item(idx).attributes.getNamedItem("versions").nodeValue.split(',').toTypedArray())

                    val artifact = AndroidXArtifact(
                        name = "${groupId}:${groupIdElement.childNodes.item(idx).nodeName}",
                        packageName = "${groupIdElement.childNodes.item(idx).nodeName}",
                        releasePageUrl = "$RELEASE_PAGE_BASE_URL${groupId.split(".").drop(1).joinToString(".").replace('.','-')}",
                        latestStableVersion = latestVerMap.getOrDefault("latestStableVersion", ""),
                        latestVersion = latestVerMap.getOrDefault("latestVersion", "")
                    )

                    list.add(artifact)
                    Timber.d("artifact: $artifact")
                }
            } catch(ex: SemverException) {
                val errMsg = ex.message ?: ""

                when {
                    errMsg.contains("Invalid version") -> {
                        Timber.e("Failed parsing versions for ${groupId}:${groupIdElement.childNodes.item(idx).nodeName}")
                    }
                    else -> throw ex
                }

                Timber.e(ex)
            }

        }

        return list
    }

    // returns a Map of the latest version and "Stable" version
    private fun filterLatestVersions(versions: Array<String>): Map<String, String> {
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
}