package br.com.gabrielvogado.desafiouds.service;

import br.com.gabrielvogado.desafiouds.dto.FileVersionDTO;
import br.com.gabrielvogado.desafiouds.exception.DocumentNotFoundException;
import br.com.gabrielvogado.desafiouds.exception.InvalidFileException;
import br.com.gabrielvogado.desafiouds.exception.UnauthorizedException;
import br.com.gabrielvogado.desafiouds.model.Document;
import br.com.gabrielvogado.desafiouds.model.FileVersion;
import br.com.gabrielvogado.desafiouds.model.User;
import br.com.gabrielvogado.desafiouds.repository.DocumentRepository;
import br.com.gabrielvogado.desafiouds.repository.FileVersionRepository;
import br.com.gabrielvogado.desafiouds.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FileVersionRepository fileVersionRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.storage.path:./uploads}")
    private String storagePath;

    @Value("${file.max-size:10485760}")
    private long maxFileSize;

    @Value("${file.allowed-types:application/pdf,image/png,image/jpeg}")
    private String allowedTypes;

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    @Transactional
    public FileVersionDTO uploadFile(Long documentId, MultipartFile file, String username) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + documentId));

        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to upload files to this document");
        }

        validateFile(file);

        try {
            String fileKey = generateFileKey();
            Path storageDirPath = Paths.get(storagePath);

            if (!Files.exists(storageDirPath)) {
                Files.createDirectories(storageDirPath);
            }

            Path filePath = storageDirPath.resolve(fileKey);
            Files.write(filePath, file.getBytes());

            User uploadedBy = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            FileVersion fileVersion = FileVersion.builder()
                    .document(document)
                    .fileKey(fileKey)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedBy(uploadedBy)
                    .build();

            FileVersion savedVersion = fileVersionRepository.save(fileVersion);
            return mapToDTO(savedVersion);

        } catch (IOException e) {
            throw new InvalidFileException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public FileVersionDTO getLatestVersion(Long documentId, String username) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + documentId));

        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to access this document");
        }

        FileVersion fileVersion = fileVersionRepository.findLatestByDocumentId(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("No file version found for document: " + documentId));

        return mapToDTO(fileVersion);
    }

    @Transactional(readOnly = true)
    public List<FileVersionDTO> getVersionHistory(Long documentId, String username) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + documentId));

        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to access this document");
        }

        List<FileVersion> versions = fileVersionRepository.findByDocumentIdOrderByUploadedAtDesc(documentId);
        return versions.stream().map(this::mapToDTO).toList();
    }

    @Transactional(readOnly = true)
    public byte[] downloadFile(Long versionId, String username) throws IOException {
        FileVersion fileVersion = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new DocumentNotFoundException("File version not found with id: " + versionId));

        Document document = fileVersion.getDocument();
        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to download this file");
        }

        Path filePath = Paths.get(storagePath).resolve(fileVersion.getFileKey());
        if (!Files.exists(filePath)) {
            throw new InvalidFileException("File not found on disk: " + fileVersion.getFileKey());
        }

        return Files.readAllBytes(filePath);
    }

    @Transactional
    public void deleteFileVersion(Long versionId, String username) {
        FileVersion fileVersion = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new DocumentNotFoundException("File version not found with id: " + versionId));

        Document document = fileVersion.getDocument();
        if (!document.getOwner().getUsername().equals(username) && !isAdmin(username)) {
            throw new UnauthorizedException("You don't have permission to delete this file");
        }

        try {
            Path filePath = Paths.get(storagePath).resolve(fileVersion.getFileKey());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            throw new InvalidFileException("Failed to delete file: " + e.getMessage(), e);
        }

        fileVersionRepository.delete(fileVersion);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidFileException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException("File type not allowed. Allowed types: PDF, PNG, JPEG");
        }
    }

    private String generateFileKey() {
        return UUID.randomUUID().toString();
    }

    private FileVersionDTO mapToDTO(FileVersion fileVersion) {
        return FileVersionDTO.builder()
                .id(fileVersion.getId())
                .documentId(fileVersion.getDocument().getId())
                .fileName(fileVersion.getFileName())
                .contentType(fileVersion.getContentType())
                .fileSize(fileVersion.getFileSize())
                .uploadedByUsername(fileVersion.getUploadedBy().getUsername())
                .uploadedAt(fileVersion.getUploadedAt())
                .build();
    }

    private boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getRole() == User.UserRole.ADMIN)
                .orElse(false);
    }
}

