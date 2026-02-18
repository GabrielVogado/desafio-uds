package br.com.gabrielvogado.desafiouds.service;

import br.com.gabrielvogado.desafiouds.dto.DocumentCreateRequest;
import br.com.gabrielvogado.desafiouds.dto.DocumentDTO;
import br.com.gabrielvogado.desafiouds.exception.UnauthorizedException;
import br.com.gabrielvogado.desafiouds.model.Document;
import br.com.gabrielvogado.desafiouds.model.User;
import br.com.gabrielvogado.desafiouds.repository.DocumentRepository;
import br.com.gabrielvogado.desafiouds.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    private User testUser;
    private Document testDocument;
    private DocumentCreateRequest createRequest;

    @BeforeEach
    void setUp() {
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
                .description("Test Description")
                .tags(Set.of("tag1", "tag2"))
                .owner(testUser)
                .status(Document.DocumentStatus.DRAFT)
                .build();

        createRequest = DocumentCreateRequest.builder()
                .title("Test Document")
                .description("Test Description")
                .tags(Set.of("tag1", "tag2"))
                .build();
    }

    @Test
    void deveCriarDocumentoComSucesso() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        DocumentDTO result = documentService.createDocument(createRequest, "testuser");

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Document");
        assertThat(result.getDescription()).isEqualTo("Test Description");
        assertThat(result.getStatus()).isEqualTo(Document.DocumentStatus.DRAFT);
        assertThat(result.getOwnerUsername()).isEqualTo("testuser");

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void deveLancarExcecaoQuandoCriarDocumentoComTituloVazio() {
        DocumentCreateRequest invalidRequest = DocumentCreateRequest.builder()
                .title("")
                .description("Test Description")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> documentService.createDocument(invalidRequest, "testuser"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void deveRetornarDocumentosPaginadosComFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Document doc1 = Document.builder()
                .id(1L)
                .title("Document 1")
                .owner(testUser)
                .status(Document.DocumentStatus.DRAFT)
                .build();
        Document doc2 = Document.builder()
                .id(2L)
                .title("Document 2")
                .owner(testUser)
                .status(Document.DocumentStatus.PUBLISHED)
                .build();

        Page<Document> documentPage = new PageImpl<>(Arrays.asList(doc1, doc2), pageable, 2);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.findByOwnerAndTitleContainingAndStatus(
                testUser, "Document", Document.DocumentStatus.DRAFT, pageable))
                .thenReturn(new PageImpl<>(Arrays.asList(doc1), pageable, 1));

        Page<DocumentDTO> result = documentService.listDocuments(
                "testuser",
                "Document",
                Document.DocumentStatus.DRAFT,
                pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Document 1");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(documentRepository, times(1)).findByOwnerAndTitleContainingAndStatus(
                testUser, "Document", Document.DocumentStatus.DRAFT, pageable);
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoAutorizadoTentarAcessarDocumento() {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .passwordHash("hashedpassword")
                .role(User.UserRole.USER)
                .build();

        Document otherUserDocument = Document.builder()
                .id(1L)
                .title("Other Document")
                .owner(otherUser)
                .status(Document.DocumentStatus.DRAFT)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(otherUserDocument));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> documentService.getDocumentById(1L, "testuser"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permission");

        verify(documentRepository, times(1)).findById(1L);
    }

    @Test
    void deveDeletarDocumentoComSucesso() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        documentService.deleteDocument(1L, "testuser");

        verify(documentRepository, times(1)).delete(testDocument);
    }

    @Test
    void deveAlterarStatusDoDocumentoComSucesso() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        DocumentDTO result = documentService.changeStatus(1L, Document.DocumentStatus.PUBLISHED, "testuser");

        assertThat(result).isNotNull();
        verify(documentRepository, times(1)).save(any(Document.class));
    }
}

