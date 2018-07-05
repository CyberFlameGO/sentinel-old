package com.fredboat.sentinel.test

import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DockerExtension::class, SharedSpringContext::class)
open class IntegrationTest