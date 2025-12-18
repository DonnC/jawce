package org.dcl.jawce.server.repository;

import org.dcl.jawce.server.model.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByChatIdOrderByTimestampAsc(String chatId);
}
