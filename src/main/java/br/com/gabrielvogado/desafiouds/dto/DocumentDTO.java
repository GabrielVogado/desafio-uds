package br.com.gabrielvogado.desafiouds.dto;

import br.com.gabrielvogado.desafiouds.model.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDTO {

    private Long id;
    private String title;
    private String description;
    private Set<String> tags;
    private String ownerUsername;
    private Document.DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

