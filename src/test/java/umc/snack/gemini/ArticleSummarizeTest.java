package umc.snack.gemini;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import umc.snack.global.gemini.GeminiService;

@SpringBootTest
public class ArticleSummarizeTest {

    @Autowired
    private GeminiService geminiService;

    String articleContent = "[이코노미스트 이병희 기자] 국내 금융사의 절반가량이 아마존웹서비스(AWS)를 사용하는 것으로 나타났다. 노경훈 AWS 금융 사업부 총괄은 16일 서울 역삼 센터필드에서 열린 'AWS의 글로벌 금융 사업 전략 및 IDC한국 금융권 클라우드 도입 현황' 기자간담회에서 이같이 밝혔다.\n" +
            "\n" +
            "AWS가 시장조사업체 IEC와 한국 금융권의 클라우드 이용 현황 연구를 실시한 결과 한국 전체 금융기관의 92%가 퍼블릭 클라우드를 사용한 것으로 집계됐다. 퍼블릭 클라우드는 인터넷으로 누구나 사용할 수 있도록 공개된 클라우드 컴퓨팅 서비스로 서버나 장비를 직접 살 필요가 없어서 초기 비용이 들지 않는다.\n" +
            "\n" +
            "주목할 점은 응답한 금융사의 53%가 퍼블릭 클라우드 서비스로 AWS를 선택했다는 점이다. 금융사들은 클라우드 사업자 선택 시 보안 규정 준수(42%), 가용성(안정성)과 재해 복구 기능(41%), 제품 로드맵(39%) 등 요인을 고려하는 것으로 나타났다.\n" +
            "\n" +
            "AWS는 한국 금융권이 AI 도입 시 활용 현황으로 서류 심사 등 규제 준수 자동화, 금융사기 예방, 맞춤형 마케팅, 고급 데이터 분석 등을 꼽았다.\n" +
            "\n" +
            "차 실장에 따르면 케이뱅크는 멀티 클라우드 기반 앱 뱅킹, 마이크로서비스 아키텍처(MSA), 데이터 레이크하우스 등 세 가지 영역에서 클라우드 전환을 추진하고 있다.\n" +
            "\n" +
            "차 실장은 \"클라우드 전환은 단순한 인프라 이전이 아니라 운영 효율성과 기술 주도권을 위한 전략적 디지털 전환 과정\"이라며 \"하이브리드와 멀티 클라우드 아키텍처로 기술 역량 강화에 속도를 내고 있다\"고 말했다.";

    String prompt = """
            아래 뉴스 기사를 700글자 이내로 간결하게 요약해줘.
            기사 내용을 바탕으로 4지선다 객관식 퀴즈 2개를 만들어줘.
            각 퀴즈는 반드시 [문제, 보기 4개(choices), 정답(answer), 해설(explanation)]을 포함해줘.
            정답은 "{보기번호}. {보기내용}" 형식으로 되어야해.
            기사에서 중고등학생이 이해하기 어려울 만한 단어 4개를 뽑아줘.
            
            결과는 반드시 아래 JSON 형식으로만 응답해.
            
            {
              "summary": "...",
              "quizzes": [
                {
                  "question": "...",
                  "choices": ["...", "...", "...", "..."],
                  "answer": "...",
                  "explanation": "..."
                }
              ],
              "difficult_words": [
                { "word": "...", "meaning": "..." }
              ]
            }
            
            기사 본문:
            """ + articleContent;

    @Test
    void getCompletion() {
        String result = geminiService.getCompletion(prompt, "gemini-2.5-pro");
        System.out.println(result);
    }
}
