package be.kommaboard.kareer.user.lib.dto.request

import be.kommaboard.kareer.user.lib.constraint.ItemSize
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

class EditUserDetailsSkillsDTO(

    @get:NotNull(message = "{EditUserDetailsSkillsDTO.skills.NotNull}")
    @get:Size(message = "{EditUserDetailsSkillsDTO.skills.Size}", min = 1)
    @get:ItemSize(message = "{EditUserDetailsSkillsDTO.skills.ItemSize}", min = 1, max = 25)
    val skills: List<String>?,
)
