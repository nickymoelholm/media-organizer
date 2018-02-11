package com.moelholm.tools.mediaorganizer

import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths

@RestController
@RequestMapping("/api/mediaorganizer")
class MediaOrganizerController(
    private val environment: Environment,
    private val organizer: MediaOrganizer
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/trigger")
    fun runMediaOrganizer(@RequestHeader("Authorization") apiKey: String): ResponseEntity<*> {

        if (apiKey != environment.getProperty("web.apiKey")) {
            logger.warn("Unauthorized request")
            return ResponseEntity<String>(HttpStatus.UNAUTHORIZED)
        }

        val fromDir = environment.getProperty(MainArgument.FROM_DIR.argumentName)

        val toDir = environment.getProperty(MainArgument.TO_DIR.argumentName)

        organizer.asyncUndoFlatMess(Paths.get(fromDir), Paths.get(toDir))

        return ResponseEntity<String>(HttpStatus.OK)

    }

}