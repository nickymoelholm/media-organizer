package com.moelholm.tools.mediaorganizer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class MediaOrganizerProperties(

    @Value("\${mediafiles.mediaFileExtensionsToMatch}")
    private val mediaFileExtensionsToMatch: Array<String>,

    @Value("\${daemon.scheduleAsCronExpression}")
    val scheduleAsCronExpression: String,

    @Value("\${mediafiles.datepattern}")
    val mediaFilesDatePattern: String,

    @Value("\${destination.amountOfMediaFilesIndicatingAnEvent}")
    val amountOfMediaFilesIndicatingAnEvent: Int,

    @Value("\${destination.localeForGeneratingDestinationFolderNames}")
    val locale: Locale,

    @Value("\${destination.suffixForDestinationFolderOfUnknownEventMediaFiles}")
    val suffixForDestinationFolderOfUnknownEventMediaFiles: String,

    @Value("\${destination.suffixForDestinationFolderOfMiscMediaFiles}")
    val suffixForDestinationFolderOfMiscMediaFiles: String

) {
    fun getMediaFileExtensionsToMatch(): List<String> = listOf(*mediaFileExtensionsToMatch)
}
