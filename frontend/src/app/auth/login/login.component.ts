import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="card" style="max-width: 400px; margin: 0 auto;">
        <h2>Login</h2>
        
        <div *ngIf="error" class="alert alert-error">
          {{ error }}
        </div>

        <form (ngSubmit)="login()">
          <div class="form-group">
            <label>Usuário</label>
            <input type="text" [(ngModel)]="username" name="username" required>
          </div>

          <div class="form-group">
            <label>Senha</label>
            <input type="password" [(ngModel)]="password" name="password" required>
          </div>

          <button type="submit" class="btn btn-primary" style="width: 100%; margin-bottom: 1rem;" [disabled]="isLoading">
            {{ isLoading ? 'Conectando...' : 'Entrar' }}
          </button>

          <div style="text-align: center; color: #666;">
            Não tem conta? 
            <a href="javascript:void(0)" (click)="toggleRegister()" style="color: #3498db; text-decoration: none;">
              Registre-se aqui
            </a>
          </div>
        </form>
      </div>

      <div class="card" *ngIf="showRegister" style="max-width: 400px; margin: 2rem auto;">
        <h2>Registrar</h2>
        
        <div *ngIf="registerError" class="alert alert-error">
          {{ registerError }}
        </div>

        <form (ngSubmit)="register()">
          <div class="form-group">
            <label>Usuário</label>
            <input type="text" [(ngModel)]="registerUsername" name="registerUsername" required>
          </div>

          <div class="form-group">
            <label>Email</label>
            <input type="email" [(ngModel)]="registerEmail" name="registerEmail" required>
          </div>

          <div class="form-group">
            <label>Senha</label>
            <input type="password" [(ngModel)]="registerPassword" name="registerPassword" required>
          </div>

          <button type="submit" class="btn btn-primary" style="width: 100%; margin-bottom: 1rem;" [disabled]="isLoading">
            {{ isLoading ? 'Registrando...' : 'Registrar' }}
          </button>

          <div style="text-align: center; color: #666;">
            Já tem conta? 
            <a href="javascript:void(0)" (click)="toggleRegister()" style="color: #3498db; text-decoration: none;">
              Voltar ao login
            </a>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 80vh;
    }
  `]
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  error: string = '';
  isLoading: boolean = false;

  registerUsername: string = '';
  registerEmail: string = '';
  registerPassword: string = '';
  registerError: string = '';
  showRegister: boolean = false;

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/documents']);
    }
  }

  login(): void {
    if (!this.username || !this.password) {
      this.error = 'Usuário e senha são obrigatórios';
      return;
    }

    this.isLoading = true;
    this.error = '';

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/documents']);
      },
      error: (error: any) => {
        this.isLoading = false;
        this.error = error.error?.message || 'Erro ao fazer login. Verifique suas credenciais.';
      }
    });
  }

  register(): void {
    if (!this.registerUsername || !this.registerEmail || !this.registerPassword) {
      this.registerError = 'Todos os campos são obrigatórios';
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.registerEmail)) {
      this.registerError = 'Email inválido';
      return;
    }

    this.isLoading = true;
    this.registerError = '';

    this.authService.register(this.registerUsername, this.registerEmail, this.registerPassword).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/documents']);
      },
      error: (error: any) => {
        this.isLoading = false;
        this.registerError = error.error?.message || 'Erro ao registrar. Tente novamente.';
      }
    });
  }

  toggleRegister(): void {
    this.showRegister = !this.showRegister;
    this.error = '';
    this.registerError = '';
  }
}
