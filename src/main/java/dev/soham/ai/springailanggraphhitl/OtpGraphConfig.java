package dev.soham.ai.springailanggraphhitl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.InterruptableAction;
import org.bsc.langgraph4j.action.InterruptionMetadata;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OtpGraphConfig {
    private final ChatClient chatClient;


    public NodeAction<OtpAuthenticationFlowState> getUserPhoneNumber() {
        return state -> {
            String userMessage = state.getUserMessagePhone();
            var phone = chatClient.prompt().system("Extract Phone number from user message")
                    .user(userMessage)
                    .call().entity(UserInputMobile.class);
            log.info("Extracted phone number: {}", phone);
            return java.util.Map.of(OtpAuthenticationFlowState.PHONE_NUMBER, phone.mobile());
        };
    }

    public NodeAction<OtpAuthenticationFlowState> generateOtp() {
        return state -> {
            var otp = String.valueOf((int) (Math.random() * 9000) + 1000);
            log.info("Generated OTP: for phone number:{} {}", otp, state.getPhoneNumber());
            return java.util.Map.of(OtpAuthenticationFlowState.OTP, otp);
        };
    }

    public NodeAction<OtpAuthenticationFlowState> extractOtp() {
        return state -> {
            String userOtpInputMessage = state.getUserOtpInput();
            var userOtpInput = chatClient.prompt().system("Extract OTP from user message")
                    .user(userOtpInputMessage)
                    .call().entity(UserInputOtp.class);
            log.info("Extracted user OTP input: {}", userOtpInput);
            return java.util.Map.of(OtpAuthenticationFlowState.USER_OTP, userOtpInput.otp(),OtpAuthenticationFlowState.RETRY_COUNT, state.retryCount() + 1);
        };
    }

    public NodeAction<OtpAuthenticationFlowState> successNode() {
        return state -> {
            log.info("OTP verification successful for phone number: {}", state.getPhoneNumber());
            return java.util.Map.of(OtpAuthenticationFlowState.RESULT, "OTP verification successful",OtpAuthenticationFlowState.CONTROLLER_MESSAGE,"OTP verification successful");
        };
    }

    public NodeAction<OtpAuthenticationFlowState> failureNode() {
        return state -> {
            log.info("OTP verification failed for phone number: {}", state.getPhoneNumber());
            return java.util.Map.of(OtpAuthenticationFlowState.RESULT, "OTP verification failed",OtpAuthenticationFlowState.CONTROLLER_MESSAGE,"OTP verification failed!");
        };
    }


    @Bean
    public CompiledGraph<OtpAuthenticationFlowState> otpAuthenticationGraph() throws GraphStateException {
        CompileConfig compileConfig = CompileConfig.builder()
                .checkpointSaver(new MemorySaver())
                .interruptBefore("extractOtpNode")
                .build();


        StateGraph<OtpAuthenticationFlowState> graph = new StateGraph<>(Map.of(), OtpAuthenticationFlowState::new)
                .addNode("extractPhoneNumberNode", AsyncNodeAction.node_async(getUserPhoneNumber()))
                .addNode("generateOtpNode", AsyncNodeAction.node_async(generateOtp()))
                .addNode("extractOtpNode", AsyncNodeAction.node_async(extractOtp()))
                .addNode("successNode", AsyncNodeAction.node_async(successNode()))
                .addNode("failureNode", AsyncNodeAction.node_async(failureNode()))
                .addEdge(StateGraph.START, "extractPhoneNumberNode")
                .addEdge("extractPhoneNumberNode", "generateOtpNode")
                .addEdge("generateOtpNode", "extractOtpNode")
                .addConditionalEdges("extractOtpNode", edge_async(state -> {
                    String generatedOtp = state.getOtp();
                    String userInputOtp = state.userOtp();
                    log.info("Comparing generated OTP: {} with user input OTP: {}", generatedOtp, userInputOtp);
                    if (generatedOtp.equals(userInputOtp)) {
                        return "successNode";
                    } else {

                        return state.retryCount() < 3 ? "extractOtpNode" : "failureNode";
                    }
                }), Map.of(
                        "successNode", "successNode",
                        "failureNode", "failureNode",
                        "extractOtpNode", "extractOtpNode"
                ))
                .addEdge("successNode", StateGraph.END)
                .addEdge("failureNode", StateGraph.END);
        return graph.compile(compileConfig);
    }
}
