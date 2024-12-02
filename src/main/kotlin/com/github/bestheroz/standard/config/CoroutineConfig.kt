package com.github.bestheroz.standard.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig : DisposableBean {
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    @Bean fun coroutineScope(): CoroutineScope = coroutineScope

    override fun destroy() {
        job.cancel() // 스코프 취소
    }
}
