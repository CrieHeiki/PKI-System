export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message: string;
}
