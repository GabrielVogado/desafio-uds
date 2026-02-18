import {Routes} from '@angular/router';
import {authGuard} from './guards/auth.guard';
import {LoginComponent} from './auth/login/login.component';
import {DashboardComponent} from './dashboard/dashboard.component';
import {DocumentListComponent} from './documents/document-list/document-list.component';
import {DocumentDetailComponent} from './documents/document-detail/document-detail.component';
import {DocumentEditComponent} from './documents/document-edit/document-edit.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'documents', component: DocumentListComponent, canActivate: [authGuard] },
  { path: 'documents/new', component: DocumentEditComponent, canActivate: [authGuard] },
  { path: 'documents/:id', component: DocumentDetailComponent, canActivate: [authGuard] },
  { path: 'documents/:id/edit', component: DocumentEditComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '/dashboard' }
];

