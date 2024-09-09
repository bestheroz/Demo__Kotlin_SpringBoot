package com.github.bestheroz.standard.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.providers.AwsRegionProvider
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain

@Configuration
class AwsConfig {
    @Bean
    fun awsRegionProvider(): AwsRegionProvider = DefaultAwsRegionProviderChain()
}
