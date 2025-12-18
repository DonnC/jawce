package org.dcl.jawce.server.repository;

import org.dcl.jawce.server.model.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, String> {
}
