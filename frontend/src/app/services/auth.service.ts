import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, Observable} from 'rxjs';
import {tap} from 'rxjs/operators';
import {Router} from '@angular/router';

interface LoginRequest {
  username: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

interface AuthResponse {
  token: string;
  username: string;
  email: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private tokenKey = 'auth_token';
  private usernameKey = 'username';
  private currentUserSubject = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<AuthResponse> {
    const payload: LoginRequest = { username, password };
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, payload).pipe(
      tap(response => {
        localStorage.setItem(this.tokenKey, response.token);
        localStorage.setItem(this.usernameKey, response.username);
        this.currentUserSubject.next(true);
      })
    );
  }

  register(username: string, email: string, password: string): Observable<AuthResponse> {
    const payload: RegisterRequest = { username, email, password };
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, payload).pipe(
      tap(response => {
        localStorage.setItem(this.tokenKey, response.token);
        localStorage.setItem(this.usernameKey, response.username);
        this.currentUserSubject.next(true);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.usernameKey);
    this.currentUserSubject.next(false);
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getUsername(): string {
    return localStorage.getItem(this.usernameKey) || '';
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }

  getCurrentUser$(): Observable<boolean> {
    return this.currentUserSubject.asObservable();
  }
}

