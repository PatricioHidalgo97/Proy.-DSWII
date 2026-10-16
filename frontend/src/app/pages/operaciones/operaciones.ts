import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Banco, CuentaCobrar, Recibo } from '../../models/models';
import { BancoService } from '../../services/banco.service';
import { CuentaService } from '../../services/cuenta.service';
import { ReciboService } from '../../services/recibo.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({ selector: 'app-operaciones', imports: [ReactiveFormsModule], templateUrl: './operaciones.html' })
export class Operaciones {
  readonly bancos = signal<Banco[]>([]); readonly cuentas = signal<CuentaCobrar[]>([]); readonly receipt = signal<Recibo | null>(null);
  readonly error = signal(''); readonly message = signal(''); readonly externalForm; readonly exchangeForm;
  constructor(formBuilder: FormBuilder, private readonly cuentasService: CuentaService, private readonly recibosService: ReciboService, bancoService: BancoService) {
    this.externalForm = formBuilder.nonNullable.group({ depositante: ['', Validators.required], categoria: ['OTROS', Validators.required], concepto: ['', Validators.required], monto: [0, Validators.min(0.01)] });
    this.exchangeForm = formBuilder.nonNullable.group({ cuentaId: [0, Validators.min(1)], bancoId: [0, Validators.min(1)], fechaDeposito: [this.today(), Validators.required] });
    bancoService.getAll().subscribe({ next: (items) => this.bancos.set(items.filter((x) => x.activo)), error: (e: unknown) => this.showError(e) });
    this.loadAccounts();
  }
  loadAccounts(): void { this.cuentasService.getAll().subscribe({ next: (items) => this.cuentas.set(items.filter((x) => x.estado === 'PENDIENTE' && x.socio !== null)), error: (e: unknown) => this.showError(e) }); }
  createExternal(): void { if (this.externalForm.invalid) return; this.clear(); const v = this.externalForm.getRawValue(); this.recibosService.createExternal({ ...v, monto: Number(v.monto) }).subscribe({ next: (r) => { this.receipt.set(r); this.message.set(`Ingreso externo registrado con ${r.correlativo}.`); this.externalForm.reset({ depositante: '', categoria: 'OTROS', concepto: '', monto: 0 }); }, error: (e: unknown) => this.showError(e) }); }
  exchange(): void { if (this.exchangeForm.invalid) return; this.clear(); const v = this.exchangeForm.getRawValue(); this.cuentasService.exchange({ cuentaId: Number(v.cuentaId), bancoId: Number(v.bancoId), fechaDeposito: v.fechaDeposito }).subscribe({ next: (r) => { this.receipt.set(r); this.message.set(`Canje registrado con ${r.correlativo}.`); this.loadAccounts(); }, error: (e: unknown) => this.showError(e) }); }
  accountLabel(c: CuentaCobrar): string { return `${c.socio?.codigo} · ${c.socio?.nombres} ${c.socio?.apellidos} · ${c.servicio.nombre} · ${c.monto}`; }
  private today(): string { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`; }
  private clear(): void { this.error.set(''); this.message.set(''); this.receipt.set(null); }
  private showError(e: unknown): void { this.error.set(apiErrorMessage(e)); }
}
