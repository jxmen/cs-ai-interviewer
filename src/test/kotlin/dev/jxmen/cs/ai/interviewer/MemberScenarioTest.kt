package dev.jxmen.cs.ai.interviewer

import dev.jxmen.cs.ai.interviewer.application.adapter.ChatAppender
import dev.jxmen.cs.ai.interviewer.application.port.input.ReactiveMemberChatUseCase
import dev.jxmen.cs.ai.interviewer.domain.chat.ChatArchiveContentQueryRepository
import dev.jxmen.cs.ai.interviewer.domain.chat.ChatArchiveQueryRepository
import dev.jxmen.cs.ai.interviewer.domain.member.Member
import dev.jxmen.cs.ai.interviewer.domain.member.MemberCommandRepository
import dev.jxmen.cs.ai.interviewer.domain.member.MockMemberArgumentResolver
import dev.jxmen.cs.ai.interviewer.domain.subject.Subject
import dev.jxmen.cs.ai.interviewer.domain.subject.SubjectCategory
import dev.jxmen.cs.ai.interviewer.domain.subject.SubjectCommandRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.haveLength
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.willReturn
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.LocalDateTime

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ActiveProfiles("test")
class MemberScenarioTest(
    private val context: WebApplicationContext,
    private val subjectCommandRepository: SubjectCommandRepository,
    private val memberCommandRepository: MemberCommandRepository,
    private val chatArchiveQueryRepository: ChatArchiveQueryRepository,
    private val chatArchiveContentQueryRepository: ChatArchiveContentQueryRepository,
    private val chatAppender: ChatAppender,
) {
    @MockBean
    lateinit var reactiveMemberChatService: ReactiveMemberChatUseCase

    lateinit var mockMvc: MockMvc
    lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        // kotlin에서는 버그로 인해 필터 추가 불가 - https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework/server-filters.html
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()

        /**
         * WebTestClient는 WebFlux를 사용하는 경우에 사용. mockMvc는 MockMvcWebTestClient를 사용
         */
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context).build()
    }

    @Test
    fun `멤버 답변 및 채팅 내역 시나리오 테스트`() {
        // 멤버 생성
        val member = MockMemberArgumentResolver.member
        memberCommandRepository.save(member)

        // 멤버 인증 정보 저장
        val oauth2User = createOAuth2User(member)
        val authentication = createOAuth2AuthenticationToken(oauth2User)
        SecurityContextHolder.getContext().authentication = authentication

        // 주제 생성
        val subject =
            subjectCommandRepository.save(
                Subject(
                    title = "test subject",
                    question = "test question",
                    category = SubjectCategory.OS,
                ),
            )

        // 주제 목록 조회
        mockMvc
            .get("/api/v1/subjects") { param("category", "OS") }
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isArray() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].id") { value(subject.id) }
                jsonPath("$.data[0].title") { value("test subject") }
                jsonPath("$.data[0].category") { value("OS") }
                jsonPath("$.error") { value(null) }
            }

        // 주제 상세 조회
        mockMvc
            .get("/api/v1/subjects/${subject.id}")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(subject.id) }
                jsonPath("$.data.title") { value("test subject") }
                jsonPath("$.data.question") { value("test question") }
                jsonPath("$.data.category") { value("OS") }
                jsonPath("$.error") { value(null) }
            }.andDo {
                print()
            }

        // 내 주제 목록 조회
        mockMvc
            .get("/api/v1/subjects/my")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { haveLength(1) }
                jsonPath("$.data[0].id") { value(subject.id) }
                jsonPath("$.data[0].title") { value("test subject") }
                jsonPath("$.data[0].category") { value("OS") }
                jsonPath("$.data[0].maxScore") { value(null) }
                jsonPath("$.error") { value(null) }
            }

        // 채팅 API 조회 시 빈 값 응답 검증
        mockMvc
            .get("/api/v1/subjects/${subject.id}/chats")
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { isEmpty() }
                jsonPath("$.error") { isEmpty() }
            }

        val answer = "test answer"
        val nextQuestion =
            """
            답변에 대한 점수: 10점
            """.trimIndent().trimMargin()
        given { reactiveMemberChatService.answerAsync(any()) }.willReturn {
            Flux
                .create<ChatResponse?> {
                    it.next(
                        ChatResponse(listOf(Generation(answer))),
                    )
                    it.complete()
                }
                // 차단되지 않는 컨텍스트에서 호출을 차단하면 스레드 고갈이 발생할 수 있습니다.
                .publishOn(Schedulers.boundedElastic())
                .doOnComplete {
                    // NOTE: chatAppender.addAnswerAndNextQuestion() 호출
                    chatAppender.addAnswerAndNextQuestion(
                        subject = subject,
                        member = member,
                        answer = answer,
                        chats = emptyList(),
                        nextQuestion = nextQuestion,
                    )
                }
        }

        // 특정 주제에 대해 답변
        webTestClient
            .get()
            .uri("/api/v5/subjects/{subjectId}/answer?message={message}", subject.id, answer)
            .header("Authorization", "Bearer test-token")
            .exchange()
            .expectStatus()
            .isOk

        // 채팅 API 조회 - 답변과 다음 질문이 생성되었는지 검증
        val now = LocalDateTime.now()
        mockMvc
            .get("/api/v1/subjects/${subject.id}/chats")
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { haveLength(3) }
                jsonPath("$.data[0].type") { value("question") }
                jsonPath("$.data[0].message") {
                    value(
                        Subject(
                            title = "test subject",
                            question = "test question",
                            category = SubjectCategory.OS,
                        ).question,
                    )
                }
                jsonPath("$.data[0].score") { value(null) }
                jsonPath("$.data[0].createdAt") { value(null) }
                jsonPath("$.data[1].type") { value("answer") }
                jsonPath("$.data[1].message") { value(answer) }
                jsonPath("$.data[1].score") { value(10) }
                jsonPath("$.data[1].createdAt") { value(matcher = BeforeDateMatcher(now)) }
                jsonPath("$.data[2].type") { value("question") }
                jsonPath("$.data[2].message") { value(nextQuestion) }
                jsonPath("$.data[2].score") { value(null) }
                jsonPath("$.data[2].createdAt") { value(null) }
            }

        mockMvc
            .get("/api/v1/subjects/my")
            .andExpect {
                status { isOk() }
                jsonPath("$.data[0].maxScore") { value(10) }
            }

        // 채팅 아카이브
        mockMvc
            .post("/api/v2/subjects/${subject.id}/chats/archive")
            .andExpect {
                status { isCreated() }

                // NOTE: location 헤더 값은 알 수 없으므로 검증하지 않는다.
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { value(null) }
                jsonPath("$.error") { value(null) }
            }

        // 채팅 내역 재조회 - 아카이브 후 빈 값 응답 검증
        mockMvc
            .get("/api/v1/subjects/${subject.id}/chats")
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { isEmpty() }
            }

        mockMvc
            .get("/api/v1/subjects/my")
            .andExpect {
                status { isOk() }
                jsonPath("$.data[0].maxScore") { value(null) }
            }

        // NOTE: 추후 API 개발 완료 시 API 호출로 변경
        val archives = chatArchiveQueryRepository.findBySubjectAndMember(subject, member)
        archives.size shouldBe 1

        val archiveContents = chatArchiveContentQueryRepository.findByArchive(archives[0])
        archiveContents.size shouldBe 3
    }

    private fun createOAuth2AuthenticationToken(
        oauth2User: DefaultOAuth2User,
        provider: String = "google",
    ): OAuth2AuthenticationToken {
        val authentication =
            OAuth2AuthenticationToken(
                oauth2User,
                emptyList<GrantedAuthority>(),
                provider, // NOTE: 구글 외 다른 로그인 수단 추가 시 변경 필요
            )
        return authentication
    }

    private fun createOAuth2User(createdMember: Member): DefaultOAuth2User {
        val oauth2User =
            DefaultOAuth2User(
                emptyList<GrantedAuthority>(),
                mapOf(
                    "sub" to createdMember.id,
                    "name" to createdMember.name,
                    "email" to createdMember.email,
                ),
                "sub",
            )
        return oauth2User
    }
}

class BeforeDateMatcher(
    private val date: LocalDateTime,
) : BaseMatcher<LocalDateTime>() {
    override fun describeTo(description: Description?) {
        description?.appendText("date is $date")
    }

    /**
     * 생성자에서 받은 시간이 현재 시간보다 이전이면 true를 반환한다.
     */
    override fun matches(actual: Any?): Boolean {
        val now = LocalDateTime.now()
        return now.isAfter(date)
    }
}
