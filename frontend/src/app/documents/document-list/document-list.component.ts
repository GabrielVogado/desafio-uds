import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {Document, DocumentService, PaginatedResponse} from '../../services/document.service';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="document-list">
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
          <h1>Meus Documentos</h1>
          <button routerLink="/documents/new" routerLinkActive="active" class="btn btn-primary">
            + Novo Documento
          </button>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr 150px; gap: 1rem; margin-bottom: 1.5rem;">
          <input type="text" [(ngModel)]="searchTitle" placeholder="Buscar por título..." (keyup)="onSearchChange()">
          <select [(ngModel)]="selectedStatus" (change)="onFilterChange()">
            <option value="">Todos os status</option>
            <option value="DRAFT">Rascunho</option>
            <option value="PUBLISHED">Publicado</option>
            <option value="ARCHIVED">Arquivado</option>
          </select>
          <button class="btn btn-secondary" (click)="resetFilters()">Resetar</button>
        </div>

        <div *ngIf="isLoading" class="loading">
          <div class="spinner"></div>
          <p>Carregando documentos...</p>
        </div>

        <div *ngIf="!isLoading && documents.length === 0" class="alert alert-warning">
          Nenhum documento encontrado. <a routerLink="/documents" (click)="createNew()" style="color: #856404;">Criar um novo.</a>
        </div>

        <table *ngIf="!isLoading && documents.length > 0" class="table">
          <thead>
            <tr>
              <th>Título</th>
              <th>Status</th>
              <th>Data Criação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let doc of documents">
              <td>{{ doc.title }}</td>
              <td>
                <span [class]="'badge badge-' + doc.status.toLowerCase()">
                  {{ doc.status }}
                </span>
              </td>
              <td>{{ doc.createdAt | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <button routerLink="/documents/{{ doc.id }}" class="btn btn-secondary" style="padding: 0.5rem; margin-right: 0.5rem;">
                  Ver
                </button>
                <button routerLink="/documents/{{ doc.id }}/edit" class="btn btn-secondary" style="padding: 0.5rem;">
                  Editar
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div *ngIf="!isLoading && totalPages > 1" style="display: flex; justify-content: center; gap: 0.5rem; margin-top: 1.5rem;">
          <button class="btn btn-secondary" (click)="previousPage()" [disabled]="currentPage === 0">Anterior</button>
          <span style="padding: 0.75rem;">Página {{ currentPage + 1 }} de {{ totalPages }}</span>
          <button class="btn btn-secondary" (click)="nextPage()" [disabled]="currentPage >= totalPages - 1">Próxima</button>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class DocumentListComponent implements OnInit {
  documents: Document[] = [];
  isLoading: boolean = false;
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  searchTitle: string = '';
  selectedStatus: string = '';

  constructor(private documentService: DocumentService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.isLoading = true;
    this.documentService.listDocuments(
      this.currentPage,
      this.pageSize,
      this.searchTitle || undefined,
      this.selectedStatus || undefined
    ).subscribe({
      next: (response: PaginatedResponse<Document>) => {
        this.documents = response.content;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar documentos:', error);
        this.isLoading = false;
      }
    });
  }

  onSearchChange(): void {
    this.currentPage = 0;
    this.loadDocuments();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadDocuments();
  }

  resetFilters(): void {
    this.searchTitle = '';
    this.selectedStatus = '';
    this.currentPage = 0;
    this.loadDocuments();
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadDocuments();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadDocuments();
    }
  }

  createNew(): void {
    // Navega para página de edição (sem ID significa novo documento)
    // O click do routerLink já navega para /documents/edit
  }
}

