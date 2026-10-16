import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Giro, GiroPayload } from '../models/models';

@Injectable({ providedIn: 'root' })
export class GiroService {
  private readonly url = `${environment.apiUrl}/giros`;
  constructor(private readonly http: HttpClient) {}
  getAll(): Observable<Giro[]> { return this.http.get<Giro[]>(this.url); }
  create(data: GiroPayload): Observable<Giro> { return this.http.post<Giro>(this.url, data); }
  update(id: number, data: GiroPayload): Observable<Giro> { return this.http.put<Giro>(`${this.url}/${id}`, data); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
