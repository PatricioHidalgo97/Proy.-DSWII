import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { IngresoExternoPayload, Recibo, TipoRecibo } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ReciboService {
  private readonly url = `${environment.apiUrl}/recibos`;
  constructor(private readonly http: HttpClient) {}
  getByDate(fecha: string, tipo?: TipoRecibo | ''): Observable<Recibo[]> {
    let params = new HttpParams().set('fecha', fecha);
    if (tipo) params = params.set('tipo', tipo);
    return this.http.get<Recibo[]>(this.url, { params });
  }
  getById(id: number): Observable<Recibo> { return this.http.get<Recibo>(`${this.url}/${id}`); }
  createExternal(data: IngresoExternoPayload): Observable<Recibo> {
    return this.http.post<Recibo>(`${this.url}/externos`, data);
  }
}
