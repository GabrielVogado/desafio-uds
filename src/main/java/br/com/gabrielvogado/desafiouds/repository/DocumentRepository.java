package br.com.gabrielvogado.desafiouds.repository;

import br.com.gabrielvogado.desafiouds.model.Document;
import br.com.gabrielvogado.desafiouds.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Page<Document> findByOwner(User owner, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND d.status = :status")
    Page<Document> findByOwnerAndStatus(@Param("owner") User owner, @Param("status") Document.DocumentStatus status, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    Page<Document> findByOwnerAndTitleContaining(@Param("owner") User owner, @Param("title") String title, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.owner = :owner AND LOWER(d.title) LIKE LOWER(CONCAT('%', :title, '%')) AND d.status = :status")
    Page<Document> findByOwnerAndTitleContainingAndStatus(@Param("owner") User owner, @Param("title") String title, @Param("status") Document.DocumentStatus status, Pageable pageable);
}

