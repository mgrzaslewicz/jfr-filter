package com.mg.jfr.api

import jdk.jfr.consumer.RecordedEvent
import java.util.function.Predicate

/**
 * Needed to display selection for filtering by CPU Samples/Total Time in intellij
 */
class JdkActiveSettingPredicate : Predicate<RecordedEvent> {
    override fun test(event: RecordedEvent) = event.eventType.name == "jdk.ActiveSetting"
}
