package com.moelholm.tools.mediaorganizer

import com.moelholm.tools.mediaorganizer.filesystem.FileSystem
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.support.CronTrigger
import org.springframework.scheduling.support.SimpleTriggerContext
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors.groupingBy

@Component
class MediaOrganizer(private val configuration: MediaOrganizerProperties,
                     private val scheduler: TaskScheduler,
                     private val fileSystem: FileSystem) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun asyncUndoFlatMess(from: Path, to: Path) = undoFlatMess(from, to)

    fun scheduleUndoFlatMess(from: Path, to: Path) {

        if (hasInvalidParameters(from, to)) {
            return
        }

        val trigger = CronTrigger(configuration.scheduleAsCronExpression)
        scheduler.schedule(Runnable { undoFlatMess(from, to) }, trigger)

        logger.info("Scheduled job that will move files from [$from] to [$to]")
        val nextExecutionTime = trigger.nextExecutionTime(SimpleTriggerContext())
        logger.info("    - Job will start at [${formatDateAsString(nextExecutionTime)}]")
    }

    fun undoFlatMess(from: Path, to: Path) {

        if (hasInvalidParameters(from, to)) {
            return
        }

        logger.info("Moving files from [$from] to [$to]")

        fileSystem.streamOfAllFilesFromPath(from)
            .filter { selectMediaFiles(it) }
            .collect(groupingBy(this::toYearMonthDayString))
            .forEach { yearMonthDayString, mediaFilePathList ->

                logger.info("Processing [$yearMonthDayString] which has [${mediaFilePathList.size}] media files")

                val destinationDirectoryName = generateFinalDestinationDirectoryName(yearMonthDayString, mediaFilePathList)
                val destinationDirectoryPath = to.resolve(destinationDirectoryName)

                mediaFilePathList.forEach { move(it, destinationDirectoryPath.resolve(it.fileName)) }
            }
    }

    private fun selectMediaFiles(path: Path) =
        configuration.getMediaFileExtensionsToMatch().any { path.toString().toLowerCase().endsWith(".$it") }

    private fun hasInvalidParameters(from: Path, to: Path): Boolean {

        var result = false

        if (!fileSystem.existingDirectory(from)) {
            logger.info("Argument [from] is not an existing directory")
            result = true
        }

        if (!fileSystem.existingDirectory(to)) {
            logger.info("Argument [to] is not an existing directory")
            result = true
        }

        return result
    }

    private fun toYearMonthDayString(path: Path): String {

        val date = parseDateFromPathName(path) ?: return "unknown"

        val dateCal = Calendar.getInstance()
        dateCal.time = date

        val year = dateCal.get(Calendar.YEAR)

        var month = DateFormatSymbols(configuration.locale).months[dateCal.get(Calendar.MONTH)]
        month = Character.toUpperCase(month[0]) + month.substring(1)

        val day = dateCal.get(Calendar.DAY_OF_MONTH)

        return "$year - $month - $day"
    }

    // TODO kotlinize all this stuff....

    private fun generateFinalDestinationDirectoryName(folderName: String, mediaFilePaths: List<Path>): String {
        val lastPartOfFolderName = "( - \\d+)$"
        val replaceWithNewLastPartOfFolderName: String
        if (mediaFilePaths.size >= configuration.amountOfMediaFilesIndicatingAnEvent) {
            replaceWithNewLastPartOfFolderName = String.format("$1 - %s", configuration.suffixForDestinationFolderOfUnknownEventMediaFiles)
        } else {
            replaceWithNewLastPartOfFolderName = String.format(" - %s", configuration.suffixForDestinationFolderOfMiscMediaFiles)
        }
        return folderName.replace(lastPartOfFolderName.toRegex(), replaceWithNewLastPartOfFolderName)
    }

    private fun parseDateFromPathName(path: Path) =
        try {
            SimpleDateFormat(configuration.mediaFilesDatePattern).parse(path.fileName.toString())
        } catch (e: ParseException) {
            logger.warn("Failed to extract date from $path (Cause says: ${e.message})")
            null
        }

    private fun formatDateAsString(date: Date) =
        DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(date)

    private fun move(fileToMove: Path, pathThatFileShouldBeMovedTo: Path) =
        try {
            logger.info("    $pathThatFileShouldBeMovedTo.fileName")
            fileSystem.move(fileToMove, pathThatFileShouldBeMovedTo)
        } catch (e: FileAlreadyExistsException) {
            logger.info("File [${pathThatFileShouldBeMovedTo.fileName}] exists at destination folder - so skipping that")
        } catch (e: IOException) {
            logger.warn("Failed to move file from [$pathThatFileShouldBeMovedTo] to [$pathThatFileShouldBeMovedTo]", e)
        }
}