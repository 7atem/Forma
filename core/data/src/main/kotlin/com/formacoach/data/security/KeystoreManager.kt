// Adapted from Nemory KeystoreManager.kt, Apache-2.0. See docs/NEMORY_REUSE_AUDIT.md.
package com.formacoach.data.security

import android.content.Context
import android.util.AtomicFile
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom

/**
 * Manages the SQLCipher database passphrase.
 *
 * The passphrase is generated once, encrypted with AES-256 GCM via
 * [EncryptedFile] (backed by Android Keystore), and stored in app private storage.
 */
class KeystoreManager(
    private val context: Context
) {

    companion object {
        private const val PASSPHRASE_FILE_NAME = "forma_passphrase.bin"
        private const val PASSPHRASE_LENGTH_BYTES = 32
        private val keyLock = Any()
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context, "forma_master_key")
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false)
            .build()
    }

    /**
     * Returns the existing database passphrase or generates and persists a new one.
     */
    fun getOrCreateDatabasePassphrase(): ByteArray = synchronized(keyLock) {
        val file = File(context.filesDir, PASSPHRASE_FILE_NAME)
        val atomicFile = AtomicFile(file)
        if (file.exists() || File(file.path + ".bak").exists()) {
            // Recover an interrupted atomic write before EncryptedFile opens the final filename.
            atomicFile.openRead().close()
            // Existing unreadable keys fail closed; Forma has no legacy key format.
            readPassphrase(file)
        } else {
            check(!context.getDatabasePath("forma.db").exists()) {
                "The existing database key is missing; refusing to replace it"
            }
            val passphrase = generatePassphrase()
            writePassphrase(file, passphrase)
            passphrase
        }
    }

    private fun generatePassphrase(): ByteArray {
        val random = SecureRandom()
        return ByteArray(PASSPHRASE_LENGTH_BYTES).apply {
            random.nextBytes(this)
        }
    }

    private fun readPassphrase(file: File): ByteArray {
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        return encryptedFile.openFileInput().use { it.readBytes() }.also {
            require(it.size == PASSPHRASE_LENGTH_BYTES) { "Invalid stored database key" }
        }
    }

    private fun writePassphrase(file: File, passphrase: ByteArray) {
        // EncryptedFile authenticates the basename. Stage under the SAME basename.
        val directory = java.nio.file.Files.createTempDirectory(context.filesDir.toPath(), "key_write_").toFile()
        val temp = File(directory, file.name)
        try {
            val encryptedFile = EncryptedFile.Builder(
                context,
                temp,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileOutput().use { output ->
                output.write(passphrase)
            }
            val verified = readPassphrase(temp)
            try {
                check(verified.contentEquals(passphrase)) { "Database key verification failed" }
            } finally {
                verified.fill(0)
            }
            val atomicFile = AtomicFile(file)
            val output = atomicFile.startWrite()
            try {
                temp.inputStream().use { it.copyTo(output) }
                atomicFile.finishWrite(output)
            } catch (failure: Throwable) {
                atomicFile.failWrite(output)
                throw failure
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
