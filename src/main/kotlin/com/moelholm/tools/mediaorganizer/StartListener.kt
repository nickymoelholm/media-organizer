package com.moelholm.tools.mediaorganizer

import com.moelholm.tools.mediaorganizer.filesystem.FileSystemType
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.nio.file.Paths
import javax.annotation.PostConstruct

@Component
@Profile("production")
class StartListener(
    private val organizer: MediaOrganizer,
    private val environment: Environment
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun started() {

        val startedWithMandatoryArguments = environment.containsProperty(MainArgument.FROM_DIR.argumentName) && environment.containsProperty(MainArgument.TO_DIR.argumentName)

        if (!startedWithMandatoryArguments) {
            printUsageAndExit()
            return
        }

        var runMode = environment.getProperty(MainArgument.RUN_MODE.argumentName)
        val fileSystemType = FileSystemType.fromString(environment.getProperty(MainArgument.FILESYSTEM_TYPE.argumentName))
        val fromDir = environment.getRequiredProperty(MainArgument.FROM_DIR.argumentName)
        val toDir = environment.getRequiredProperty(MainArgument.TO_DIR.argumentName)

        printApplicationStartedMessage(fromDir, toDir, runMode, fileSystemType)

        runMode = if (runMode == null) "once" else runMode

        when {
            "daemon".equals(runMode, ignoreCase = true) -> runAsDaemon(fromDir, toDir)
            "once".equals(runMode, ignoreCase = true) -> runOnce(fromDir, toDir)
            "web".equals(runMode, ignoreCase = true) -> runInWebMode()
            else -> logValidationErrorAndExit(runMode)
        }
    }

    private fun runInWebMode() {
        logger.info("Running in 'web' mode - awaiting run signals on HTTP endpoint")
    }

    private fun logValidationErrorAndExit(runMode: String) {
        logger.warn("Unknown run mode [{}]. Exiting application", runMode)
        System.exit(-1)
    }

    private fun runOnce(fromDir: String, toDir: String) {
        try {
            logger.info("Running in 'once' mode")
            organizer.undoFlatMess(Paths.get(fromDir), Paths.get(toDir))
        } catch (e: Exception) {
            logger.warn("Exiting application with error", e)
        } finally {
            logger.info("Exiting application")
        }
        System.exit(0)
    }

    private fun runAsDaemon(fromDir: String, toDir: String) {
        logger.info("Running in 'daemon' mode")
        organizer.scheduleUndoFlatMess(Paths.get(fromDir), Paths.get(toDir))
    }

    private fun printApplicationStartedMessage(fromDir: String, toDir: String, runMode: String?,
                                               fileSystemType: FileSystemType) {
        logger.info("")
        logger.info("Application started with the following arguments:")
        logger.info("    --{} = {}", MainArgument.RUN_MODE.argumentName, runMode)
        logger.info("    --{} = {}", MainArgument.FILESYSTEM_TYPE.argumentName,
            fileSystemType.toString().toLowerCase())
        logger.info("    --{} = {}", MainArgument.FROM_DIR.argumentName, fromDir)
        logger.info("    --{}   = {}", MainArgument.TO_DIR.argumentName, toDir)
        logger.info("")
    }

    private fun printUsageAndExit() {

        // TODO :: Kotlinize this baby

        logger.info("")
        logger.info(
            "Usage: Main --{}=[dir to copy from] --{}=[dir to copy to] [--{}=[mode]] [--{}=[type]]",
            MainArgument.FROM_DIR.argumentName,
            MainArgument.TO_DIR.argumentName, MainArgument.RUN_MODE.argumentName,
            MainArgument.FILESYSTEM_TYPE.argumentName)
        logger.info("")
        logger.info("  Where:")
        logger.info("")
        logger.info("    --{} folder that contains your media files",
            MainArgument.FROM_DIR.argumentName)
        logger.info("    --{} folder that should contain the organized media files",
            MainArgument.TO_DIR.argumentName)
        logger.info("    --{} One of: [once, daemon, web]. Default is once",
            MainArgument.RUN_MODE.argumentName)
        logger.info("    --{} One of: [local, dropbox]. Default is local",
            MainArgument.FILESYSTEM_TYPE.argumentName)
        logger.info("")
        System.exit(0)
    }
}
