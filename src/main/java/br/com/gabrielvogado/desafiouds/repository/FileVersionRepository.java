package br.com.gabrielvogado.desafiouds.repository;

import br.com.gabrielvogado.desafiouds.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    @Query("SELECT fv FROM FileVersion fv WHERE fv.document.id = :documentId ORDER BY fv.uploadedAt DESC")
    List<FileVersion> findByDocumentIdOrderByUploadedAtDesc(@Param("documentId") Long documentId);

    @Query(value = "SELECT * FROM file_versions fv WHERE fv.document_id = :documentId ORDER BY fv.uploaded_at DESC LIMIT 1", nativeQuery = true)
    Optional<FileVersion> findLatestByDocumentId(@Param("documentId") Long documentId);
}

