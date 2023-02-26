package be.kommaboard.kareer.user.proxy

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.storage.lib.dto.request.CreateFileReferenceDTO
import be.kommaboard.kareer.storage.lib.dto.response.FileReferenceDTO
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader

interface StorageProxy {

    @GetMapping("/api/storage/v1/{id}")
    fun getFileReference(
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable id: String,
    ): FileReferenceDTO

    @PostMapping("/api/storage/v1")
    fun createFileReference(
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        dto: CreateFileReferenceDTO,
    ): FileReferenceDTO

    @DeleteMapping("/api/storage/v1/{id}")
    fun deleteFileReference(
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable id: String,
    ): ResponseEntity<Unit>
}

@Profile("kubernetes")
@FeignClient("ms-storage")
interface KubernetesStorageProxy : StorageProxy

@Profile("!kubernetes")
@FeignClient("ms-storage", url = "http://\${kareer.hosts.ms-storage}:8005")
interface LocalStorageProxy : StorageProxy
