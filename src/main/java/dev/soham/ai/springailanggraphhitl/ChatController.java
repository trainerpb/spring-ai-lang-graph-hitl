package dev.soham.ai.springailanggraphhitl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatClient chatClient;
    private final CompiledGraph<OtpAuthenticationFlowState> otpGraph;

    @GetMapping(value="/start-otp-flow",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startOtpFlow(@RequestParam("q") String userMessage) throws IOException {
        SseEmitter emitter = new SseEmitter();
        var uuid=java.util.UUID.randomUUID().toString();
        RunnableConfig runnableConfig=RunnableConfig.builder().threadId(uuid).build();
        AsyncGenerator.Cancellable<NodeOutput<OtpAuthenticationFlowState>> stream = otpGraph.stream(Map.of(OtpAuthenticationFlowState.USER_PHONE_INPUT, userMessage), runnableConfig);
        String messageToClient = "Processing..."+uuid;
        emitter.send(SseEmitter.event().data(messageToClient));
        for (var stepResult : stream) {

            GraphResult result = GraphResult.from(stepResult);

            // Check if the result is state data
            if (result.isStateData()) {
                Map<String, Object> currentState = result.asStateData();

                // Extract your custom message to log or send back in your HTTP response!
                String message = (String) currentState.get(OtpAuthenticationFlowState.CONTROLLER_MESSAGE);
                if (message != null) {
                    System.out.println("Message for Controller: " + message);
                    messageToClient=message;
                    emitter.send(SseEmitter.event().data(message));
                }
            }
        }
        return emitter;
    }

    @GetMapping("/resume-otp-flow/{threadId}")
    public String resumeOtpFlow(@PathVariable("threadId") String threadId, @RequestParam("userOtpInput") String userOtpInput) {
        RunnableConfig runnableConfig=RunnableConfig.builder().threadId(threadId).build();
        var result=otpGraph.invoke(GraphInput.resume(Map.of(OtpAuthenticationFlowState.USER_OTP_INPUT,userOtpInput)),runnableConfig);
        return result.get().result();
    }
}
