package co.fcv.citas.session; import java.util.List; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository; public interface RefreshSessionRepository extends JpaRepository<RefreshSession,Long>{ Optional<RefreshSession> findByTokenHash(String hash);
 /** HU-006: sesiones vivas de una cuenta, para revocarlas al cambiar la contrasena. */
 List<RefreshSession> findByUserIdAndRevokedFalse(Long userId); }
