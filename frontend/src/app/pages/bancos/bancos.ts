import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Banco, BancoPayload } from '../../models/models';
import { AuthService } from '../../services/auth.service';
import { BancoService } from '../../services/banco.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({ selector: 'app-bancos', imports: [ReactiveFormsModule], templateUrl: './bancos.html' })
export class Bancos {
  readonly items = signal<Banco[]>([]); readonly editingId = signal<number | null>(null);
  readonly error = signal(''); readonly message = signal(''); readonly form;
  constructor(formBuilder: FormBuilder, private readonly service: BancoService, public readonly auth: AuthService) {
    this.form = formBuilder.nonNullable.group({ nombre: ['', Validators.required], numeroCuenta: ['', Validators.required], cci: [''], moneda: ['PEN'], activo: [true] });
    this.load();
  }
  load(): void { this.service.getAll().subscribe({ next: (items) => this.items.set(items), error: (e: unknown) => this.error.set(apiErrorMessage(e)) }); }
  edit(item: Banco): void { this.editingId.set(item.id); this.form.setValue({ nombre: item.nombre, numeroCuenta: item.numeroCuenta, cci: item.cci ?? '', moneda: item.moneda, activo: item.activo }); }
  save(): void {
    if (this.form.invalid) return; this.error.set('');
    const value = this.form.getRawValue();
    const payload: BancoPayload = { nombre: value.nombre, numeroCuenta: value.numeroCuenta, cci: value.cci || null, moneda: value.moneda as 'PEN' | 'USD', activo: value.activo };
    const id = this.editingId(); const request = id === null ? this.service.create(payload) : this.service.update(id, payload);
    request.subscribe({ next: () => { this.message.set(id === null ? 'Banco registrado.' : 'Banco actualizado.'); this.cancel(); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) });
  }
  remove(item: Banco): void { if (!confirm(`¿Eliminar ${item.nombre}?`)) return; this.service.delete(item.id).subscribe({ next: () => { this.message.set('Banco eliminado.'); this.load(); }, error: (e: unknown) => this.error.set(apiErrorMessage(e)) }); }
  cancel(): void { this.editingId.set(null); this.form.reset({ nombre: '', numeroCuenta: '', cci: '', moneda: 'PEN', activo: true }); }
}
