import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterLink, RouterOutlet} from '@angular/router';
import {AuthService} from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <nav class="navbar">
      <div class="container">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <a routerLink="/dashboard" style="font-size: 1.3rem; font-weight: bold;">📄 GED</a>
          </div>
          <div>
            <a *ngIf="isLoggedIn()" routerLink="/documents" routerLinkActive="active">Documentos</a>
            <span *ngIf="isLoggedIn()" style="color: #bdc3c7; margin: 0 1rem;">|</span>
            <button *ngIf="isLoggedIn()" (click)="logout()" class="btn btn-secondary" style="padding: 0.5rem 1rem;">
              Logout ({{ username }})
            </button>
          </div>
        </div>
      </div>
    </nav>

    <div class="container" style="padding-top: 2rem;">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: []
})
export class AppComponent {
  username: string = '';

  constructor(private authService: AuthService) {
    this.username = this.authService.getUsername();
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  logout(): void {
    this.authService.logout();
  }
}

