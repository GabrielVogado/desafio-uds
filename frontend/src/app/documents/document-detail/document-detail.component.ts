import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {Document, DocumentService, FileVersion} from '../../services/document.service';

@Component({
  selector: 'app-document-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="document-detail">
      <div *ngIf="isLoading" class="loading">
        <div class="spinner"></div>
      </div>

      <div *ngIf="!isLoading && document" class="card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
          <h1>{{ document.title }}</h1>
          <div>
            <button routerLink="/documents/{{ document.id }}/edit" class="btn btn-primary" style="margin-right: 0.5rem;">
              Editar
            </button>
            <button (click)="deleteDocument()" class="btn btn-danger">
              Deletar
            </button>
          </div>
        </div>

        <div style="margin-bottom: 1.5rem;">
          <p><strong>Descrição:</strong> {{ document.description || 'N/A' }}</p>
          <p><strong>Status:</strong> <span [class]="'badge badge-' + document.status.toLowerCase()">{{ document.status }}</span></p>
          <p><strong>Tags:</strong> <span *ngFor="let tag of document.tags" class="badge badge-draft" style="margin-right: 0.5rem;">{{ tag }}</span></p>
          <p><strong>Criado em:</strong> {{ document.createdAt | date: 'dd/MM/yyyy HH:mm' }}</p>
        </div>

        <hr>

        <h2 style="margin-top: 1.5rem; margin-bottom: 1rem;">Upload de Arquivo</h2>
        
        <div class="form-group">
          <label>Novo Arquivo (PDF, PNG ou JPEG)</label>
          <input type="file" #fileInput accept=".pdf,.png,.jpg,.jpeg" (change)="onFileSelected($event)">
        </div>

        <button (click)="uploadFile()" class="btn btn-primary" [disabled]="!selectedFile || isUploading">
          {{ isUploading ? 'Enviando...' : 'Upload' }}
        </button>

        <div *ngIf="uploadSuccess" class="alert alert-success" style="margin-top: 1rem;">
          Arquivo enviado com sucesso!
        </div>

        <div *ngIf="uploadError" class="alert alert-error" style="margin-top: 1rem;">
          {{ uploadError }}
        </div>

        <hr style="margin-top: 1.5rem;">

        <h2 style="margin-bottom: 1rem;">Versões do Arquivo</h2>

        <div *ngIf="loadingVersions" class="loading">
          <div class="spinner"></div>
        </div>

        <table *ngIf="!loadingVersions && versions.length > 0" class="table">
          <thead>
            <tr>
              <th>Nome do Arquivo</th>
              <th>Tipo</th>
              <th>Tamanho</th>
              <th>Enviado por</th>
              <th>Data</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let version of versions">
              <td>{{ version.fileName }}</td>
              <td>{{ version.contentType }}</td>
              <td>{{ (version.fileSize / 1024).toFixed(2) }} KB</td>
              <td>{{ version.uploadedByUsername }}</td>
              <td>{{ version.uploadedAt | date: 'dd/MM/yyyy HH:mm' }}</td>
              <td>
                <button (click)="downloadVersion(version.id)" class="btn btn-secondary" style="padding: 0.5rem; margin-right: 0.5rem;">
                  Download
                </button>
                <button (click)="deleteVersion(version.id)" class="btn btn-danger" style="padding: 0.5rem;">
                  Deletar
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div *ngIf="!loadingVersions && versions.length === 0" class="alert alert-warning">
          Nenhuma versão de arquivo encontrada.
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class DocumentDetailComponent implements OnInit {
  document: Document | null = null;
  versions: FileVersion[] = [];
  isLoading: boolean = false;
  loadingVersions: boolean = false;
  isUploading: boolean = false;
  selectedFile: File | null = null;
  uploadSuccess: boolean = false;
  uploadError: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private documentService: DocumentService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = parseInt(params['id']);
      this.loadDocument(id);
      this.loadVersions(id);
    });
  }

  loadDocument(id: number): void {
    this.isLoading = true;
    this.documentService.getDocument(id).subscribe({
      next: (doc: any) => {
        this.document = doc;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erro ao carregar documento:', error);
        this.isLoading = false;
      }
    });
  }

  loadVersions(id: number): void {
    this.loadingVersions = true;
    this.documentService.getVersions(id).subscribe({
      next: (versions: any) => {
        this.versions = versions;
        this.loadingVersions = false;
      },
      error: (error: any) => {
        console.error('Erro ao carregar versões:', error);
        this.loadingVersions = false;
      }
    });
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    if (files && files.length > 0) {
      this.selectedFile = files[0];
    }
  }

  uploadFile(): void {
    if (!this.selectedFile || !this.document) return;

    this.isUploading = true;
    this.uploadError = '';
    this.uploadSuccess = false;

    this.documentService.uploadFile(this.document.id, this.selectedFile).subscribe({
      next: () => {
        this.isUploading = false;
        this.uploadSuccess = true;
        this.selectedFile = null;
        this.loadVersions(this.document!.id);
        setTimeout(() => this.uploadSuccess = false, 3000);
      },
      error: (error: any) => {
        this.isUploading = false;
        this.uploadError = error.error?.message || 'Erro ao fazer upload do arquivo.';
      }
    });
  }

  downloadVersion(versionId: number): void {
    this.documentService.downloadFile(versionId).subscribe({
      next: (blob: any) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'file';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error: any) => {
        console.error('Erro ao baixar arquivo:', error);
      }
    });
  }

  deleteVersion(versionId: number): void {
    if (confirm('Tem certeza que deseja deletar esta versão?')) {
      this.documentService.deleteVersion(versionId).subscribe({
        next: (_res: any) => {
          if (this.document) {
            this.loadVersions(this.document.id);
          }
        },
        error: (error: any) => {
          console.error('Erro ao deletar versão:', error);
        }
      });
    }
  }

  deleteDocument(): void {
    if (confirm('Tem certeza que deseja deletar este documento?')) {
      if (this.document) {
        this.documentService.deleteDocument(this.document.id).subscribe({
          next: (_res: any) => {
            this.router.navigate(['/documents']);
          },
          error: (error: any) => {
            console.error('Erro ao deletar documento:', error);
          }
        });
      }
    }
  }
}
