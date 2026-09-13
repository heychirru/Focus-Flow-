package com.focusflow.study.repository;

import com.focusflow.study.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findAllByOrderByCreatedAtDesc();

    @Query("SELECT d FROM Deck d LEFT JOIN FETCH d.flashcards WHERE d.id = :id")
    Optional<Deck> findByIdWithCards(@Param("id") Long id);
}
