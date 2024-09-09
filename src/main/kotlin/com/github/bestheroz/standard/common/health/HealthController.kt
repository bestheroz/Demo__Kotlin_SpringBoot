package com.github.bestheroz.standard.common.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1/health")
class HealthController(
    private val healthRepository: HealthRepository,
) {
    @GetMapping("liveness")
    fun liveness(): String = "liveness"

    @GetMapping("readiness")
    fun readiness(): String {
        healthRepository.selectNow()
        return "readiness"
    }
}
