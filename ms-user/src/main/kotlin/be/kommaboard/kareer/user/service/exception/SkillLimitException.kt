package be.kommaboard.kareer.user.service.exception

class SkillLimitException(val limit: Int) : IllegalArgumentException("Cannot exceed the limit of $limit skills")
