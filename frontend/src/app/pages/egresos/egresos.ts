import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { EgresoService } from '../../services/egreso.service';
import { Egreso, EgresoPayload } from '../../models/models';
import { apiErrorMessage } from '../../shared/api-error';

@Component({ selector: 'app-egresos', imports: [ReactiveFormsModule], templateUrl: './egresos.html' })
export class Egresos {
  readonly items = signal<Egreso[]>([]); readonly error = signal(''); readonly message = signal(''); readonly form; readonly filterForm;
  constructor(formBuilder: FormBuilder, private readonly service: EgresoService, public readonly auth: AuthService) {
    this.form = formBuilder.nonNullable.group({ tipoDocumento: ['FACTURA', Validators.required], numeroDocumento: ['', Validators.required], proveedor: ['', Validators.required], fecha: [this.today(), Validators.required], subtotal: [0, Validators.min(0)], impuesto: [0, Validators.min(0)], total: [0, Validators.min(0.01)], motivo: ['', Validators.required], documentoAsociado: [''] });
    this.filterForm = formBuilder.nonNullable.group({ periodo: [this.currentPeriod(), Validators.required] }); this.load();
  }
  load(): void { this.service.getByMonth(this.filterForm.getRawValue().periodo).subscribe({ next: (items) => this.items.set(items), error: (e: unknown) => this.showError(e) }); }
  create(): void { if (this.form.invalid) return; this.clear(); const v = this.form.getRawValue(); const payload: EgresoPayload = { ...v, subtotal: Number(v.subtotal), impuesto: Number(v.impuesto), total: Number(v.total), documentoAsociado: v.documentoAsociado || null }; this.service.create(payload).subscribe({ next: (e) => { this.message.set(`Egreso ${e.comprobante} registrado.`); this.form.patchValue({ numeroDocumento: '', proveedor: '', subtotal: 0, impuesto: 0, total: 0, motivo: '', documentoAsociado: '' }); this.load(); }, error: (e: unknown) => this.showError(e) }); }
  process(item: Egreso): void { this.clear(); this.service.process(item.id).subscribe({ next: () => { this.message.set('Egreso procesado.'); this.load(); }, error: (e: unknown) => this.showError(e) }); }
  annul(item: Egreso): void { if (!confirm(`¿Anular ${item.comprobante}?`)) return; this.clear(); this.service.annul(item.id).subscribe({ next: () => { this.message.set('Egreso anulado.'); this.load(); }, error: (e: unknown) => this.showError(e) }); }
  import(event: Event): void { const file = (event.target as HTMLInputElement).files?.[0]; if (!file) return; this.clear(); this.service.importCsv(file).subscribe({ next: (items) => { this.message.set(`${items.length} egresos importados.`); this.load(); }, error: (e: unknown) => this.showError(e) }); }
  total(): number { return this.items().filter((x) => x.estado !== 'ANULADO').reduce((sum, x) => sum + Number(x.total), 0); }
  private currentPeriod(): string { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`; }
  private today(): string { const d = new Date(); return `${this.currentPeriod()}-${String(d.getDate()).padStart(2, '0')}`; }
  private clear(): void { this.error.set(''); this.message.set(''); }
  private showError(e: unknown): void { this.error.set(apiErrorMessage(e)); }
}
