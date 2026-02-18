import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="dashboard">
      <div class="card text-center">
        <h1>Bem-vindo ao GED</h1>
        <p>Gestão Eletrônica de Documentos</p>
        
        <div style="margin-top: 2rem;">
          <button routerLink="/documents" class="btn btn-primary" style="margin-right: 1rem;">
            Acessar Documentos
          </button>
          <button routerLink="/documents" class="btn btn-secondary">
            Ver Meus Documentos
          </button>
        </div>
      </div>

      <div class="card" style="margin-top: 2rem;">
        <h2>Recursos Disponíveis</h2>
        <ul style="list-style: none; text-align: left; margin: 1rem auto; max-width: 500px;">
          <li>✓ Criar e gerenciar documentos</li>
          <li>✓ Upload de arquivos (PDF, PNG, JPEG)</li>
          <li>✓ Versionamento automático de arquivos</li>
          <li>✓ Pesquisa e filtros por status e título</li>
          <li>✓ Controle de acesso por perfil</li>
          <li>✓ Download de versões anteriores</li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      max-width: 800px;
      margin: 0 auto;
    }
  `]
})
export class DashboardComponent {}
