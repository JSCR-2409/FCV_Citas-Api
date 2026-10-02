package co.fcv.citas.user;

import co.fcv.citas.catalog.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "users", uniqueConstraints = {
  @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
  @UniqueConstraint(name = "uk_users_document", columnNames = {"document_type", "document_number"})
})
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "first_name", nullable = false) private String names;
  @Column(name = "last_name", nullable = false) private String surnames;
  @Column(name = "document_type", nullable = false) private String documentType;
  @Column(name = "document_number", nullable = false) private String documentNumber;
  @Column(nullable = false) private String email;
  @Column(nullable = false) private String phone;
  @Column(name = "password_hash", nullable = false) private String passwordHash;
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();
  @Column(nullable = false) private boolean active = true;
  @Column(nullable = false) private Instant createdAt = Instant.now();

  protected User() {}
  public User(String n, String s, String dt, String dn, String e, String p, String h) { names = n; surnames = s; documentType = dt; documentNumber = dn; email = e; phone = p; passwordHash = h; }
  public Long getId() { return id; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public void changePassword(String encodedPassword) { this.passwordHash = encodedPassword; }
  public boolean isActive() { return active; }
  public void addRole(Role role) { roles.add(role); }
  /** Rol de presentacion por defecto. El orden alfabetico deja ADMIN < PROFESSIONAL < USER. */
  public String getPrimaryRole() { return roles.stream().map(Role::getCode).sorted().findFirst().orElse("USER"); }
  /** Todos los roles del usuario: user_roles es N:M y el PRD admite usuarios con varios roles. */
  public java.util.List<String> getRoleCodes() { var codes = roles.stream().map(Role::getCode).sorted().toList(); return codes.isEmpty() ? java.util.List.of("USER") : codes; }
}
