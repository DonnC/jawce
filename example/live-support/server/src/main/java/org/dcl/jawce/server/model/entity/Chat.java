package org.dcl.jawce.server.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.dcl.jawce.server.constant.ChatStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private LocalDateTime createdAt;

    private String customerPhone;
    private String customerName;

    private String sessionId;

    @Enumerated(EnumType.STRING)
    private ChatStatus status = ChatStatus.PENDING;

    private String assignedAgent;
    private String lastMessage;
    private LocalDateTime lastMessageTime;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Message> messages;

}
