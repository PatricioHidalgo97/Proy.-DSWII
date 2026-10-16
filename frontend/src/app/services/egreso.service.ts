import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Egreso, EgresoPayload } from '../models/models';

@Injectable({ providedIn: 'root' })
export class EgresoService {
  private readonly url = `${environment.apiUrl}/egresos`;
  constructor(private readonly http: HttpClient) {}
  getByMonth(periodo: string): Observable<Egreso[]> {
    return this.http.get<Egreso[]>(this.url, { params: new HttpParams().set('periodo', periodo) });
  }
  create(data: EgresoPayload): Observable<Egreso> { return this.http.post<Egreso>(this.url, data); }
  process(id: number): Observable<Egreso> { return this.http.put<Egreso>(`${this.url}/${id}/procesar`, {}); }
  annul(id: number): Observable<Egreso> { return this.http.put<Egreso>(`${this.url}/${id}/anular`, {}); }
  importCsv(file: File): Observable<Egreso[]> {
    const data = new FormData(); data.append('archivo', file);
    return this.http.post<Egreso[]>(`${this.url}/importar`, data);
  }
}
