package com.qll.repository;

import com.qll.model.entities.ThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Thread entity
 * Supports LANGGRAPH: State Persistence
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Repository
public interface ThreadRepository extends JpaRepository<ThreadEntity, String> {

    /**
     * Find thread by status
     */
    java.util.List<ThreadEntity> findByStatus(String status);

    /**
     * Find active threads
     */
    default java.util.List<ThreadEntity> findActiveThreads() {
        return findByStatus("ACTIVE");
    }
}
