import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Socio, SocioPayload } from '../models/models';

@Injectable({ providedIn: 'root' })
export class SocioService {
  private readonly url = `${environment.apiUrl}/socios`;
  constructor(private readonly http: HttpClient) {}
  getAll(): Observable<Socio[]> { return this.http.get<Socio[]>(this.url); }
  create(data: SocioPayload): Observable<Socio> { return this.http.post<Socio>(this.url, data); }
  update(id: number, data: SocioPayload): Observable<Socio> { return this.http.put<Socio>(`${this.url}/${id}`, data); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
