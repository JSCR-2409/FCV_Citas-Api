-- V4 solo sembro REQUESTED y APPROVED, pero el codigo resuelve CANCELLED y REJECTED
-- por codigo (SELECT id FROM appointment_statuses WHERE code=...). Sin estas filas esas
-- subconsultas devuelven NULL y cancelar o rechazar falla en silencio.
-- Se insertan en el mismo orden del modelo de referencia para que los ids coincidan
-- (1 REQUESTED, 2 APPROVED, 3 REJECTED, 4 CANCELLED, 5 COMPLETED, 6 NO_SHOW).
-- Idempotente: en la BD de desarrollo las 6 filas ya existen y estos INSERT no hacen nada.

INSERT INTO appointment_statuses (code,name,is_terminal)
  SELECT 'REJECTED','Rechazada',TRUE WHERE NOT EXISTS (SELECT 1 FROM appointment_statuses WHERE code='REJECTED');
INSERT INTO appointment_statuses (code,name,is_terminal)
  SELECT 'CANCELLED','Cancelada',TRUE WHERE NOT EXISTS (SELECT 1 FROM appointment_statuses WHERE code='CANCELLED');
INSERT INTO appointment_statuses (code,name,is_terminal)
  SELECT 'COMPLETED','Atendida',TRUE WHERE NOT EXISTS (SELECT 1 FROM appointment_statuses WHERE code='COMPLETED');
INSERT INTO appointment_statuses (code,name,is_terminal)
  SELECT 'NO_SHOW','No asistio',TRUE WHERE NOT EXISTS (SELECT 1 FROM appointment_statuses WHERE code='NO_SHOW');

INSERT INTO reschedule_request_statuses (code,name,is_terminal)
  SELECT 'APPROVED','Aprobada',TRUE WHERE NOT EXISTS (SELECT 1 FROM reschedule_request_statuses WHERE code='APPROVED');
INSERT INTO reschedule_request_statuses (code,name,is_terminal)
  SELECT 'REJECTED','Rechazada',TRUE WHERE NOT EXISTS (SELECT 1 FROM reschedule_request_statuses WHERE code='REJECTED');
