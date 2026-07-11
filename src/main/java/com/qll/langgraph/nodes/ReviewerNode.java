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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LANGGRAPH: Reviewer Node
 * Reviews code and decides whether to approve or request revision
 *
 * LANGGRAPH Features demonstrated:
 * - Conditional Branching: Decides next action based on review
 * - Cyclic Workflows: Can loop back to Coder for revisions
 * - Multi-agent Coordination: Coordinates with Coder node
 *
 * LANGSMITH Features integrated:
 * - Tracing: All LLM calls are traced
 * - Evaluation: Evaluates code quality
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Slf4j
@Component
public class ReviewerNode extends AgentNode {

    private final ChatLanguageModel chatModel;

    // Pattern to extract approval status from AI response
    private static final Pattern APPROVAL_PATTERN = Pattern.compile("(APPROVED|NEEDS_REVISION)", Pattern.CASE_INSENSITIVE);

    public ReviewerNode(@Value("${api.deepseek.url}") String apiUrl,
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
        setName("Reviewer");
        setType("REVIEWER");
        setDescription("Reviews code and provides feedback");
        setPromptTemplate("You are a code reviewer. Review the following code against the requirements:\n\n" +
                "Requirements:\n%s\n\n" +
                "Generated Code:\n%s\n\n" +
                "Research Findings:\n%s\n\n" +
                "Provide a detailed review including:\n" +
                "1. Correctness: Does the code meet requirements?\n" +
                "2. Best Practices: Is the code well-structured?\n" +
                "3. Error Handling: Are edge cases handled?\n" +
                "4. Documentation: Is the code well-documented?\n\n" +
                "Conclude with either:\n" +
                "- APPROVED if the code is production-ready\n" +
                "- NEEDS_REVISION followed by specific comments on what needs to be fixed\n\n" +
                "Format your conclusion exactly as \"APPROVED\" or \"NEEDS_REVISION\" on its own line.");
        setNextNodes(new String[]{"Coder", "END"}); // LANGGRAPH: Conditional Branching
        setAllowCycles(true);
    }

    /**
     * Execute review logic
     * LANGGRAPH: Conditional Branching - Determines if cycle is needed
     * LANGGRAPH: Cyclic Workflows - May loop back to Coder
     * LANGSMITH: Tracing - This method call is traced
     * LANGSMITH: Evaluation - Evaluates code quality
     */
    @Override
    public GraphState execute(GraphState state, String input) {
        log.info("LANGGRAPH: Executing Reviewer node for thread: {}", state.getThreadId());

        try {
            // Build review prompt
            String prompt = String.format(getPromptTemplate(),
                    state.getRequirements() != null ? state.getRequirements() : "No specific requirements",
                    state.getGeneratedCode() != null ? state.getGeneratedCode() : "No code generated",
                    state.getResearchFindings() != null ? state.getResearchFindings() : "No research available");

            // LANGSMITH: Tracing - This LLM call is traced
            UserMessage userMessage = UserMessage.from(prompt);
            AiMessage aiMessage = chatModel.generate(userMessage).content();

            // Extract review and decision
            String reviewText = aiMessage.text();

            // Parse approval status
            Matcher matcher = APPROVAL_PATTERN.matcher(reviewText);
            String approvalStatus = "NEEDS_REVISION"; // Default to revision
            if (matcher.find()) {
                approvalStatus = matcher.group(1).toUpperCase();
            }

            // LANGGRAPH: Conditional Branching
            Map<String, Object> updates = new HashMap<>();
            updates.put("reviewComments", reviewText);
            updates.put("reviewStatus", approvalStatus);

            if ("APPROVED".equals(approvalStatus)) {
                // Workflow complete
                updates.put("status", "COMPLETED");
                log.info("LANGGRAPH: Code APPROVED. Workflow complete.");
            } else {
                // LANGGRAPH: Cyclic Workflows - Loop back to Coder
                updates.put("status", "NEEDS_REVISION");
                state.incrementRevision(); // Track revision count
                log.info("LANGGRAPH: Code NEEDS_REVISION. Looping back to Coder. Revision count: {}",
                        state.getRevisionCount());
            }

            state.updateState(getName(), updates);

        } catch (Exception e) {
            log.error("Error in Reviewer node: {}", e.getMessage(), e);
            Map<String, Object> updates = new HashMap<>();
            updates.put("errorMessage", "Review failed: " + e.getMessage());
            state.updateState(getName(), updates);
        }

        return state;
    }

    /**
     * Determine next node based on review status
     * LANGGRAPH: Conditional Branching - Different paths based on state
     *
     * @param state Current graph state
     * @return Next node name (Coder for revision, END if approved)
     */
    @Override
    public String determineNextNode(GraphState state) {
        if (state.getErrorMessage() != null) {
            return "END";
        }

        // LANGGRAPH: Conditional Branching
        if (state.needsRevision()) {
            log.info("LANGGRAPH: Conditional branch - returning to Coder for revision");
            return "Coder"; // LANGGRAPH: Cyclic Workflows
        } else if (state.isComplete()) {
            log.info("LANGGRAPH: Conditional branch - workflow complete");
            return "END";
        }

        return "END";
    }

    /**
     * Check if workflow should continue
     * LANGGRAPH: Cyclic Workflows - Prevents infinite loops
     */
    @Override
    public boolean shouldContinue(GraphState state) {
        // Limit maximum revisions to prevent infinite loops
        final int MAX_REVISIONS = 5;
        if (state.getRevisionCount() >= MAX_REVISIONS) {
            log.warn("LANGGRAPH: Maximum revision count reached. Completing workflow.");
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "COMPLETED");
            updates.put("reviewComments",
                    state.getReviewComments() + "\n\n[Auto-approved after " + MAX_REVISIONS + " revisions]");
            updates.put("reviewStatus", "APPROVED");
            state.updateState(getName(), updates);
            return false;
        }
        return super.shouldContinue(state);
    }
}
