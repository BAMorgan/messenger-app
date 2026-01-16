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
            @RequestParam String body
    ) {
       return service.sendMessage(conversationId, senderId, body);
    }

    @GetMapping("/conversations/{id}/messages")
    public List<MessageService.MessageView> list(@PathVariable Long id){
        return service.listMessages(id);
    }

}
