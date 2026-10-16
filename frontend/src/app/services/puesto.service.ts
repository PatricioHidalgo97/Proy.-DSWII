import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Puesto, PuestoPayload } from '../models/models';

@Injectable({ providedIn: 'root' })
export class PuestoService {
  private readonly url = `${environment.apiUrl}/puestos`;
  constructor(private readonly http: HttpClient) {}
  getAll(): Observable<Puesto[]> { return this.http.get<Puesto[]>(this.url); }
  create(data: PuestoPayload): Observable<Puesto> { return this.http.post<Puesto>(this.url, data); }
  update(id: number, data: PuestoPayload): Observable<Puesto> { return this.http.put<Puesto>(`${this.url}/${id}`, data); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
