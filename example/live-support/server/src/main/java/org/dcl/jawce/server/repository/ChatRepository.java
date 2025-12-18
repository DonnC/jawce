package org.dcl.jawce.server.repository;

import org.dcl.jawce.server.constant.ChatStatus;
import org.dcl.jawce.server.model.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByStatusNot(ChatStatus status);

    Optional<Chat> findByCustomerPhoneAndStatus(String customerPhone, ChatStatus status);
}
