package com.moelholm.tools.mediaorganizer.filesystem

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang.builder.ReflectionToStringBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.joining
import java.util.stream.Stream

class DropboxFileSystem : FileSystem {

    @Value("\${dropbox.accessToken}")
    private val dropboxAccessToken: String? = null

    override fun existingDirectory(pathToTest: Path): Boolean {
        try {
            val dropboxPathToTest = toAbsoluteDropboxPath(pathToTest)
            val dropBoxRequest = DropboxFileRequest(dropboxPathToTest)
            val metaData = postToDropboxAndGetResponse("/files/get_metadata", dropBoxRequest, DropboxFile::class.java)
            return metaData.isDirectory
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {// -(file-does-not-exist)-
                return false
            }
            throw asRuntimeException(e)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    override fun streamOfAllFilesFromPath(from: Path): Stream<Path> {
        try {
            val dropboxPathToTest = toAbsoluteDropboxPath(from)
            val dropBoxRequest = DropboxFileRequest(dropboxPathToTest)
            val listFolderResponse = postToDropboxAndGetResponse("/files/list_folder", dropBoxRequest, DropboxListFolderResponse::class.java)
            return listFolderResponse.dropboxFiles!!.stream()//
                .map { it.pathLower }//
                .map { Paths.get(it) }
        } catch (e: HttpClientErrorException) {
            throw asRuntimeException(e)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(IOException::class)
    override fun move(from: Path, to: Path) {
        try {
            val fromDropboxPath = toAbsoluteDropboxPath(from)
            val toDropboxPath = toAbsoluteDropboxPath(to)
            val dropBoxRequest = DropboxMoveRequest(fromDropboxPath, toDropboxPath)
            postToDropboxAndGetResponse("/files/move", dropBoxRequest, DropboxFile::class.java)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                throw FileAlreadyExistsException(to.toString())
            }
            throw asRuntimeException(e)
        } catch (e: Exception) {
            throw IOException(e)
        }

    }

    private fun asRuntimeException(e: HttpClientErrorException): RuntimeException {
        return RuntimeException(String.format("%s [%s]", e.message, e.responseBodyAsString), e)
    }

    private fun createRestTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        restTemplate.interceptors = listOf(ClientHttpRequestInterceptor { request, bytes, execution ->
            request.headers["Authorization"] = listOf(String.format("Bearer %s", dropboxAccessToken))
            request.headers["Content-Type"] = listOf("application/json")
            execution.execute(request, bytes)
        })
        return restTemplate
    }

    @Throws(IOException::class, JsonParseException::class, JsonMappingException::class)
    private fun <T> postToDropboxAndGetResponse(path: String, arg: Any, responseType: Class<T>): T {

        val resultJsonString = createRestTemplate().postForObject(String.format("https://api.dropboxapi.com/2%s", path), arg, String::class.java)

        // Note (!) : this is a workaround for an ...ahem temporary issue... with getting the Java POJO object directly from the RestTemplate
        val mapper = ObjectMapper()

        return mapper.readValue(resultJsonString, responseType)
    }

    private fun toAbsoluteDropboxPath(pathToTest: Path): String {
        val pathAsString = pathToTest.toString()
        return if (pathAsString.startsWith("/")) pathAsString else String.format("/%s", pathAsString)
    }

    class DropboxFileRequest(val path: String)

    class DropboxMoveRequest(@field:JsonProperty("from_path")
                             val fromPath: String, @field:JsonProperty("to_path")
                             val toPath: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    class DropboxListFolderResponse {

        @JsonProperty("entries")
        val dropboxFiles: List<DropboxFile>? = null

        override fun toString(): String {
            return dropboxFiles!!.stream().map { it.toString() }.collect(joining("\n"))
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class DropboxFile {

        @JsonProperty(".tag")
        private val tag: String? = null

        @JsonProperty("path_lower")
        val pathLower: String? = null

        val isDirectory: Boolean
            get() = "folder".equals(tag!!, ignoreCase = true)

        override fun toString(): String {
            return ReflectionToStringBuilder.toString(this)
        }
    }

}