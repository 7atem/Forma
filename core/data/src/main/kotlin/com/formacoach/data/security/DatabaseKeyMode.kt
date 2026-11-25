package com.formacoach.data.security

import android.content.Context
import android.util.AtomicFile
import java.io.File

/**
 * Chooses how the stored 32-byte random database key is handed to SQLCipher.
 *
 * As a passphrase, SQLCipher stretches it with PBKDF2 on every open (about 1 s on a slow emulator). The key is
 * already 256 random bits from SecureRandom, held only under the Android Keystore, so stretching adds no strength.
 * Databases created by this version use the same bytes as a SQLCipher raw key (`x'<hex>'`), which skips PBKDF2.
 *
 * Databases created before this change keep passphrase mode and are never re-keyed: no existing file is rewritten.
 * A non-secret marker file records raw mode. It is written before the database is created, so an existing database
 * without the marker is always opened exactly as it was created.
 */
object DatabaseKeyMode {
    private const val MARKER_FILE_NAME = "forma_db_key_mode"
    private const val RAW_MODE = "raw-v1"
    private const val DATABASE_NAME = "forma.db"

    fun sqlcipherKey(context: Context, key: ByteArray): ByteArray {
        val marker = File(context.filesDir, MARKER_FILE_NAME)
        val database = context.getDatabasePath(DATABASE_NAME)
        val raw = when {
            markerExists(marker) -> {
                // An unreadable or unknown marker fails closed instead of guessing a key mode.
                check(readMarker(marker) == RAW_MODE) { "Unknown database key mode" }
                true
            }
            database.exists() -> false
            else -> {
                writeMarker(marker)
                true
            }
        }
        return if (raw) rawKey(key) else key
    }

    /** SQLCipher raw-key literal for [key]; never logged or written anywhere. */
    internal fun rawKey(key: ByteArray): ByteArray {
        val hex = CharArray(key.size * 2)
        key.forEachIndexed { index, byte ->
            val value = byte.toInt() and 0xff
            hex[index * 2] = Character.forDigit(value ushr 4, 16)
            hex[index * 2 + 1] = Character.forDigit(value and 0x0f, 16)
        }
        return ("x'" + String(hex) + "'").toByteArray(Charsets.US_ASCII).also { hex.fill('0') }
    }

    private fun markerExists(marker: File) = marker.exists() || File(marker.path + ".bak").exists()

    private fun readMarker(marker: File): String = AtomicFile(marker).readFully().toString(Charsets.US_ASCII).trim()

    private fun writeMarker(marker: File) {
        val atomic = AtomicFile(marker)
        val output = atomic.startWrite()
        try {
            output.write(RAW_MODE.toByteArray(Charsets.US_ASCII))
            output.fd.sync()
            atomic.finishWrite(output)
        } catch (failure: Exception) {
            atomic.failWrite(output)
            throw failure
        }
    }
}
