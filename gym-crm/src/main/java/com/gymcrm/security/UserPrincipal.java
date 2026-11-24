package com.gymcrm.security;

import com.gymcrm.model.Trainer;
import com.gymcrm.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

//adapts domain user to spring security UserDetails
//isEnabled() wired to the existing active/deactivated flag so deactivated user cannot log in or use a token.
//accountNonLocked is decided by the caller at load time
//it's not reaching LoginAttemptService
//plain dependency-free data holder
public class UserPrincipal implements UserDetails {

    private final User user;
    private final boolean accountNonLocked;

    public UserPrincipal(User user, boolean accountNonLocked) {
        this.user = user;
        this.accountNonLocked = accountNonLocked;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user instanceof Trainer ? "ROLE_TRAINER" : "ROLE_TRAINEE";
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
