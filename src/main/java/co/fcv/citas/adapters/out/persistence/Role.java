package co.fcv.citas.adapters.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Short id;
  @Column(nullable = false, unique = true) private String code;
  @Column(nullable = false) private String name;

  protected Role() {}
  public Short getId() { return id; }
  public String getCode() { return code; }
  public String getName() { return name; }
}
