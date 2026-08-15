package net.thunderbird.core.preference.interaction

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.StorageUpdater

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInteractionSettingsPreferenceManagerTest {
    private val logger = TestLogger()

    @Test
    fun `missing dual screen key binding should default to disabled`() = runTest {
        val testSubject = createTestSubject(TestInteractionPreferenceStorage())

        assertThat(testSubject.getConfig().dualScreenKeyBinding)
            .isEqualTo(INTERACTION_SETTINGS_DEFAULT_DUAL_SCREEN_KEY_BINDING)
    }

    @Test
    fun `saved dual screen key binding should be restored by a new manager`() = runTest {
        val storage = TestInteractionPreferenceStorage()
        val testSubject = createTestSubject(storage)
        val binding = DualScreenKeyBinding(
            keyCode = 139,
            action = DualScreenKeyAction.TOGGLE_MODE,
        )

        testSubject.save(testSubject.getConfig().copy(dualScreenKeyBinding = binding))

        val restoredSubject = createTestSubject(storage)
        assertThat(restoredSubject.getConfig().dualScreenKeyBinding).isEqualTo(binding)
    }

    @Test
    fun `invalid stored action should disable binding and negative key code should be sanitized`() = runTest {
        val storage = TestInteractionPreferenceStorage(
            mapOf(
                KEY_DUAL_SCREEN_KEY_CODE to "-9",
                KEY_DUAL_SCREEN_KEY_ACTION to "DELETE_MESSAGE",
            ),
        )

        val testSubject = createTestSubject(storage)

        assertThat(testSubject.getConfig().dualScreenKeyBinding)
            .isEqualTo(INTERACTION_SETTINGS_DEFAULT_DUAL_SCREEN_KEY_BINDING)
    }

    private fun createTestSubject(
        storage: TestInteractionPreferenceStorage,
    ): DefaultInteractionSettingsPreferenceManager {
        return DefaultInteractionSettingsPreferenceManager(
            logger = logger,
            storagePersister = storage,
            storageEditor = storage,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}

private class TestInteractionPreferenceStorage(
    initialValues: Map<String, String> = emptyMap(),
) : Storage, StoragePersister, StorageEditor {
    private val values = initialValues.toMutableMap()

    override fun loadValues(): Storage = this

    override fun createStorageEditor(storageUpdater: StorageUpdater): StorageEditor = this

    override fun isEmpty(): Boolean = values.isEmpty()

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun getAll(): Map<String, String> = values.toMap()

    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key]?.toBooleanStrictOrNull() ?: defValue

    override fun getInt(key: String, defValue: Int): Int = values[key]?.toIntOrNull() ?: defValue

    override fun getLong(key: String, defValue: Long): Long = values[key]?.toLongOrNull() ?: defValue

    override fun getString(key: String): String = values.getValue(key)

    override fun getStringOrDefault(key: String, defValue: String): String = values[key] ?: defValue

    override fun getStringOrNull(key: String): String? = values[key]

    override fun putBoolean(key: String, value: Boolean): StorageEditor = putString(key, value.toString())

    override fun putInt(key: String, value: Int): StorageEditor = putString(key, value.toString())

    override fun putLong(key: String, value: Long): StorageEditor = putString(key, value.toString())

    override fun putString(key: String, value: String?): StorageEditor = apply {
        if (value == null) values.remove(key) else values[key] = value
    }

    override fun remove(key: String): StorageEditor = apply { values.remove(key) }

    override fun commit(): Boolean = true
}
