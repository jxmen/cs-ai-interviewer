package dev.jxmen.cs.ai.interviewer.domain.chat

import dev.jxmen.cs.ai.interviewer.domain.BaseEntity
import dev.jxmen.cs.ai.interviewer.domain.member.Member
import dev.jxmen.cs.ai.interviewer.domain.subject.Subject
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Suppress("ktlint:standard:no-blank-line-in-list")
@Entity
@Table(
    indexes = [
        Index(name = "idx_chat_subject_id_member_id", columnList = "subject_id,member_id"),
    ],
)
class Chat(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    @Comment("주제")
    val subject: Subject,

    @ManyToOne(fetch = FetchType.LAZY) // NOTE: sessionId 컬럼 제거 시 nullable 제거
    @JoinColumn(name = "member_id")
    @Comment("멤버 아이디")
    val member: Member,

    @get:Embedded
    val content: ChatContent,

) : BaseEntity() {
    fun isAnswer(): Boolean = content.isAnswer()

    fun isQuestion(): Boolean = content.isQuestion()

    companion object {
        const val MAX_ANSWER_SCORE = 100
        const val MAX_ANSWER_COUNT = 10

        fun createQuestion(
            subject: Subject,
            member: Member,
            message: String,
        ): Chat {
            val content = ChatContent.createQuestion(message)
            val chat = Chat(subject = subject, member = member, content = content)

            return chat
        }

        fun createAnswer(
            subject: Subject,
            member: Member,
            answer: String,
            score: Int,
            createdAt: LocalDateTime? = null,
        ): Chat {
            val content = ChatContent.createAnswer(answer, score)
            val chat = Chat(subject = subject, member = member, content = content)

            createdAt?.let { chat.createdAt = it }
            return chat
        }

        fun createFirstQuestion(
            subject: Subject,
            member: Member,
        ): Chat {
            val content = ChatContent.createQuestion(subject.question)
            val chat = Chat(subject = subject, member = member, content = content)

            return chat
        }
    }
}
