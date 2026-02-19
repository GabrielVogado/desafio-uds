package br.com.gabrielvogado.desafiouds.service;

import br.com.gabrielvogado.desafiouds.dto.DocumentCreateRequest;
import br.com.gabrielvogado.desafiouds.dto.DocumentDTO;
import br.com.gabrielvogado.desafiouds.exception.DocumentNotFoundException;
import br.com.gabrielvogado.desafiouds.exception.UnauthorizedException;
import br.com.gabrielvogado.desafiouds.model.Document;
import br.com.gabrielvogado.desafiouds.model.User;
import br.com.gabrielvogado.desafiouds.repository.DocumentRepository;
import br.com.gabrielvogado.desafiouds.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @CacheEvict(value = "documents", allEntries = true)
    @Transactional
    public DocumentDTO createDocument(DocumentCreateRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .tags(request.getTags())
                .owner(owner)
                .status(Document.DocumentStatus.DRAFT)
                .build();

        Document savedDocument = documentRepository.save(document);
        return mapToDTO(savedDocument);
    }

    @Cacheable(value = "documents", key = "#id")
    @Transactional(readOnly = true)
    public DocumentDTO getDocumentById(Long id, String username) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to access this document");
        }

        return mapToDTO(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentDTO> listDocuments(String username, String title, Document.DocumentStatus status, Pageable pageable) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Page<Document> documents;

        if (title != null && !title.isEmpty() && status != null) {
            documents = documentRepository.findByOwnerAndTitleContainingAndStatus(owner, title, status, pageable);
        } else if (title != null && !title.isEmpty()) {
            documents = documentRepository.findByOwnerAndTitleContaining(owner, title, pageable);
        } else if (status != null) {
            documents = documentRepository.findByOwnerAndStatus(owner, status, pageable);
        } else {
            documents = documentRepository.findByOwner(owner, pageable);
        }

        return documents.map(this::mapToDTO);
    }

    @CachePut(value = "documents", key = "#id")
    @CacheEvict(value = "documents", allEntries = true)
    @Transactional
    public DocumentDTO updateDocument(Long id, DocumentCreateRequest request, String username) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to update this document");
        }

        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setTags(request.getTags());

        Document updatedDocument = documentRepository.save(document);
        return mapToDTO(updatedDocument);
    }

    @CacheEvict(value = "documents", key = "#id")
    @Transactional
    public void deleteDocument(Long id, String username) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to delete this document");
        }

        documentRepository.delete(document);
    }

    @Transactional
    public DocumentDTO changeStatus(Long id, Document.DocumentStatus status, String username) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to change status of this document");
        }

        document.setStatus(status);
        Document updatedDocument = documentRepository.save(document);
        return mapToDTO(updatedDocument);
    }

    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .tags(document.getTags())
                .ownerUsername(document.getOwner().getUsername())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    private boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRole() == User.UserRole.ADMIN)
                .orElse(false);
    }
}

