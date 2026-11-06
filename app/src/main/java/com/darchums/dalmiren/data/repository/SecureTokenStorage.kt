package com.darchums.dalmiren.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.secureSessionDataStore by preferencesDataStore(
    name = "dalmiren_secure_session"
)

data class StoredAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
    val tokenType: String = "bearer"
)

class SecureTokenStorageException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class SecureTokenStorage(context: Context) {

    private val appContext = context.applicationContext

    private object Keys {
        val IV = stringPreferencesKey("encrypted_session_iv")
        val CIPHERTEXT = stringPreferencesKey("encrypted_session_data")
    }

    companion object {
        private const val KEY_ALIAS = "DALMIREN_AUTH_AES_KEY"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    suspend fun saveTokens(tokens: StoredAuthTokens) {
        if (
            tokens.accessToken.isBlank() ||
            tokens.refreshToken.isBlank() ||
            tokens.expiresInSeconds <= 0
        ) {
            throw SecureTokenStorageException("Invalid authentication session data.")
        }

        try {
            val payload = JSONObject().apply {
                put("access_token", tokens.accessToken)
                put("refresh_token", tokens.refreshToken)
                put("expires_in", tokens.expiresInSeconds)
                put("token_type", tokens.tokenType)
            }.toString()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

            val encryptedBytes = cipher.doFinal(
                payload.toByteArray(Charsets.UTF_8)
            )

            val encodedIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            val encodedCiphertext = Base64.encodeToString(
                encryptedBytes,
                Base64.NO_WRAP
            )

            appContext.secureSessionDataStore.edit { preferences ->
                preferences[Keys.IV] = encodedIv
                preferences[Keys.CIPHERTEXT] = encodedCiphertext
            }
        } catch (exception: Exception) {
            throw SecureTokenStorageException(
                "Unable to securely save the authentication session.",
                exception
            )
        }
    }

    suspend fun readTokens(): StoredAuthTokens? {
        try {
            val preferences = appContext.secureSessionDataStore.data.first()

            val encodedIv = preferences[Keys.IV]
            val encodedCiphertext = preferences[Keys.CIPHERTEXT]

            if (encodedIv == null && encodedCiphertext == null) return null

            if (encodedIv.isNullOrBlank() || encodedCiphertext.isNullOrBlank()) {
                throw SecureTokenStorageException(
                    "The encrypted authentication session is incomplete."
                )
            }

            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            )

            val plaintext = cipher.doFinal(ciphertext)
            val payload = JSONObject(String(plaintext, Charsets.UTF_8))

            val tokens = StoredAuthTokens(
                accessToken = payload.getString("access_token"),
                refreshToken = payload.getString("refresh_token"),
                expiresInSeconds = payload.getInt("expires_in"),
                tokenType = payload.optString("token_type", "bearer")
            )

            if (
                tokens.accessToken.isBlank() ||
                tokens.refreshToken.isBlank() ||
                tokens.expiresInSeconds <= 0
            ) {
                throw SecureTokenStorageException(
                    "The stored authentication session is invalid."
                )
            }

            return tokens
        } catch (exception: SecureTokenStorageException) {
            throw exception
        } catch (exception: Exception) {
            throw SecureTokenStorageException(
                "Unable to decrypt the authentication session.",
                exception
            )
        }
    }

    suspend fun clearTokens() {
        try {
            appContext.secureSessionDataStore.edit { preferences ->
                preferences.remove(Keys.IV)
                preferences.remove(Keys.CIPHERTEXT)
            }
        } catch (exception: Exception) {
            throw SecureTokenStorageException(
                "Unable to clear the authentication session.",
                exception
            )
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}
