package be.kommaboard.kareer.user.service.exception

class SkillLimitException(val limit: Int) : IllegalArgumentException("Must have at least 1, and no more than $limit skills")
