import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { LoginResponse, SessionUser } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'gestion_token';
  private readonly userKey = 'gestion_user';
  readonly user = signal<SessionUser | null>(this.readUser());

  constructor(private readonly http: HttpClient) {}

  login(usuario: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { usuario, password })
      .pipe(tap((response) => this.saveSession(response)));
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.user.set(null);
  }

  token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return this.token() !== null;
  }

  private saveSession(response: LoginResponse): void {
    const user: SessionUser = {
      id: response.id,
      usuario: response.usuario,
      nombres: response.nombres,
      apellidos: response.apellidos,
      rol: response.rol,
    };
    localStorage.setItem(this.tokenKey, response.token);
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.user.set(user);
  }

  private readUser(): SessionUser | null {
    const stored = localStorage.getItem(this.userKey);
    if (!stored) return null;
    try {
      return JSON.parse(stored) as SessionUser;
    } catch {
      localStorage.removeItem(this.userKey);
      return null;
    }
  }
}
