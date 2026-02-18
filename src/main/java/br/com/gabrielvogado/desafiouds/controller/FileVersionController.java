package br.com.gabrielvogado.desafiouds.controller;

import br.com.gabrielvogado.desafiouds.dto.FileVersionDTO;
import br.com.gabrielvogado.desafiouds.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
public class FileVersionController {

    @Autowired
    private FileService fileService;

    @PostMapping("/{documentId}/versions/upload")
    public ResponseEntity<FileVersionDTO> uploadFile(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        FileVersionDTO response = fileService.uploadFile(documentId, file, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{documentId}/versions")
    public ResponseEntity<List<FileVersionDTO>> getVersionHistory(
            @PathVariable Long documentId,
            Authentication authentication) {
        List<FileVersionDTO> response = fileService.getVersionHistory(documentId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/versions/latest")
    public ResponseEntity<FileVersionDTO> getLatestVersion(
            @PathVariable Long documentId,
            Authentication authentication) {
        FileVersionDTO response = fileService.getLatestVersion(documentId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long versionId,
            Authentication authentication) throws IOException {
        byte[] fileContent = fileService.downloadFile(versionId, authentication.getName());

        // Get file info to set proper filename in response
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("file")
                        .build()
                        .toString())
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .body(fileContent);
    }

    @DeleteMapping("/versions/{versionId}")
    public ResponseEntity<Void> deleteFileVersion(
            @PathVariable Long versionId,
            Authentication authentication) {
        fileService.deleteFileVersion(versionId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

