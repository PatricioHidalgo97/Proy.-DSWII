import { Component, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CuentaCobrar, CuentaPayload, Puesto, Recibo, Servicio, Socio } from '../../models/models';
import { CuentaService } from '../../services/cuenta.service';
import { PuestoService } from '../../services/puesto.service';
import { ServicioService } from '../../services/servicio.service';
import { SocioService } from '../../services/socio.service';
import { apiErrorMessage } from '../../shared/api-error';

@Component({ selector: 'app-cuentas', imports: [ReactiveFormsModule], templateUrl: './cuentas.html' })
export class Cuentas {
  readonly cuentas = signal<CuentaCobrar[]>([]);
  readonly puestos = signal<Puesto[]>([]);
  readonly socios = signal<Socio[]>([]);
  readonly servicios = signal<Servicio[]>([]);
  readonly selected = signal<Set<number>>(new Set());
  readonly targetSelection = signal<Set<number>>(new Set());
  readonly lastReceipt = signal<Recibo | null>(null);
  readonly error = signal('');
  readonly message = signal('');
  readonly form;
  readonly filterForm;
  readonly selectedTotal = computed(() => this.cuentas()
    .filter((item) => this.selected().has(item.id)).reduce((sum, item) => sum + Number(item.monto), 0));

  constructor(
    formBuilder: FormBuilder,
    private readonly cuentaService: CuentaService,
    puestoService: PuestoService,
    socioService: SocioService,
    servicioService: ServicioService,
  ) {
    this.form = formBuilder.nonNullable.group({
      modo: ['INDIVIDUAL'], destino: ['PUESTO'],
      periodo: [this.currentPeriod(), [Validators.required, Validators.pattern(/^[0-9]{4}-(0[1-9]|1[0-2])$/)]],
      targetId: [0], servicioId: [0, Validators.min(1)], monto: [0],
      etapa: [0], sociosUnicos: [false], lecturaInicial: [0], lecturaFinal: [0], costoUnitario: [0],
    });
    this.filterForm = formBuilder.nonNullable.group({ tipo: ['TODOS'], targetId: [0], estado: ['TODOS'] });
    puestoService.getAll().subscribe({ next: (items) => this.puestos.set(items), error: (e: unknown) => this.showError(e) });
    socioService.getAll().subscribe({ next: (items) => this.socios.set(items), error: (e: unknown) => this.showError(e) });
    servicioService.getAll().subscribe({ next: (items) => this.servicios.set(items), error: (e: unknown) => this.showError(e) });
    this.load();
  }

  servicesForTarget(): Servicio[] { return this.servicios().filter((item) => item.cargoA === this.form.controls.destino.value); }
  selectedService(): Servicio | undefined { return this.servicios().find((item) => item.id === Number(this.form.controls.servicioId.value)); }
  targets(): Array<Puesto | Socio> { return this.form.controls.destino.value === 'PUESTO' ? this.puestos() : this.socios(); }
  targetLabel(item: Puesto | Socio): string {
    return 'numero' in item ? `Puesto ${item.numero}${item.socio ? ` · ${item.socio.nombres} ${item.socio.apellidos}` : ''}`
      : `${item.codigo} · ${item.nombres} ${item.apellidos}`;
  }

  destinationChanged(): void {
    this.form.patchValue({ targetId: 0, servicioId: 0 });
    this.targetSelection.set(new Set());
  }

  toggleTarget(id: number, checked: boolean): void {
    const next = new Set(this.targetSelection()); checked ? next.add(id) : next.delete(id); this.targetSelection.set(next);
  }

  toggleAccount(cuenta: CuentaCobrar, checked: boolean): void {
    if (cuenta.estado !== 'PENDIENTE') return;
    const next = new Set(this.selected()); checked ? next.add(cuenta.id) : next.delete(cuenta.id); this.selected.set(next);
  }

  toggleAll(checked: boolean): void {
    this.selected.set(checked ? new Set(this.visible().filter((item) => item.estado === 'PENDIENTE').map((item) => item.id)) : new Set());
  }

  load(): void {
    const filter = this.filterForm.getRawValue();
    const id = Number(filter.targetId);
    const request = filter.tipo === 'PUESTO' && id > 0 ? this.cuentaService.getByPuesto(id)
      : filter.tipo === 'SOCIO' && id > 0 ? this.cuentaService.getBySocio(id) : this.cuentaService.getAll();
    request.subscribe({
      next: (items) => { this.cuentas.set(items); this.selected.set(new Set()); },
      error: (e: unknown) => this.showError(e),
    });
  }

  visible(): CuentaCobrar[] {
    const status = this.filterForm.controls.estado.value;
    return status === 'TODOS' ? this.cuentas() : this.cuentas().filter((item) => item.estado === status);
  }

  create(): void {
    if (this.form.invalid) return;
    this.clearAlerts();
    const value = this.form.getRawValue();
    const service = this.selectedService();
    if (!service) { this.error.set('Seleccione un servicio.'); return; }
    if (value.modo === 'MASIVO') {
      const ids = [...this.targetSelection()];
      const payload = { periodo: value.periodo, servicioId: Number(value.servicioId),
        monto: Number(value.monto) > 0 ? Number(value.monto) : null,
        ...(value.destino === 'PUESTO' ? { puestoIds: ids } : { socioIds: ids, etapa: Number(value.etapa) || null, sociosUnicos: value.sociosUnicos }) };
      const request = value.destino === 'PUESTO'
        ? this.cuentaService.generateForPuestos(payload) : this.cuentaService.generateForSocios(payload);
      request.subscribe({ next: (items) => { this.message.set(`${items.length} cuentas generadas.`); this.targetSelection.set(new Set()); this.load(); }, error: (e: unknown) => this.showError(e) });
      return;
    }
    if (Number(value.targetId) <= 0) { this.error.set('Seleccione el destinatario.'); return; }
    const payload: CuentaPayload = {
      periodo: value.periodo, servicioId: Number(value.servicioId),
      monto: Number(value.monto) > 0 ? Number(value.monto) : null,
      ...(value.destino === 'PUESTO' ? { puestoId: Number(value.targetId) } : { socioId: Number(value.targetId) }),
      ...(service.dependeConsumo ? { lecturaInicial: Number(value.lecturaInicial), lecturaFinal: Number(value.lecturaFinal), costoUnitario: Number(value.costoUnitario) } : {}),
    };
    this.cuentaService.create(payload).subscribe({
      next: () => { this.message.set('Cuenta por cobrar generada.'); this.load(); },
      error: (e: unknown) => this.showError(e),
    });
  }

  pay(cuenta: CuentaCobrar): void {
    if (!confirm(`¿Registrar el pago de ${cuenta.servicio.moneda} ${cuenta.monto}?`)) return;
    this.clearAlerts();
    this.cuentaService.pay(cuenta.id).subscribe({
      next: (receipt) => { this.lastReceipt.set(receipt); this.message.set(`Pago registrado. Recibo ${receipt.correlativo}.`); this.load(); },
      error: (e: unknown) => this.showError(e),
    });
  }

  paySelected(): void {
    const ids = [...this.selected()];
    if (!ids.length || !confirm(`¿Pagar ${ids.length} cuentas por un total de ${this.selectedTotal().toFixed(2)}?`)) return;
    this.clearAlerts();
    this.cuentaService.payBatch(ids).subscribe({ next: (items) => { this.lastReceipt.set(items.at(-1) ?? null); this.message.set(`${items.length} pagos y recibos registrados.`); this.load(); }, error: (e: unknown) => this.showError(e) });
  }

  exonerate(cuenta: CuentaCobrar): void {
    if (!confirm('¿Exonerar esta cuenta?')) return;
    this.clearAlerts();
    this.cuentaService.exonerate(cuenta.id).subscribe({ next: () => { this.message.set('Cuenta exonerada.'); this.load(); }, error: (e: unknown) => this.showError(e) });
  }

  exonerateSelected(): void {
    const ids = [...this.selected()];
    if (!ids.length || !confirm(`¿Exonerar ${ids.length} cuentas seleccionadas?`)) return;
    this.clearAlerts();
    this.cuentaService.exonerateBatch(ids).subscribe({ next: (items) => { this.message.set(`${items.length} cuentas exoneradas.`); this.load(); }, error: (e: unknown) => this.showError(e) });
  }

  totalPending(): number { return this.visible().filter((item) => item.estado === 'PENDIENTE').reduce((sum, item) => sum + Number(item.monto), 0); }
  accountTarget(cuenta: CuentaCobrar): string {
    return cuenta.socio ? `${cuenta.socio.codigo} · ${cuenta.socio.nombres} ${cuenta.socio.apellidos}`
      : cuenta.puesto ? `Puesto ${cuenta.puesto.numero}${cuenta.puesto.socio ? ` · ${cuenta.puesto.socio.nombres} ${cuenta.puesto.socio.apellidos}` : ''}` : '—';
  }

  private currentPeriod(): string {
    const today = new Date(); return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
  }
  private clearAlerts(): void { this.error.set(''); this.message.set(''); this.lastReceipt.set(null); }
  private showError(error: unknown): void { this.error.set(apiErrorMessage(error)); }
}
