import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CanjePayload, CuentaCobrar, CuentaPayload, GenerarCuentasPayload, Recibo } from '../models/models';

@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly url = `${environment.apiUrl}/cuentas`;
  constructor(private readonly http: HttpClient) {}
  getAll(): Observable<CuentaCobrar[]> { return this.http.get<CuentaCobrar[]>(this.url); }
  getByPuesto(puestoId: number): Observable<CuentaCobrar[]> {
    return this.http.get<CuentaCobrar[]>(`${this.url}/puesto/${puestoId}`);
  }
  getBySocio(socioId: number): Observable<CuentaCobrar[]> {
    return this.http.get<CuentaCobrar[]>(`${this.url}/socio/${socioId}`);
  }
  create(data: CuentaPayload): Observable<CuentaCobrar> { return this.http.post<CuentaCobrar>(this.url, data); }
  generateForPuestos(data: GenerarCuentasPayload): Observable<CuentaCobrar[]> {
    return this.http.post<CuentaCobrar[]>(`${this.url}/generar/puestos`, data);
  }
  generateForSocios(data: GenerarCuentasPayload): Observable<CuentaCobrar[]> {
    return this.http.post<CuentaCobrar[]>(`${this.url}/generar/socios`, data);
  }
  exonerate(id: number): Observable<CuentaCobrar> { return this.http.put<CuentaCobrar>(`${this.url}/${id}/exonerar`, {}); }
  exonerateBatch(cuentaIds: number[]): Observable<CuentaCobrar[]> {
    return this.http.put<CuentaCobrar[]>(`${this.url}/exonerar-lote`, { cuentaIds });
  }
  pay(id: number): Observable<Recibo> { return this.http.post<Recibo>(`${this.url}/${id}/pagar`, {}); }
  payBatch(cuentaIds: number[]): Observable<Recibo[]> {
    return this.http.post<Recibo[]>(`${this.url}/pagar-lote`, { cuentaIds });
  }
  exchange(data: CanjePayload): Observable<Recibo> { return this.http.post<Recibo>(`${this.url}/canjear`, data); }
}
