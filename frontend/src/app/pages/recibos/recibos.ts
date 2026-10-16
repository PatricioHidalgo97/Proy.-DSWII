import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Recibo, TipoRecibo } from '../../models/models';
import { ReciboService } from '../../services/recibo.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({ selector: 'app-recibos', imports: [ReactiveFormsModule], templateUrl: './recibos.html' })
export class Recibos {
  readonly items = signal<Recibo[]>([]); readonly selected = signal<Recibo | null>(null); readonly error = signal(''); readonly form;
  constructor(formBuilder: FormBuilder, private readonly service: ReciboService) {
    this.form = formBuilder.nonNullable.group({ fecha: [this.currentLocalDate(), Validators.required], tipo: [''] }); this.search();
  }
  search(): void { if (this.form.invalid) return; this.error.set(''); this.selected.set(null); const v = this.form.getRawValue(); this.service.getByDate(v.fecha, v.tipo as TipoRecibo | '').subscribe({ next: (items) => this.items.set(items), error: (e: unknown) => this.error.set(apiErrorMessage(e)) }); }
  view(item: Recibo): void { this.service.getById(item.id).subscribe({ next: (receipt) => this.selected.set(receipt), error: (e: unknown) => this.error.set(apiErrorMessage(e)) }); }
  target(item: Recibo): string { const c = item.cuentaCobrar; if (!c) return item.depositante ?? 'Ingreso externo'; if (c.socio) return `${c.socio.codigo} · ${c.socio.nombres} ${c.socio.apellidos}`; return c.puesto ? `Puesto ${c.puesto.numero}` : '—'; }
  concept(item: Recibo): string { return item.cuentaCobrar?.servicio.nombre ?? `${item.categoria ?? ''} ${item.concepto ?? ''}`.trim(); }
  total(): number { return this.items().reduce((sum, item) => sum + Number(item.monto), 0); }
  private currentLocalDate(): string { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
}
