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
 * LANGGRAPH: Coder Node
 * Writes code based on research findings and review feedback
 *
 * LANGGRAPH Features demonstrated:
 * - State Persistence: Updates code in shared state
 * - Cyclic Workflows: Can receive input from Reviewer for revisions
 * - Multi-agent Coordination: Works with Researcher and Reviewer
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
public class CoderNode extends AgentNode {

    private final ChatLanguageModel chatModel;

    public CoderNode(@Value("${api.deepseek.url}") String apiUrl,
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
        setName("Coder");
        setType("CODER");
        setDescription("Writes code based on research and requirements");
        setPromptTemplate("You are a senior software engineer. Given the following information:\n\n" +
                "Requirements:\n%s\n\n" +
                "Research Findings:\n%s\n\n" +
                "%s\n\n" +
                "Write production-ready code that:\n" +
                "1. Implements the requirements correctly\n" +
                "2. Follows best practices\n" +
                "3. Includes error handling\n" +
                "4. Is well-documented\n\n" +
                "Provide the complete code implementation.");
        setNextNodes(new String[]{"Reviewer"});
        setAllowCycles(true); // LANGGRAPH: Cyclic Workflows - can be revisited
    }

    /**
     * Execute coding logic
     * LANGGRAPH: State Persistence - Updates shared state with code
     * LANGGRAPH: Cyclic Workflows - Can handle revisions from Reviewer
     * LANGSMITH: Tracing - This method call is traced
     */
    @Override
    public GraphState execute(GraphState state, String input) {
        log.info("LANGGRAPH: Executing Coder node for thread: {}, revision: {}",
                state.getThreadId(), state.getRevisionCount());

        try {
            // Build coding prompt
            String revisionContext = "";
            if (state.getRevisionCount() > 0 && state.getReviewComments() != null) {
                revisionContext = String.format(
                        "Previous Review Comments:\n%s\n\n" +
                        "Please address these comments and revise the code accordingly.",
                        state.getReviewComments());
            }

            String prompt = String.format(getPromptTemplate(),
                    state.getRequirements() != null ? state.getRequirements() : "No specific requirements",
                    state.getResearchFindings() != null ? state.getResearchFindings() : "No research available",
                    revisionContext);

            // LANGSMITH: Tracing - This LLM call is traced
            UserMessage userMessage = UserMessage.from(prompt);
            AiMessage aiMessage = chatModel.generate(userMessage).content();

            // Extract generated code
            String code = aiMessage.text();

            // LANGGRAPH: State Persistence - Update state with code
            Map<String, Object> updates = new HashMap<>();
            updates.put("generatedCode", code);

            state.updateState(getName(), updates);
            log.info("LANGGRAPH: Code generated. Length: {} chars", code.length());

        } catch (Exception e) {
            log.error("Error in Coder node: {}", e.getMessage(), e);
            Map<String, Object> updates = new HashMap<>();
            updates.put("errorMessage", "Code generation failed: " + e.getMessage());
            state.updateState(getName(), updates);
        }

        return state;
    }

    /**
     * Determine next node
     * LANGGRAPH: Always transitions to Reviewer
     */
    @Override
    public String determineNextNode(GraphState state) {
        if (state.getErrorMessage() != null) {
            return "END";
        }
        return "Reviewer";
    }
}
