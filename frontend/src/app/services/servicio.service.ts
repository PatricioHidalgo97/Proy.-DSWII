import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Servicio, ServicioPayload } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ServicioService {
  private readonly url = `${environment.apiUrl}/servicios`;
  constructor(private readonly http: HttpClient) {}
  getAll(): Observable<Servicio[]> { return this.http.get<Servicio[]>(this.url); }
  create(data: ServicioPayload): Observable<Servicio> { return this.http.post<Servicio>(this.url, data); }
  update(id: number, data: ServicioPayload): Observable<Servicio> { return this.http.put<Servicio>(`${this.url}/${id}`, data); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
