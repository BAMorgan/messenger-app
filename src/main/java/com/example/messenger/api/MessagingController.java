package com.example.messenger.api;

import com.example.messenger.domain.AppUser;
import com.example.messenger.domain.Conversation;
import com.example.messenger.domain.Message;
import com.example.messenger.dto.*;
import com.example.messenger.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MessagingController {
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final MessageService service;

    public MessagingController(MessageService service) {
        this.service = service;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummary>> listConversations(@AuthenticationPrincipal AppUser currentUser) {
        List<ConversationSummary> conversations = service.listConversationsForUser(currentUser.getId());
        return ResponseEntity.ok(conversations);
    }

    @PostMapping("/conversations")
    public ResponseEntity<Conversation> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        Conversation conversation = service.createConversation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        String idempotencyKey = request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()
                ? request.getIdempotencyKey() : null;
        Message message = service.sendMessage(id, currentUser.getId(), request.getBody(), idempotencyKey);
        MessageService.MessageView view = service.toMessageView(message);
        MessageResponse response = new MessageResponse(
                view.id(),
                view.senderId(),
                view.senderUsername(),
                view.body(),
                view.createdAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<PaginatedResponse<MessageResponse>> listMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        MessageService.MessageListPage page = service.listMessages(id, currentUser.getId(), cursor, pageSize);
        List<MessageResponse> items = page.messages().stream()
                .map(v -> new MessageResponse(v.id(), v.senderId(), v.senderUsername(), v.body(), v.createdAt()))
                .toList();
        return ResponseEntity.ok(new PaginatedResponse<>(items, page.nextCursor()));
    }
}
