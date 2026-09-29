CREATE TABLE IF NOT EXISTS roles (
  id SMALLINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(30) NOT NULL,
  name VARCHAR(100) NOT NULL,
  CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL,
  role_id SMALLINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

INSERT INTO roles (code, name)
SELECT 'USER', 'Usuario' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'USER');
INSERT INTO roles (code, name)
SELECT 'PROFESSIONAL', 'Profesional' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'PROFESSIONAL');
INSERT INTO roles (code, name)
SELECT 'ADMIN', 'Administrador' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'USER'
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
