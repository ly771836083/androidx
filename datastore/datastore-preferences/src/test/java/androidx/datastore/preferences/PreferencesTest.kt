/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.datastore.preferences

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(JUnit4::class)
class PreferencesTest {

    @Test
    fun testBoolean() {
        val booleanKey = preferencesKey<Boolean>("boolean_key")

        val prefs = preferencesOf(booleanKey to true)

        assertTrue { booleanKey in prefs }
        assertTrue(prefs[booleanKey]!!)
    }

    @Test
    fun testBooleanNotSet() {
        val booleanKey = preferencesKey<Boolean>("boolean_key")

        assertNull(emptyPreferences()[booleanKey])
    }

    @Test
    fun testFloat() {
        val floatKey = preferencesKey<Float>("float_key")

        val prefs = preferencesOf(floatKey to 1.1f)

        assertTrue { floatKey in prefs }
        assertEquals(1.1f, prefs[floatKey])
    }

    @Test
    fun testFloatNotSet() {
        val floatKey = preferencesKey<Float>("float_key")
        assertNull(emptyPreferences()[floatKey])
    }

    @Test
    fun testInt() {
        val intKey = preferencesKey<Int>("int_key")

        val prefs = preferencesOf(intKey to 1)

        assertTrue { prefs.contains(intKey) }
        assertEquals(1, prefs[intKey])
    }

    @Test
    fun testIntNotSet() {
        val intKey = preferencesKey<Int>("int_key")
        assertNull(emptyPreferences()[intKey])
    }

    @Test
    fun testLong() {
        val longKey = preferencesKey<Long>("long_key")

        val bigLong = 1L shr 50; // 2^50 > Int.MAX_VALUE

        val prefs = preferencesOf(longKey to bigLong)

        assertTrue { prefs.contains(longKey) }
        assertEquals(bigLong, prefs[longKey])
    }

    @Test
    fun testLongNotSet() {
        val longKey = preferencesKey<Long>("long_key")

        assertNull(emptyPreferences()[longKey])
    }

    @Test
    fun testString() {
        val stringKey = preferencesKey<String>("string_key")

        val prefs = preferencesOf(stringKey to "string123")

        assertTrue { prefs.contains(stringKey) }
        assertEquals("string123", prefs[stringKey])
    }

    @Test
    fun testStringNotSet() {
        val stringKey = preferencesKey<String>("string_key")

        assertNull(emptyPreferences()[stringKey])
    }

    @Test
    fun testStringSet() {
        val stringSetKey = preferencesSetKey<String>("string_set_key")

        val prefs = preferencesOf(stringSetKey to setOf("string1", "string2", "string3"))

        assertTrue { prefs.contains(stringSetKey) }
        assertEquals(
            setOf("string1", "string2", "string3"), prefs[stringSetKey]
        )
    }

    @Test
    fun testStringSetNotSet() {
        val stringSetKey = preferencesSetKey<String>("string_set_key")

        assertNull(emptyPreferences()[stringSetKey])
    }

    @Test
    fun testModifyingStringSetDoesntModifyInternalState() {
        val stringSetKey = preferencesSetKey<String>("string_set_key")

        val stringSet = mutableSetOf("1", "2", "3")

        val prefs = preferencesOf(stringSetKey to stringSet)

        stringSet.add("4") // modify the set passed into preferences

        // modify the returned set.
        val returnedSet: Set<String> = prefs[stringSetKey]!!
        val mutableReturnedSet: MutableSet<String> = returnedSet as MutableSet<String>

        assertFailsWith<UnsupportedOperationException> {
            mutableReturnedSet.clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            mutableReturnedSet.add("Original set does not contain this string")
        }

        assertEquals(setOf("1", "2", "3"), prefs[stringSetKey])
    }

    @Test
    @Suppress("UNUSED_VARIABLE")
    fun testWrongTypeThrowsClassCastException() {
        val stringKey = preferencesKey<String>("string_key")
        val intKey = preferencesKey<Int>("string_key") // long key of the same name as stringKey!
        val longKey = preferencesKey<Long>("string_key")

        val prefs = preferencesOf(intKey to 123456)

        assertTrue { prefs.contains(intKey) }
        assertTrue { prefs.contains(stringKey) } // TODO: I don't think we can prevent this

        // Trying to get a long where there is an Int value throws a ClassCastException.
        assertFailsWith<ClassCastException> {
            var unused = prefs[stringKey] // This only throws if it's assigned to a
            // variable
        }

        // Trying to get a Long where there is an Int value throws a ClassCastException.
        assertFailsWith<ClassCastException> {
            var unused = prefs[longKey] // This only throws if it's assigned to a
            // variable
        }
    }

    @Test
    fun testGetAll() {
        val intKey = preferencesKey<Int>("int_key")
        val stringSetKey = preferencesSetKey<String>("string_set_key")

        val prefs = preferencesOf(intKey to 123, stringSetKey to setOf("1", "2", "3"))

        val allPreferences: Map<Preferences.Key<*>, Any> = prefs.asMap()
        assertEquals(2, allPreferences.size)

        assertEquals(123, allPreferences[intKey])
        assertEquals(setOf("1", "2", "3"), (allPreferences[stringSetKey]))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testGetAllCantMutateInternalState() {
        val intKey = preferencesKey<Int>("int_key")
        val stringSetKey = preferencesSetKey<String>("string_set_key")

        val prefs = preferencesOf(intKey to 123, stringSetKey to setOf("1", "2", "3"))

        val mutableAllPreferences = prefs.asMap() as MutableMap
        assertFailsWith<UnsupportedOperationException> {
            mutableAllPreferences[intKey] = 99999
        }
        assertFailsWith<UnsupportedOperationException> {
            (mutableAllPreferences[stringSetKey] as MutableSet<String>).clear()
        }

        assertEquals(123, prefs[intKey])
        assertEquals(setOf("1", "2", "3"), prefs[stringSetKey])
    }

    @Test
    fun testMutablePreferencesClear() {
        val intKey = preferencesKey<Int>("int_key")

        val prefsWithInt = preferencesOf(intKey to 123)

        val emptyPrefs = prefsWithInt.toMutablePreferences().apply { clear() }.toPreferences()

        assertEquals(emptyPreferences(), emptyPrefs)
    }

    @Test
    fun testMutablePreferencesRemove() {
        val intKey = preferencesKey<Int>("int_key")

        val prefsWithInt = preferencesOf(intKey to 123)

        val emptyPrefs =
            prefsWithInt.toMutablePreferences().apply { remove(intKey) }.toPreferences()

        assertEquals(emptyPreferences(), emptyPrefs)

        val emptyPrefs2 = prefsWithInt.toMutablePreferences()
        emptyPrefs2 -= intKey

        assertEquals(emptyPreferences(), emptyPrefs2)
    }

    @Test
    fun testBuilderPublicConstructor() {
        val emptyPrefs = mutablePreferencesOf().toPreferences()

        assertEquals(emptyPreferences(), emptyPrefs)
    }

    @Test
    fun testEqualsDifferentInstances() {
        val intKey1 = preferencesKey<Int>("int_key1")

        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey1 to 123)

        assertEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentKeys() {
        val intKey1 = preferencesKey<Int>("int_key1")
        val intKey2 = preferencesKey<Int>("int_key2")

        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey2 to 123)

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentValues() {
        val intKey1 = preferencesKey<Int>("int_key1")

        val prefs1 = preferencesOf(intKey1 to 123)
        val prefs2 = preferencesOf(intKey1 to 999)

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testNotEqualsDifferentStringSets() {
        val stringSetKey = preferencesSetKey<String>("string_set")

        val prefs1 = preferencesOf(stringSetKey to setOf("1"))
        val prefs2 = preferencesOf(stringSetKey to setOf())

        assertNotEquals(prefs1, prefs2)
    }

    @Test
    fun testCreateUnsupportedKeyType_failsWithIllegalStateException() {
        assertFailsWith<IllegalArgumentException> { preferencesKey<Set<String>>("test") }
        assertFailsWith<IllegalArgumentException> { preferencesKey<Set<*>>("test") }
        assertFailsWith<IllegalArgumentException> { preferencesKey<Double>("test") }
        assertFailsWith<IllegalArgumentException> { preferencesKey<Any>("test") }
    }

    @Test
    fun testCreateUnsupportedSetKeyType_failsWithIllegalStateException() {
        assertFailsWith<IllegalArgumentException> { preferencesSetKey<Set<String>>("test") }
        assertFailsWith<IllegalArgumentException> { preferencesSetKey<Set<*>>("test") }
        assertFailsWith<IllegalArgumentException> { preferencesSetKey<Double>("test") }
        assertFailsWith<IllegalArgumentException> { preferencesSetKey<Any>("test") }
    }

    @Test
    fun testToPreferences_retainsAllKeys() {
        val intKey1 = preferencesKey<Int>("int_key1")
        val intKey2 = preferencesKey<Int>("int_key2")
        val prefs = preferencesOf(intKey1 to 1, intKey2 to 2)
        val toPrefs = prefs.toPreferences()
        assertEquals( 2, toPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)
        val mutableToPrefs = mutablePreferences.toPreferences()
        assertEquals( 2, mutableToPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])
    }

    @Test
    fun testToMutablePreferences_retainsAllKeys() {
        val intKey1 = preferencesKey<Int>("int_key1")
        val intKey2 = preferencesKey<Int>("int_key2")
        val prefs = preferencesOf(intKey1 to 1, intKey2 to 2)
        val toPrefs = prefs.toMutablePreferences()
        assertEquals( 2, toPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)
        val mutableToPrefs = mutablePreferences.toMutablePreferences()
        assertEquals( 2, mutableToPrefs.asMap().size)
        assertEquals(1, prefs[intKey1])
        assertEquals(2, prefs[intKey2])
    }

    @Test
    fun testToMutablePreferences_doesntMutateOriginal() {
        val intKey1 = preferencesKey<Int>("int_key1")
        val intKey2 = preferencesKey<Int>("int_key2")
        val prefs = mutablePreferencesOf(intKey1 to 1, intKey2 to 2)
        val toPrefs = prefs.toMutablePreferences()
        toPrefs[intKey1] = 12903819
        assertEquals(1, prefs[intKey1])

        val mutablePreferences = preferencesOf(intKey1 to 1, intKey2 to 2)
        val mutableToPrefs = mutablePreferences.toMutablePreferences()
        mutableToPrefs[intKey1] = 12903819
        assertEquals(1, prefs[intKey1])
    }
}