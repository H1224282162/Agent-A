package org.example.Controller;

import org.example.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public String chat(
            @RequestParam String sessionId,
            @RequestParam String modelType,
            @RequestParam String msg
    ) {
        return chatService.chatSync(sessionId, modelType, msg);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestParam String sessionId,
            @RequestParam String modelType,
            @RequestParam String msg
    ) {
        return chatService.chatStream(sessionId, modelType, msg);
    }



    @GetMapping("/chat/sessions")
    public List<String> getAllSessions(){
        return null;
    }


}
