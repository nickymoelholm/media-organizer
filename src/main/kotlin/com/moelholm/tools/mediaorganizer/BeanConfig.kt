package com.moelholm.tools.mediaorganizer

import com.moelholm.tools.mediaorganizer.filesystem.DropboxFileSystem
import com.moelholm.tools.mediaorganizer.filesystem.FileSystem
import com.moelholm.tools.mediaorganizer.filesystem.FileSystemType
import com.moelholm.tools.mediaorganizer.filesystem.LocalFileSystem
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class BeanConfig(private val environment: Environment) {

    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler()

    @Bean
    fun fileSystem(): FileSystem =
        if (FileSystemType.LOCAL == FileSystemType.fromString(environment.getProperty(MainArgument.FILESYSTEM_TYPE.argumentName))) {
            LocalFileSystem()
        } else {
            DropboxFileSystem()
        }

}
