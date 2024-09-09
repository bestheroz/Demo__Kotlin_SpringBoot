package com.github.bestheroz.standard.common.util

import org.springframework.core.env.AbstractEnvironment

object EnvironmentUtils {
    private fun getActivateProfile(): String = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME) ?: "local"

    fun isLocal(): Boolean = getActivateProfile() == "local"
}
