package be.kommaboard.kareer.user.proxy

import be.kommaboard.kareer.authorization.InternalHttpHeaders
import be.kommaboard.kareer.organization.lib.dto.response.OrganizationDTO
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader

interface OrganizationProxy {

    @GetMapping("/api/organizations/v1/{uuid}")
    fun getOrganization(
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ROLE) consumerRole: String,
        @RequestHeader(value = InternalHttpHeaders.CONSUMER_ID) consumerId: String,
        @PathVariable uuid: String,
    ): OrganizationDTO
}

@Profile("kubernetes")
@FeignClient("ms-organization")
interface KubernetesOrganizationProxy : OrganizationProxy

@Profile("!kubernetes")
@FeignClient("ms-organization", url = "\${kareer.hosts.ms-organization}:8004")
interface LocalOrganizationProxy : OrganizationProxy
