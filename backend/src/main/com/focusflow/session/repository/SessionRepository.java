package com.focusflow.session.repository;

import com.focusflow.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findAllByOrderByStartTimeDesc();

    List<Session> findByTagOrderByStartTimeDesc(String tag);

    @Query("SELECT s FROM Session s WHERE s.startTime >= :startOfDay AND s.startTime < :endOfDay")
    List<Session> findTodaySessions(@Param("startOfDay") LocalDateTime startOfDay,
                                    @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT s FROM Session s WHERE s.completed = true AND s.startTime >= :start AND s.startTime < :end ORDER BY s.startTime DESC")
    List<Session> findCompletedSessionsBetween(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT DATE(s.startTime) FROM Session s WHERE s.completed = true ORDER BY DATE(s.startTime)")
    List<java.sql.Date> findDistinctCompletedDays();

    @Query("SELECT s.tag, COUNT(s), SUM(s.durationMinutes) FROM Session s WHERE s.completed = true GROUP BY s.tag ORDER BY SUM(s.durationMinutes) DESC")
    List<Object[]> findTagStats();

    long countByCompleted(boolean completed);
}
