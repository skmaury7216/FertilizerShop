package com.fertilizershop.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService users(@Value("${shop.admin.username}") String username,@Value("${shop.admin.password}") String password, PasswordEncoder encoder){
   if(username.isBlank()||password.isBlank()||password.length()<12) throw new IllegalStateException("Set SHOP_ADMIN_USER and SHOP_ADMIN_PASSWORD (minimum 12 characters) before starting");
   return new InMemoryUserDetailsManager(User.withUsername(username).password(encoder.encode(password)).roles("ADMIN").build());
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
   return http.authorizeHttpRequests(a->a.requestMatchers("/css/**","/js/**","/favicon.ico").permitAll().anyRequest().authenticated())
    .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/dashboard",true).permitAll())
    .logout(l->l.logoutSuccessUrl("/login?logout").permitAll())
    .headers(h->h.frameOptions(frame->frame.deny())).build();
 }
}
