package dev.jxmen.cs.ai.interviewer.domain.subject.exceptions

open class SubjectNotFoundException(
    id: Long,
) : RuntimeException("Subject not found by id: $id")
