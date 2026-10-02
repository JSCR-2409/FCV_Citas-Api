-- Cuarto estado de reprogramacion: una solicitud PENDING queda CANCELLED si el paciente cancela
-- la cita antes de que el ADMIN decida. Existe en el modelo de referencia pero V4 y V8 no lo
-- sembraron, asi que el entorno de pruebas se quedaba sin el.
-- Idempotente: en la BD inicializada desde db.sql la fila ya existe y este INSERT no hace nada.

INSERT INTO reschedule_request_statuses (code,name,is_terminal)
  SELECT 'CANCELLED','Cancelada',TRUE
  WHERE NOT EXISTS (SELECT 1 FROM reschedule_request_statuses WHERE code='CANCELLED');
