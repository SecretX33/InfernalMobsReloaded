package com.github.secretx33.infernalmobsreloaded

import be.seeseemelk.mockbukkit.MockBukkit
import org.junit.jupiter.api.AfterAll

class TestPluginClass {

    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(InfernalMobsReloaded::class.java)

    @AfterAll
    fun unload() {
        MockBukkit.unmock()
    }
}
