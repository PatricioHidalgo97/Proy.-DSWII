import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly url = `${environment.apiUrl}/reportes`;
  constructor(private readonly http: HttpClient) {}
  download(path: string, parameter?: { key: string; value: string }): Observable<Blob> {
    const params = parameter ? new HttpParams().set(parameter.key, parameter.value) : undefined;
    return this.http.get(`${this.url}/${path}`, { params, responseType: 'blob' });
  }
}
