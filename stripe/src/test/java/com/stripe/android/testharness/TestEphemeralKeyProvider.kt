package com.stripe.android.testharness

import androidx.annotation.Size
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener

/**
 * An [EphemeralKeyProvider] to be used in tests that automatically returns test values.
 */
internal class TestEphemeralKeyProvider : EphemeralKeyProvider {
    private var errorCode = INVALID_ERROR_CODE
    private var errorMessage: String? = null
    private var rawEphemeralKey: String? = null

    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        when {
            rawEphemeralKey != null ->
                keyUpdateListener.onKeyUpdate(rawEphemeralKey!!)
            errorCode != INVALID_ERROR_CODE ->
                keyUpdateListener.onKeyUpdateFailure(errorCode, errorMessage!!)
            else -> // Useful to test edge cases
                keyUpdateListener.onKeyUpdate("")
        }
    }

    fun setNextRawEphemeralKey(rawEphemeralKey: String) {
        this.rawEphemeralKey = rawEphemeralKey
        errorCode = INVALID_ERROR_CODE
        errorMessage = ""
    }

    fun setNextError(errorCode: Int, errorMessage: String) {
        rawEphemeralKey = null
        this.errorCode = errorCode
        this.errorMessage = errorMessage
    }

    companion object {

        private const val INVALID_ERROR_CODE = -1
    }
}
