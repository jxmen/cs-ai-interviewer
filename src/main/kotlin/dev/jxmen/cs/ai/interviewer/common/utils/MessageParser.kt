package dev.jxmen.cs.ai.interviewer.common.utils

import org.springframework.stereotype.Component

@Component
class MessageParser {
    companion object {
        /**
         * @see PromptMessageFactory 프롬프트 메시지 생성 담당
         */
        private val SCORE_REGEX = "답변에 대한 점수: (\\d+)점".toRegex()
    }

    fun parseScore(s: String): Int {
        val parsedScore =
            SCORE_REGEX
                .find(s)
                ?.groupValues
                ?.get(1)
                ?.toInt()

        return parsedScore ?: 0 // null이면 0점으로 처리
    }
}
