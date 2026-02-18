package br.com.gabrielvogado.desafiouds.service;

import br.com.gabrielvogado.desafiouds.dto.FileVersionDTO;
import br.com.gabrielvogado.desafiouds.exception.InvalidFileException;
import br.com.gabrielvogado.desafiouds.model.Document;
import br.com.gabrielvogado.desafiouds.model.FileVersion;
import br.com.gabrielvogado.desafiouds.model.User;
import br.com.gabrielvogado.desafiouds.repository.DocumentRepository;
import br.com.gabrielvogado.desafiouds.repository.FileVersionRepository;
import br.com.gabrielvogado.desafiouds.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileVersionRepository fileVersionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileService fileService;

    private User testUser;
    private Document testDocument;
    private FileVersion testFileVersion;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "storagePath", "./test-uploads");
        ReflectionTestUtils.setField(fileService, "maxFileSize", 10485760L);
        ReflectionTestUtils.setField(fileService, "allowedTypes", "application/pdf,image/png,image/jpeg");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(User.UserRole.USER)
                .build();

        testDocument = Document.builder()
                .id(1L)
                .title("Test Document")
                .owner(testUser)
                .status(Document.DocumentStatus.DRAFT)
                .build();

        testFileVersion = FileVersion.builder()
                .id(1L)
                .document(testDocument)
                .fileKey("test-file-key")
                .fileName("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .uploadedBy(testUser)
                .build();
    }

    @Test
    void deveFazerUploadDeArquivoValidoComSucesso() throws IOException {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(fileVersionRepository.save(any(FileVersion.class))).thenReturn(testFileVersion);

        FileVersionDTO result = fileService.uploadFile(1L, multipartFile, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("test.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");

        verify(documentRepository, times(1)).findById(1L);
        verify(fileVersionRepository, times(1)).save(any(FileVersion.class));
    }

    @Test
    void deveLancarExcecaoQuandoFazerUploadDeArquivoComTipoInvalido() throws IOException {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        assertThatThrownBy(() -> fileService.uploadFile(1L, multipartFile, "testuser"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File type not allowed");

        verify(fileVersionRepository, never()).save(any(FileVersion.class));
    }

    @Test
    void deveLancarExcecaoQuandoFazerUploadDeArquivoExcedendoTamanhoMaximo() throws IOException {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(20971520L);

        assertThatThrownBy(() -> fileService.uploadFile(1L, multipartFile, "testuser"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File size exceeds maximum");

        verify(fileVersionRepository, never()).save(any(FileVersion.class));
    }
}

