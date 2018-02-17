package com.moelholm.tools.mediaorganizer.filesystem

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

class LocalFileSystem : FileSystem {

    override fun existingDirectory(path: Path) =
        path.toFile().isDirectory

    override fun streamOfAllFilesFromPath(path: Path): Stream<Path> =
        Files.list(path)

    override fun move(from: Path, to: Path) {
        if (!to.parent.toFile().exists()) {
            to.parent.toFile().mkdirs()
        }
        Files.move(from, to)
    }

}