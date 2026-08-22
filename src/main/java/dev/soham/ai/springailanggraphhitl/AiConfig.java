package dev.soham.ai.springailanggraphhitl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .build();
    }

    @Bean
    public ChatClientBuilderCustomizer systyemPromptCustomizer() {
        return builder -> builder.defaultSystem("You are a helpful assistant that provides information");
    }

    @Bean
    public ChatClientBuilderCustomizer memoryCustomizer() {
        return builder-> MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(15).build()).build();

    }

}
