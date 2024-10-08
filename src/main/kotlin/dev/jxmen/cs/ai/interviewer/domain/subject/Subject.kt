package dev.jxmen.cs.ai.interviewer.domain.subject

import dev.jxmen.cs.ai.interviewer.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment

/**
 * 주제
 */
@Suppress("ktlint:standard:no-blank-line-in-list")
@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["title", "question"]),
    ],
)
class Subject(

    @Column(nullable = false, unique = true)
    @Comment("제목")
    val title: String,

    @Column(nullable = false, unique = true)
    @Comment("질문")
    val question: String,

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    @Convert(converter = SubjectCategoryConverter::class)
    @Comment("카테고리")
    val category: SubjectCategory,
) : BaseEntity() {
    constructor(id: Long, title: String, question: String, category: SubjectCategory) : this(
        title = title,
        question = question,
        category = category,
    ) {
        super.id = id
    }

    companion object {
        fun createOS(
            id: Long,
            title: String,
            question: String,
        ): Subject = Subject(id, title, question, SubjectCategory.OS)
    }
}
