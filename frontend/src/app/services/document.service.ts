import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';

export interface Document {
  id: number;
  title: string;
  description: string;
  tags: string[];
  ownerUsername: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  createdAt: string;
  updatedAt: string;
}

export interface DocumentCreateRequest {
  title: string;
  description?: string;
  tags?: string[];
}

export interface FileVersion {
  id: number;
  documentId: number;
  fileName: string;
  contentType: string;
  fileSize: number;
  uploadedByUsername: string;
  uploadedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: any;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = 'http://localhost:8080/api/documents';

  constructor(private http: HttpClient) {}

  listDocuments(page: number = 0, size: number = 10, title?: string, status?: string, sortBy: string = 'createdAt', direction: string = 'DESC'): Observable<PaginatedResponse<Document>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);

    if (title) {
      params = params.set('title', title);
    }
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PaginatedResponse<Document>>(this.apiUrl, { params });
  }

  getDocument(id: number): Observable<Document> {
    return this.http.get<Document>(`${this.apiUrl}/${id}`);
  }

  createDocument(request: DocumentCreateRequest): Observable<Document> {
    return this.http.post<Document>(this.apiUrl, request);
  }

  updateDocument(id: number, request: DocumentCreateRequest): Observable<Document> {
    return this.http.put<Document>(`${this.apiUrl}/${id}`, request);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  changeStatus(id: number, status: string): Observable<Document> {
    let params = new HttpParams().set('status', status);
    return this.http.put<Document>(`${this.apiUrl}/${id}/status`, {}, { params });
  }

  uploadFile(documentId: number, file: File): Observable<FileVersion> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<FileVersion>(
      `${this.apiUrl}/${documentId}/versions/upload`,
      formData
    );
  }

  getVersions(documentId: number): Observable<FileVersion[]> {
    return this.http.get<FileVersion[]>(`${this.apiUrl}/${documentId}/versions`);
  }

  getLatestVersion(documentId: number): Observable<FileVersion> {
    return this.http.get<FileVersion>(`${this.apiUrl}/${documentId}/versions/latest`);
  }

  downloadFile(versionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/versions/${versionId}/download`, {
      responseType: 'blob'
    });
  }

  deleteVersion(versionId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/versions/${versionId}`);
  }
}

