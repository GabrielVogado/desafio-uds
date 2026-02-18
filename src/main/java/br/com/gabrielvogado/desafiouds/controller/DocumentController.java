package br.com.gabrielvogado.desafiouds.controller;

import br.com.gabrielvogado.desafiouds.dto.DocumentCreateRequest;
import br.com.gabrielvogado.desafiouds.dto.DocumentDTO;
import br.com.gabrielvogado.desafiouds.model.Document;
import br.com.gabrielvogado.desafiouds.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentDTO> createDocument(
            @Valid @RequestBody DocumentCreateRequest request,
            Authentication authentication) {
        DocumentDTO response = documentService.createDocument(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocumentById(
            @PathVariable Long id,
            Authentication authentication) {
        DocumentDTO response = documentService.getDocumentById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentDTO>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Document.DocumentStatus status,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<DocumentDTO> response = documentService.listDocuments(
                authentication.getName(),
                title,
                status,
                pageable
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentCreateRequest request,
            Authentication authentication) {
        DocumentDTO response = documentService.updateDocument(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            Authentication authentication) {
        documentService.deleteDocument(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<DocumentDTO> changeDocumentStatus(
            @PathVariable Long id,
            @RequestParam Document.DocumentStatus status,
            Authentication authentication) {
        DocumentDTO response = documentService.changeStatus(id, status, authentication.getName());
        return ResponseEntity.ok(response);
    }
}

