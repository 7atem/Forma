package com.formacoach.coach

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class LocalHighlightsTest {
    @Test fun `unknown facts prose duplicates and invented numbers fail closed`() {
        val allowed=setOf("training","food","coverage")
        assertEquals(listOf("food","training"),LocalHighlights.verify("[\"food\",\"training\"]",allowed))
        listOf("[]","[\"advice\"]","[\"food\",\"food\"]","Eat 1200 calories","[\"food\",42]").forEach {assertNull(LocalHighlights.verify(it,allowed))}
    }
    @Test fun `background and unsupported devices never generate`()=runBlocking {
        var generated=false
        val model=object:LocalTextModel {
            override suspend fun status()=ModelAvailability.UNAVAILABLE
            override suspend fun generate(prompt:String):String {generated=true;return "[]"}
            override fun download()=emptyFlow<ModelAvailability>()
        }
        assertNull(LocalHighlights.select(model,listOf(CoachFact("training","1 session"))){true})
        assertNull(LocalHighlights.select(model,listOf(CoachFact("training","1 session"))){false})
        assertFalse(generated)
    }
    @Test fun `inference remains cancellable and never returns after foreground ends`()=runBlocking {
        var foreground=true
        val model=object:LocalTextModel {
            override suspend fun status()=ModelAvailability.READY
            override suspend fun generate(prompt:String):String {foreground=false;return "[\"training\"]"}
            override fun download()=emptyFlow<ModelAvailability>()
        }
        assertNull(LocalHighlights.select(model,listOf(CoachFact("training","1 session"))){foreground})
    }
    @Test fun `cancellation propagates to a running provider`()=runBlocking {
        val started=CompletableDeferred<Unit>()
        val model=object:LocalTextModel {
            override suspend fun status()=ModelAvailability.READY
            override suspend fun generate(prompt:String):String {started.complete(Unit);awaitCancellation()}
            override fun download()=emptyFlow<ModelAvailability>()
        }
        val job=launch {LocalHighlights.select(model,listOf(CoachFact("training","1 session"))){true}}
        started.await();job.cancelAndJoin();assertTrue(job.isCancelled)
    }
}
