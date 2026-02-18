import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {DocumentCreateRequest, DocumentService} from '../../services/document.service';

@Component({
  selector: 'app-document-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="document-edit">
      <div *ngIf="isLoading" class="loading">
        <div class="spinner"></div>
      </div>

      <div *ngIf="!isLoading" class="card" style="max-width: 600px; margin: 0 auto;">
        <h1>{{ isNew ? 'Novo Documento' : 'Editar Documento' }}</h1>

        <div *ngIf="error" class="alert alert-error">
          {{ error }}
        </div>

        <div *ngIf="success" class="alert alert-success">
          Documento {{ isNew ? 'criado' : 'atualizado' }} com sucesso!
        </div>

        <form (ngSubmit)="saveDocument()">
          <div class="form-group">
            <label>Título *</label>
            <input type="text" [(ngModel)]="title" name="title" required>
          </div>

          <div class="form-group">
            <label>Descrição</label>
            <textarea [(ngModel)]="description" name="description"></textarea>
          </div>

          <div class="form-group">
            <label>Tags (separadas por vírgula)</label>
            <input type="text" [(ngModel)]="tagsInput" name="tagsInput" placeholder="tag1, tag2, tag3">
          </div>

          <div *ngIf="!isNew" class="form-group">
            <label>Status</label>
            <select [(ngModel)]="status" name="status">
              <option value="DRAFT">Rascunho</option>
              <option value="PUBLISHED">Publicado</option>
              <option value="ARCHIVED">Arquivado</option>
            </select>
          </div>

          <div style="display: flex; gap: 1rem;">
            <button type="submit" class="btn btn-primary" [disabled]="isSaving">
              {{ isSaving ? 'Salvando...' : 'Salvar' }}
            </button>
            <button type="button" routerLink="/documents" class="btn btn-secondary">
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: []
})
export class DocumentEditComponent implements OnInit {
  isNew: boolean = true;
  isLoading: boolean = false;
  isSaving: boolean = false;
  error: string = '';
  success: boolean = false;

  title: string = '';
  description: string = '';
  tagsInput: string = '';
  status: string = 'DRAFT';
  documentId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isNew = false;
        this.documentId = parseInt(params['id']);
        this.loadDocument(this.documentId);
      }
    });
  }

  loadDocument(id: number): void {
    this.isLoading = true;
    this.documentService.getDocument(id).subscribe({
      next: (doc) => {
        this.title = doc.title;
        this.description = doc.description || '';
        this.tagsInput = doc.tags.join(', ');
        this.status = doc.status;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erro ao carregar documento:', error);
        this.error = 'Erro ao carregar documento.';
        this.isLoading = false;
      }
    });
  }

  saveDocument(): void {
    if (!this.title.trim()) {
      this.error = 'Título é obrigatório';
      return;
    }

    const tags = this.tagsInput
      .split(',')
      .map(tag => tag.trim())
      .filter(tag => tag.length > 0);

    const request: DocumentCreateRequest = {
      title: this.title,
      description: this.description,
      tags: tags as any
    };

    this.isSaving = true;
    this.error = '';
    this.success = false;

    const saveOperation = this.isNew
      ? this.documentService.createDocument(request)
      : this.documentService.updateDocument(this.documentId!, request);

    saveOperation.subscribe({
      next: (doc) => {
        this.isSaving = false;
        this.success = true;

        if (!this.isNew && this.status !== 'DRAFT') {
          this.documentService.changeStatus(doc.id, this.status).subscribe({
            next: () => {
              setTimeout(() => this.router.navigate(['/documents']), 1500);
            },
            error: (error) => {
              console.error('Erro ao mudar status:', error);
              setTimeout(() => this.router.navigate(['/documents']), 1500);
            }
          });
        } else {
          setTimeout(() => this.router.navigate(['/documents']), 1500);
        }
      },
      error: (error) => {
        this.isSaving = false;
        this.error = error.error?.message || 'Erro ao salvar documento.';
      }
    });
  }
}

