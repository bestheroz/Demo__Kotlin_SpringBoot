package com.github.bestheroz.standard.config

import com.github.bestheroz.standard.common.log.logger
import com.github.bestheroz.standard.common.util.LogUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfig : DisposableBean {
    companion object {
        private val log = logger()
    }

    private val job = SupervisorJob()
    private val exceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            log.error(LogUtils.getStackTrace(exception))
        }
    private val coroutineScope = CoroutineScope(job + Dispatchers.IO + exceptionHandler)

    @Bean fun coroutineScope(): CoroutineScope = coroutineScope

    override fun destroy() {
        job.cancel() // 스코프 취소
    }
}
