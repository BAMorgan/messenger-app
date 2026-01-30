package com.example.messenger.api;

import com.example.messenger.domain.Message;
import com.example.messenger.domain.AppUser;
import com.example.messenger.domain.Conversation;
import com.example.messenger.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") //everyhting in this controler get /api prefix
public class MessagingController {
    private static final int DEFAULT_PAGE_SIZE = 50;

    private final MessageService service;

    public MessagingController(MessageService service) {
        this.service = service;
    }

    @PostMapping("/users")
    public AppUser createUser(@RequestParam String username){
        return service.createUser(username);
    }

    @PostMapping("/conversations")
    public Conversation createConversation(
            @RequestParam Long userA, //POST /api/users?username=timmy
            @RequestParam Long userB
    ){
      return service.createConversation(userA,userB);
    }

    @PostMapping("/messages")
    public Message sendMessage(
            @RequestParam Long conversationId,
            @RequestParam Long senderId,
            @RequestParam String body,
            @RequestParam(required = false) String idempotencyKey
    ) {
       return service.sendMessage(conversationId, senderId, body, idempotencyKey);
    }

    @GetMapping("/conversations/{id}/messages")
    public Object list(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit
    ) {
        if (cursor != null || (limit != null && limit > 0)) {
            int pageSize = limit != null && limit > 0 ? Math.min(limit, 100) : DEFAULT_PAGE_SIZE;
            return service.listMessages(id, cursor, pageSize);
        }
        return service.listMessages(id);
    }
}
