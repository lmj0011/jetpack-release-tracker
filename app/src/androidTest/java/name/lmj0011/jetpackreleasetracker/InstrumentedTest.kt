package name.lmj0011.jetpackreleasetracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXLibraryDataset
import name.lmj0011.jetpackreleasetracker.helpers.AndroidXReleasePuller

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class InstrumentedTest {
//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("name.lmj0011.bottomnavtinkering", appContext.packageName)
//    }

    @Test
    fun androidXReleasePullerTest() {
        // NOTE some of these test cases may become obsolete over time as new versions are released
        val arp = AndroidXReleasePuller()

        // Parse a library that has both STABLE and UNSTABLE versions

        var lib = AndroidXLibraryDataset.data.find {
            it.packageName == "androidx.activity"
        }!!
        var key = "${lib.packageName}:${lib.artifactNames[0]}"
        var map = arp.parseFeed(lib)!![key]!!
        assertEquals("androidx.activity:activity", key)
        assertEquals("androidx.activity:activity:1.1.0", "${key}:${map["latestStableVersion"]}")
        assertEquals("androidx.activity:activity:1.2.0-alpha04", "${key}:${map["latestVersion"]}")

        key = "${lib.packageName}:${lib.artifactNames[1]}"
        assertEquals("androidx.activity:activity-ktx", key)
        assertEquals("androidx.activity:activity-ktx:1.1.0", "${key}:${map["latestStableVersion"]}")
        assertEquals("androidx.activity:activity-ktx:1.2.0-alpha04", "${key}:${map["latestVersion"]}")

        // Parse a library that only has UNSTABLE versions
        lib = AndroidXLibraryDataset.data.find {
            it.packageName == "androidx.ads"
        }!!
        key = "${lib.packageName}:${lib.artifactNames[0]}"
        map = arp.parseFeed(lib)!![key]!!
        assertEquals("androidx.ads:ads-identifier", key)
        assertEquals("androidx.ads:ads-identifier:1.0.0-alpha04", "${key}:${map["latestVersion"]}")
        assertEquals(null, map["latestStableVersion"])

        key = "${lib.packageName}:${lib.artifactNames[1]}"
        assertEquals("androidx.ads:ads-identifier-common", key)
        assertEquals("androidx.ads:ads-identifier-common:1.0.0-alpha04", "${key}:${map["latestVersion"]}")

        key = "${lib.packageName}:${lib.artifactNames[2]}"
        assertEquals("androidx.ads:ads-identifier-provider", key)
        assertEquals("androidx.ads:ads-identifier-provider:1.0.0-alpha04", "${key}:${map["latestVersion"]}")

        // TODO Parse a library that only has STABLE versions

        // TODO  test that AndroidLibraryDataset up to date with https://dl.google.com/dl/android/maven2/master-index.xml

        // TODO  test that AndroidLibraryDataset "artifacts" are up to date with https://dl.google.com/dl/android/maven2/master-index.xml

        // TODO test that when a newer version is fetch that an  AndroidXArtifactUpdate is created

    }
}
