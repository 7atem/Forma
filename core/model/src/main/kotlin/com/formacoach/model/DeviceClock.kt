package com.formacoach.model

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Follow a device timezone change without requiring the repository or ViewModel to restart. */
object DeviceClock:Clock() {
    override fun instant():Instant=Instant.now()
    override fun getZone():ZoneId=ZoneId.systemDefault()
    override fun withZone(zone:ZoneId):Clock=Clock.system(zone)
}
