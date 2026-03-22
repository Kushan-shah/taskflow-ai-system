package com.taskmanager.repository;

import com.taskmanager.entity.Task;
import com.taskmanager.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // For scheduler: find overdue tasks
    @Query("SELECT t FROM Task t WHERE t.dueDate < :today AND t.status <> :doneStatus AND t.deleted = false")
    List<Task> findOverdueTasks(@Param("today") LocalDate today, @Param("doneStatus") TaskStatus doneStatus);

    // For dashboard: count tasks by status for a user
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.status = :status AND t.deleted = false")
    long countByUserAndStatus(@Param("userId") Long userId, @Param("status") TaskStatus status);

    // For dashboard: count overdue tasks for a user
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.dueDate < :today AND t.status <> :doneStatus AND t.deleted = false")
    long countOverdueByUser(@Param("userId") Long userId, @Param("today") LocalDate today, @Param("doneStatus") TaskStatus doneStatus);

    // For search: keyword search on title and description
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.deleted = false AND " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Task> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}
