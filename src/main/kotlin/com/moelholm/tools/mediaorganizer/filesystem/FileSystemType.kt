package com.moelholm.tools.mediaorganizer.filesystem

enum class FileSystemType {

    LOCAL, DROPBOX;

    companion object {

        fun fromString(fileSystemTypeAsString: String?) =
            if (fileSystemTypeAsString == null)
                LOCAL
            else
                valueOf(fileSystemTypeAsString.toUpperCase())

    }
}
