export interface GeneracionDto {
  city: string;
  responseType: string | null;
  response: string | null;
  elements: GeneracionElementDto[];
}

export interface GeneracionElementDto {
  matricula: number;
  arrancado: boolean;
  error: string | null;
}