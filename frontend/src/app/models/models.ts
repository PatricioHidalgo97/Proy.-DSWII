export interface SessionUser { id: number; usuario: string; nombres: string; apellidos: string; rol: 'ADMIN' | 'OPERADOR'; }
export interface LoginResponse extends SessionUser { token: string; }

export interface Socio {
  id: number; codigo: string; nombres: string; apellidos: string; accion: string | null;
  etapa: number; fechaNacimiento: string | null; activo: boolean;
}
export type SocioPayload = Omit<Socio, 'id'>;

export interface Giro { id: number; nombre: string; descripcion: string | null; activo: boolean; }
export type GiroPayload = Omit<Giro, 'id'>;

export interface Puesto {
  id: number; numero: string; nombreInquilino: string | null; fechaInicio: string | null;
  fechaFin: string | null; socio: Socio | null; giro: Giro; activo: boolean;
}
export interface PuestoPayload {
  numero: string; nombreInquilino: string | null; fechaInicio: string | null; fechaFin: string | null;
  socioId: number | null; giroId: number; activo: boolean;
}

export type Recurrencia = 'UNICA' | 'MENSUAL' | 'TRIMESTRAL' | 'ANUAL';
export type Moneda = 'PEN' | 'USD';
export type CargoA = 'PUESTO' | 'SOCIO';
export interface Servicio {
  id: number; nombre: string; recurrencia: Recurrencia; costo: number; moneda: Moneda;
  cargoA: CargoA; dependeConsumo: boolean; activo: boolean;
}
export type ServicioPayload = Omit<Servicio, 'id'>;

export type EstadoCuenta = 'PENDIENTE' | 'PAGADA' | 'EXONERADA';
export interface CuentaCobrar {
  id: number; periodo: string; monto: number; estado: EstadoCuenta; fechaCreacion: string;
  puesto: Puesto | null; socio: Socio | null; servicio: Servicio;
  lecturaInicial: number | null; lecturaFinal: number | null; costoUnitario: number | null;
}
export interface CuentaPayload {
  periodo: string; monto?: number | null; puestoId?: number | null; socioId?: number | null; servicioId: number;
  lecturaInicial?: number | null; lecturaFinal?: number | null; costoUnitario?: number | null;
}
export interface GenerarCuentasPayload {
  periodo: string; servicioId: number; monto?: number | null; puestoIds?: number[]; socioIds?: number[];
  etapa?: number | null; sociosUnicos?: boolean;
}

export interface Banco { id: number; nombre: string; numeroCuenta: string; cci: string | null; moneda: Moneda; activo: boolean; }
export type BancoPayload = Omit<Banco, 'id'>;

export type TipoRecibo = 'INGRESO' | 'INGRESO_EXTERNO' | 'BANCARIO';
export interface Recibo {
  id: number; correlativo: string; fecha: string; monto: number; tipo: TipoRecibo;
  cuentaCobrar: CuentaCobrar | null; banco: Banco | null; depositante: string | null; categoria: string | null;
  concepto: string | null; fechaDeposito: string | null; usuario: SessionUser;
}
export interface IngresoExternoPayload { depositante: string; categoria: string; concepto: string; monto: number; }
export interface CanjePayload { cuentaId: number; bancoId: number; fechaDeposito: string; }

export type EstadoEgreso = 'REGISTRADO' | 'PROCESADO' | 'ANULADO';
export interface Egreso {
  id: number; comprobante: string; tipoDocumento: string; numeroDocumento: string; proveedor: string;
  fecha: string; subtotal: number; impuesto: number; total: number; motivo: string;
  documentoAsociado: string | null; estado: EstadoEgreso; usuario: SessionUser;
  fechaRegistro: string; fechaActualizacion: string | null;
}
export interface EgresoPayload {
  tipoDocumento: string; numeroDocumento: string; proveedor: string; fecha: string;
  subtotal: number; impuesto: number; total: number; motivo: string; documentoAsociado: string | null;
}
