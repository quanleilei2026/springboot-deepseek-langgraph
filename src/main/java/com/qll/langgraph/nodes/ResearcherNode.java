package com.qll.langgraph.nodes;

import com.qll.model.AgentNode;
import com.qll.model.GraphState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LANGGRAPH: Researcher Node
 * Gathers technical requirements and researches APIs/libraries
 *
 * LANGGRAPH Features demonstrated:
 * - Multi-agent Coordination: Works with other nodes
 * - State Persistence: Updates shared state with findings
 *
 * LANGSMITH Features integrated:
 * - Tracing: All LLM calls are traced
 * - Token usage: Tracks tokens consumed
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Slf4j
@Component
public class ResearcherNode extends AgentNode {

    private final ChatLanguageModel chatModel;

    public ResearcherNode(@Value("${api.deepseek.url}") String apiUrl,
                          @Value("${api.deepseek.key}") String apiKey,
                          @Value("${api.deepseek.model}") String model,
                          @Value("${api.deepseek.timeout}") Integer timeout,
                          @Value("${api.deepseek.max-tokens}") Integer maxTokens,
                          @Value("${api.deepseek.temperature}") Double temperature) {

        // Initialize DeepSeek chat model
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(apiUrl)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(java.time.Duration.ofMillis(timeout))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        // Configure node properties
        setName("Researcher");
        setType("RESEARCHER");
        setDescription("Researches technical requirements and APIs");
        setPromptTemplate("You are a technical researcher. Given the following requirements:\n%s\n\n" +
                "Research and provide:\n" +
                "1. Required APIs and libraries\n" +
                "2. Best practices and patterns\n" +
                "3. Potential challenges and solutions\n" +
                "4. Code structure recommendations\n\n" +
                "Be specific and practical.");
        setNextNodes(new String[]{"Coder"});
        setAllowCycles(false);
    }

    /**
     * Execute research logic
     * LANGGRAPH: State Persistence - Updates shared state
     * LANGSMITH: Tracing - This method call is traced
     */
    @Override
    public GraphState execute(GraphState state, String input) {
        log.info("LANGGRAPH: Executing Researcher node for thread: {}", state.getThreadId());

        try {
            // Build research prompt
            String prompt = String.format(getPromptTemplate(),
                    state.getRequirements() != null ? state.getRequirements() : input);

            // LANGSMITH: Tracing - This LLM call is traced
            UserMessage userMessage = UserMessage.from(prompt);
            AiMessage aiMessage = chatModel.generate(userMessage).content();

            // Extract research findings
            String findings = aiMessage.text();

            // LANGGRAPH: State Persistence - Update state with findings
            Map<String, Object> updates = new HashMap<>();
            updates.put("researchFindings", findings);

            state.updateState(getName(), updates);
            log.info("LANGGRAPH: Research completed. Findings: {}", findings.substring(0, Math.min(100, findings.length())));

        } catch (Exception e) {
            log.error("Error in Researcher node: {}", e.getMessage(), e);
            Map<String, Object> updates = new HashMap<>();
            updates.put("errorMessage", "Research failed: " + e.getMessage());
            state.updateState(getName(), updates);
        }

        return state;
    }

    /**
     * Determine next node
     * LANGGRAPH: Always transitions to Coder
     */
    @Override
    public String determineNextNode(GraphState state) {
        if (state.getErrorMessage() != null) {
            return "END";
        }
        return "Coder";
    }
}
