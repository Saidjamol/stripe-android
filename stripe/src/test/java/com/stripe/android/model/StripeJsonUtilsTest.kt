package com.stripe.android.model

import com.stripe.android.testharness.JsonTestUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Test class for [StripeJsonUtils].
 */
class StripeJsonUtilsTest {

    @Test
    fun nullIfNullOrEmpty_returnsNullForNull() {
        assertNull(StripeJsonUtils.nullIfNullOrEmpty("null"))
    }

    @Test
    fun nullIfNullOrEmpty_returnsNullForEmpty() {
        assertNull(StripeJsonUtils.nullIfNullOrEmpty(""))
    }

    @Test
    fun nullIfNullOrEmpty_returnsInputIfNotNull() {
        val notANull = "notANull"
        assertEquals(notANull, StripeJsonUtils.nullIfNullOrEmpty(notANull))
    }

    @Test
    @Throws(JSONException::class)
    fun optString_whenFieldPresent_findsAndReturnsField() {
        val jsonObject = JSONObject()
            .put("key", "value")
        assertEquals("value", StripeJsonUtils.optString(jsonObject, "key"))
    }

    @Test
    @Throws(JSONException::class)
    fun optString_whenFieldContainsRawNull_returnsNull() {
        val jsonObject = JSONObject()
            .put("key", "null")
        assertNull(StripeJsonUtils.optString(jsonObject, "key"))
    }

    @Test
    @Throws(JSONException::class)
    fun optString_whenFieldNotPresent_returnsNull() {
        val jsonObject = JSONObject()
            .put("key", "value")
        val ob = StripeJsonUtils.optString(jsonObject, "nokeyshere")
        assertNull(ob)
    }

    @Test
    fun jsonObjectToMap_forNull_returnsNull() {
        assertNull(StripeJsonUtils.jsonObjectToMap(null))
    }

    @Test
    fun jsonArrayToList_forNull_returnsNull() {
        assertNull(StripeJsonUtils.jsonArrayToList(null))
    }

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToMap_forSimpleObjects_returnsExpectedMap() {
        val expectedMap = mapOf(
            "akey" to "avalue",
            "bkey" to "bvalue",
            "boolkey" to true,
            "numkey" to 123
        )

        val mappedObject = StripeJsonUtils.jsonObjectToMap(SIMPLE_JSON_TEST_OBJECT)
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject)
    }

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToStringMap_forSimpleObjects_returnsExpectedMap() {
        val expectedMap = mapOf(
            "akey" to "avalue",
            "bkey" to "bvalue",
            "boolkey" to "true",
            "numkey" to "123"
        )

        val mappedObject = StripeJsonUtils.jsonObjectToStringMap(SIMPLE_JSON_TEST_OBJECT)
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject)
    }

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToMap_forNestedObjects_returnsExpectedMap() {
        val expectedMap = mapOf(
            "top_key" to mapOf(
                "first_inner_key" to mapOf(
                    "innermost_key" to 1000,
                    "second_innermost_key" to "second_inner_value"
                ),
                "second_inner_key" to "just a value"),
            "second_outer_key" to mapOf("another_inner_key" to false)
        )

        val mappedObject = StripeJsonUtils.jsonObjectToMap(NESTED_JSON_TEST_OBJECT)
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject)
    }

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToStringMap_forNestedObjects_returnsExpectedFlatMap() {
        val expectedMap = mapOf(
            "top_key" to
                "{\"first_inner_key\":{\"innermost_key\":1000," +
                "\"second_innermost_key\":\"second_inner_value\"}," +
                "\"second_inner_key\":\"just a value\"}",
            "second_outer_key" to "{\"another_inner_key\":false}"
        )

        val mappedObject = StripeJsonUtils.jsonObjectToStringMap(NESTED_JSON_TEST_OBJECT)
        JsonTestUtils.assertMapEquals(expectedMap, mappedObject)
    }

    @Test
    @Throws(JSONException::class)
    fun jsonObjectToMap_withNestedObjectAndArrays_returnsExpectedMap() {
        val items = listOf(
            mapOf("id" to 123),
            mapOf("id" to "this time with letters"),
            "a string item",
            256,
            listOf(1, 2, "C", 4),
            listOf(mapOf("deep" to "deepValue"))
        )
        val expectedMap = mapOf(
            "other_outer_key" to false,
            "outer_key" to mapOf(
                "items" to items,
                "another_key" to "a simple value this time"
            )
        )

        val convertedMap =
            StripeJsonUtils.jsonObjectToMap(NESTED_MIXED_ARRAY_OBJECT)
        JsonTestUtils.assertMapEquals(expectedMap, convertedMap)
    }

    @Test
    @Throws(JSONException::class)
    fun jsonArrayToList_forSimpleList_returnsExpectedList() {
        val expectedList = listOf(1, 2, 3, "a", true, "cde")
        val convertedJsonArray = StripeJsonUtils.jsonArrayToList(SIMPLE_JSON_TEST_ARRAY)
        JsonTestUtils.assertListEquals(expectedList, convertedJsonArray)
    }

    companion object {

        private val SIMPLE_JSON_TEST_OBJECT = JSONObject(
            """
            {
                "akey": "avalue",
                "bkey": "bvalue",
                "boolkey": true,
                "numkey": 123
            }
            """.trimIndent()
        )

        private val NESTED_JSON_TEST_OBJECT = JSONObject(
            """
            {
                "top_key": {
                    "first_inner_key": {
                        "innermost_key": 1000,
                        "second_innermost_key": "second_inner_value"
                    },
                    "second_inner_key": "just a value"
                },
                "second_outer_key": {
                    "another_inner_key": false
                }
            }
            """.trimIndent()
        )

        private val SIMPLE_JSON_TEST_ARRAY = JSONArray(
            """
            [1, 2, 3, "a", true, "cde"]
            """.trimIndent()
        )

        private val NESTED_MIXED_ARRAY_OBJECT = JSONObject(
            """
            {
                "outer_key": {
                    "items": [{
                            "id": 123
                        },
                        {
                            "id": "this time with letters"
                        },
                        "a string item",
                        256,
                        [1, 2, "C", 4],
                        [{
                            "deep": "deepValue"
                        }]
                    ],
                    "another_key": "a simple value this time"
                },
                "other_outer_key": false
            }
            """.trimIndent()
        )
    }
}
