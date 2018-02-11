package com.moelholm.tools.mediaorganizer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import java.util.Arrays
import java.util.Locale

@Component
class MediaOrganizerProperties {

    @Value("\${daemon.scheduleAsCronExpression}")
    val scheduleAsCronExpression: String? = null

    @Value("\${mediafiles.datepattern}")
    val mediaFilesDatePattern: String? = null

    @Value("\${mediafiles.mediaFileExtensionsToMatch}")
    private val mediaFileExtensionsToMatch: Array<String>? = null

    @Value("\${destination.amountOfMediaFilesIndicatingAnEvent}")
    val amountOfMediaFilesIndicatingAnEvent: Int = 0

    @Value("\${destination.localeForGeneratingDestinationFolderNames}")
    val locale: Locale? = null

    @Value("\${destination.suffixForDestinationFolderOfUnknownEventMediaFiles}")
    val suffixForDestinationFolderOfUnknownEventMediaFiles: String? = null

    @Value("\${destination.suffixForDestinationFolderOfMiscMediaFiles}")
    val suffixForDestinationFolderOfMiscMediaFiles: String? = null

    fun getMediaFileExtensionsToMatch(): List<String> {
        return Arrays.asList(*mediaFileExtensionsToMatch!!)
    }

}
