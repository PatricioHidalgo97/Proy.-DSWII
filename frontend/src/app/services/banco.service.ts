import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Banco, BancoPayload } from '../models/models';

@Injectable({ providedIn: 'root' })
export class BancoService {
  private readonly url = `${environment.apiUrl}/bancos`;
  constructor(private readonly http: HttpClient) {}
  getAll(): Observable<Banco[]> { return this.http.get<Banco[]>(this.url); }
  create(data: BancoPayload): Observable<Banco> { return this.http.post<Banco>(this.url, data); }
  update(id: number, data: BancoPayload): Observable<Banco> { return this.http.put<Banco>(`${this.url}/${id}`, data); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
