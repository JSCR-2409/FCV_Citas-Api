package co.fcv.citas.adapters.in.security;
import co.fcv.citas.auth.JwtService;
import io.jsonwebtoken.Claims; import jakarta.servlet.FilterChain; import jakarta.servlet.ServletException; import jakarta.servlet.http.*; import java.io.IOException; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.core.authority.SimpleGrantedAuthority; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
@Component public class JwtAuthFilter extends OncePerRequestFilter { private final JwtService jwt; public JwtAuthFilter(JwtService j){jwt=j;} @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {String h=req.getHeader("Authorization"); if(h!=null&&h.startsWith("Bearer ")){try{Claims c=jwt.parseAccess(h.substring(7)); if("access".equals(c.get("type"))){var a=new UsernamePasswordAuthenticationToken(c.getSubject(),null,authorities(c));SecurityContextHolder.getContext().setAuthentication(a);}}catch(Exception ignored){}} chain.doFilter(req,res);}

 /**
  * Concede una autoridad por cada rol del token. Se lee "roles" cuando esta presente y se cae
  * a "role" para los tokens emitidos antes de que el claim existiera, que siguen siendo validos
  * hasta que expiran.
  */
 private java.util.List<SimpleGrantedAuthority> authorities(Claims c){
   var codes=new java.util.LinkedHashSet<String>();
   if(c.get("roles") instanceof java.util.Collection<?> list) for(Object code:list) if(code!=null) codes.add(code.toString());
   if(codes.isEmpty()&&c.get("role")!=null) codes.add(c.get("role").toString());
   return codes.stream().map(code->new SimpleGrantedAuthority("ROLE_"+code)).toList();
 }
}
