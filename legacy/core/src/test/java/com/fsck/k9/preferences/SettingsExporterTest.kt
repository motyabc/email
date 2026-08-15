package com.fsck.k9.preferences

import app.k9mail.legacy.mailstore.FolderRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.Preferences
import java.io.ByteArrayOutputStream
import org.jdom2.Document
import org.jdom2.input.SAXBuilder
import org.junit.Test
import org.koin.core.component.inject
import org.mockito.kotlin.mock
import org.robolectric.RuntimeEnvironment

class SettingsExporterTest : K9RobolectricTest() {
    private val contentResolver = RuntimeEnvironment.getApplication().contentResolver
    private val preferences: Preferences by inject()
    private val folderSettingsProvider: FolderSettingsProvider by inject()
    private val folderRepository: FolderRepository by inject()
    private val settingsExporter = SettingsExporter(
        contentResolver,
        preferences,
        folderSettingsProvider,
        folderRepository,
        notificationSettingsUpdater = mock(),
        filePrefixProvider = mock(),
    )

    @Test
    fun exportPreferences_producesXML() {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.name).isEqualTo("k9settings")
    }

    @Test
    fun exportPreferences_setsVersionToLatest() {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.getAttributeValue("version")).isEqualTo(Settings.VERSION.toString())
    }

    @Test
    fun exportPreferences_setsFormatTo1() {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.getAttributeValue("format")).isEqualTo("1")
    }

    @Test
    fun exportPreferences_exportsGlobalSettingsWhenRequested() {
        val document = exportPreferences(true, emptySet())

        assertThat(document.rootElement.getChild("global")).isNotNull()
    }

    @Test
    fun exportPreferences_includesSafeDualScreenKeyDefaults() {
        val document = exportPreferences(true, emptySet())
        val values = document.rootElement.getChild("global")
            .getChildren("value")
            .associate { element -> element.getAttributeValue("key") to element.text }

        assertThat(values["dualScreenKeyCode"]).isEqualTo("0")
        assertThat(values["dualScreenKeyAction"]).isEqualTo("DISABLED")
    }

    @Test
    fun exportPreferences_ignoresGlobalSettingsWhenRequested() {
        val document = exportPreferences(false, emptySet())

        assertThat(document.rootElement.getChild("global")).isNull()
    }

    private fun exportPreferences(globalSettings: Boolean, accounts: Set<String>): Document {
        return ByteArrayOutputStream().use { outputStream ->
            settingsExporter.exportPreferences(outputStream, globalSettings, accounts, includePasswords = false)
            parseXml(outputStream.toByteArray())
        }
    }

    private fun parseXml(xml: ByteArray): Document {
        return SAXBuilder().build(xml.inputStream())
    }
}
