package com.github.secretx33.infernalmobsreloaded.scanning.rules

import com.github.secretx33.infernalmobsreloaded.scanning.Rules
import com.github.secretx33.infernalmobsreloaded.scanning.rules.element.ElementWrapper

// ------------------------------
// Copyright (c) PiggyPiglet 2021
// https://www.piggypiglet.me
// ------------------------------
object RuleUtils {
    fun match(element: ElementWrapper, rules: Rules<*>): Boolean =
        rules.rules.all { rule -> rule.test(element) }
}
