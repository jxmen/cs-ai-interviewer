package dev.jxmen.cs.ai.interviewer.domain.subject.service.adapter

import dev.jxmen.cs.ai.interviewer.domain.subject.Subject
import dev.jxmen.cs.ai.interviewer.domain.subject.SubjectCategory
import dev.jxmen.cs.ai.interviewer.domain.subject.SubjectQueryRepository
import dev.jxmen.cs.ai.interviewer.domain.subject.exceptions.SubjectNotFoundException
import dev.jxmen.cs.ai.interviewer.domain.subject.service.port.SubjectQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubjectQueryService(
    private val subjectQueryRepository: SubjectQueryRepository,
) : SubjectQuery {
    @Transactional(readOnly = true)
    override fun getSubjectsByCategory(cateStr: String): List<Subject> {
        val subjectCategory = SubjectCategory.valueOf(cateStr.uppercase())

        return this.subjectQueryRepository.findByCategory(subjectCategory)
    }

    @Transactional(readOnly = true)
    override fun getSubjectById(id: Long): Subject = this.subjectQueryRepository.findByIdOrNull(id) ?: throw SubjectNotFoundException(id)
}

fun SubjectQueryRepository.findByIdOrNull(id: Long): Subject? = this.findById(id).orElse(null)
