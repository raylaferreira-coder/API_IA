package com.project.chat.repository;

import com.project.chat.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    List<Session> findByExpiredFalseAndLastActivityBefore(LocalDateTime dateTime);
}
