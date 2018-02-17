package com.moelholm.tools.mediaorganizer.filesystem

import java.io.IOException
import java.nio.file.Path
import java.util.stream.Stream

interface FileSystem {

    @Throws(IOException::class)
    fun move(from: Path, to: Path)

    fun streamOfAllFilesFromPath(path: Path): Stream<Path>

    fun existingDirectory(path: Path): Boolean

}
