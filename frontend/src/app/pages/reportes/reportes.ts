import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ReporteService } from '../../services/reporte.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({ selector: 'app-reportes', imports: [ReactiveFormsModule], templateUrl: './reportes.html' })
export class Reportes {
  readonly error = signal(''); readonly message = signal(''); readonly form;
  constructor(formBuilder: FormBuilder, private readonly service: ReporteService) {
    const d = new Date(); const month = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    this.form = formBuilder.nonNullable.group({ fecha: [`${month}-${String(d.getDate()).padStart(2, '0')}`], periodo: [month] });
  }
  download(path: string, filename: string, parameter?: { key: string; value: string }): void {
    this.error.set(''); this.service.download(path, parameter).subscribe({ next: (blob) => { const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = filename; link.click(); URL.revokeObjectURL(link.href); this.message.set(`Reporte ${filename} generado.`); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }
  daily(): void { const fecha = this.form.controls.fecha.value; this.download('movimientos-diarios', `movimientos-${fecha}.xlsx`, { key: 'fecha', value: fecha }); }
  monthly(): void { const p = this.form.controls.periodo.value; this.download('movimientos-mensuales', `movimientos-${p}.xlsx`, { key: 'periodo', value: p }); }
  expenses(): void { const p = this.form.controls.periodo.value; this.download('egresos', `egresos-${p}.xlsx`, { key: 'periodo', value: p }); }
}
