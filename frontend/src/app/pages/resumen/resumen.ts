import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { CuentaCobrar, Puesto, Socio } from '../../models/models';
import { CuentaService } from '../../services/cuenta.service';
import { PuestoService } from '../../services/puesto.service';
import { SocioService } from '../../services/socio.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({ selector: 'app-resumen', imports: [ReactiveFormsModule], templateUrl: './resumen.html' })
export class Resumen {
  readonly items = signal<CuentaCobrar[]>([]); readonly puestos = signal<Puesto[]>([]); readonly socios = signal<Socio[]>([]); readonly error = signal(''); readonly form;
  constructor(formBuilder: FormBuilder, private readonly cuentas: CuentaService, puestos: PuestoService, socios: SocioService) {
    this.form = formBuilder.nonNullable.group({ tipo: ['PUESTO'], targetId: [0] });
    puestos.getAll().subscribe({ next: (x) => this.puestos.set(x), error: (e: unknown) => this.showError(e) });
    socios.getAll().subscribe({ next: (x) => this.socios.set(x), error: (e: unknown) => this.showError(e) });
  }
  typeChanged(): void { this.form.patchValue({ targetId: 0 }); this.items.set([]); }
  search(): void { const v = this.form.getRawValue(); if (Number(v.targetId) <= 0) { this.error.set('Seleccione un socio o puesto.'); return; } this.error.set(''); const request = v.tipo === 'PUESTO' ? this.cuentas.getByPuesto(Number(v.targetId)) : this.cuentas.getBySocio(Number(v.targetId)); request.subscribe({ next: (x) => this.items.set(x), error: (e: unknown) => this.showError(e) }); }
  sum(status?: string): number { return this.items().filter((x) => !status || x.estado === status).reduce((total, x) => total + Number(x.monto), 0); }
  private showError(e: unknown): void { this.error.set(apiErrorMessage(e)); }
}
