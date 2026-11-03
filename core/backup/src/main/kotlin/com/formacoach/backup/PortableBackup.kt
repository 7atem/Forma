package com.formacoach.backup

import com.formacoach.model.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.LocalDate
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(val version: Int = 1, val exportedAtMillis: Long, val data: FitnessSnapshot)

/** Forma-only, authenticated portable record archive. Never exports device keys or plaintext files. */
object PortableBackup {
    const val MAX_PLAINTEXT_BYTES = 16 * 1024 * 1024
    const val ITERATIONS = 600_000
    private val magic = "FORMABK1".toByteArray(Charsets.US_ASCII)
    private const val HEADER_BYTES = 40
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun encrypt(data: FitnessSnapshot, password: CharArray, nowMillis: Long): ByteArray {
        validate(data)
        require(password.size in 12..1024)
        val plain=json.encodeToString(BackupPayload(exportedAtMillis=nowMillis,data=data)).toByteArray(Charsets.UTF_8)
        require(plain.size <= MAX_PLAINTEXT_BYTES)
        val salt=ByteArray(16).also {SecureRandom().nextBytes(it)}
        val iv=ByteArray(12).also {SecureRandom().nextBytes(it)}
        val header=ByteBuffer.allocate(HEADER_BYTES).put(magic).putInt(ITERATIONS).put(salt).put(iv).array()
        val key=derive(password,salt)
        try {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
            cipher.updateAAD(header)
            return header+cipher.doFinal(plain)
        } finally { key.fill(0); plain.fill(0) }
    }

    fun decrypt(bytes: ByteArray, password: CharArray): BackupPayload {
        require(bytes.size in (HEADER_BYTES+16)..(MAX_PLAINTEXT_BYTES+HEADER_BYTES+16))
        require(password.size in 12..1024)
        val header=bytes.copyOfRange(0,HEADER_BYTES)
        val buffer=ByteBuffer.wrap(header)
        val prefix=ByteArray(8).also {buffer.get(it)}
        require(prefix.contentEquals(magic)) { "Not a supported Forma backup" }
        require(buffer.int == ITERATIONS) { "Unsupported key parameters" }
        val salt=ByteArray(16).also {buffer.get(it)}
        val iv=ByteArray(12).also {buffer.get(it)}
        val key=derive(password,salt)
        val plain=try {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(key,"AES"),GCMParameterSpec(128,iv))
            cipher.updateAAD(header)
            cipher.doFinal(bytes,HEADER_BYTES,bytes.size-HEADER_BYTES)
        } finally { key.fill(0) }
        try {
            val text=plain.toString(Charsets.UTF_8)
            checkDepth(text)
            val payload=json.decodeFromString<BackupPayload>(text)
            require(payload.version == 1 && payload.exportedAtMillis > 0)
            validate(payload.data)
            return payload
        } finally { plain.fill(0) }
    }

    fun readBounded(input: InputStream): ByteArray {
        val limit=MAX_PLAINTEXT_BYTES+HEADER_BYTES+16
        val result=ByteArrayOutputStream()
        val buffer=ByteArray(8192)
        while(true) {
            val size=input.read(buffer)
            if(size<0) break
            if(size==0) continue
            require(result.size().toLong()+size <= limit) { "Backup is too large" }
            result.write(buffer,0,size)
        }
        return result.toByteArray()
    }

    fun validate(data: FitnessSnapshot) {
        val lists=listOf(data.sessions,data.sets,data.meals,data.measurements,data.programs,data.foods,data.checkIns,data.proposals)
        require(lists.sumOf {it.size.toLong()} <= 100_000)
        fun unique(ids: List<String>) {require(ids.distinct().size==ids.size)}
        unique(data.sessions.map {it.id}); unique(data.sets.map {it.id}); unique(data.meals.map {it.id})
        unique(data.programs.map {it.id}); unique(data.foods.map {it.id}); unique(data.checkIns.map {it.id}); unique(data.proposals.map {it.id})
        require(data.measurements.distinctBy {it.date}.size==data.measurements.size)
        require(data.sessions.count {it.completedAt==null} <= 1)
        val sessions=data.sessions.associateBy {it.id}
        require(data.sets.all {it.sessionId in sessions})
        val setCounts=data.sets.groupingBy {it.sessionId}.eachCount()
        require(data.sessions.filter {it.completedAt!=null}.all {(setCounts[it.id]?:0)>0 && it.completedAt!!>0})
        require(data.proposals.count {it.status==ProposalStatus.PENDING} <= 1)
        val checkIns=data.checkIns.associateBy {it.id}
        require(data.proposals.all {checkIns[it.checkInId]?.availableSessions == it.proposedSessions})
        val dates=data.sessions.map {it.date}+data.meals.map {it.date}+data.measurements.map {it.date}+data.checkIns.map {it.date}
        require(dates.all {it in LocalDate.of(1970,1,1)..LocalDate.of(2100,12,31)})
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec=PBEKeySpec(password,salt,ITERATIONS,256)
        return try {SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded}
        finally {spec.clearPassword()}
    }

    private fun checkDepth(text: String) {
        var quoted=false; var escaped=false; var depth=0
        text.forEach {char ->
            if(quoted) {
                if(escaped) escaped=false else if(char=='\\') escaped=true else if(char=='"') quoted=false
            } else when(char) {
                '"' -> quoted=true
                '{','[' -> {depth++; require(depth<=32)}
                '}',']' -> depth--
            }
        }
    }
}
