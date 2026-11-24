package com.gymcrm.security;

import com.gymcrm.dao.UserDao;
import com.gymcrm.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//security task 3/5
//for loading  user login attempt or incoming JWT
//used by AuthentificationManager during POST /login and by JwtAuthenticationFilter(realoaded on every request for immediate deactivation)
//since it called from servlet filter and from auth manager .authenticate(), both do not open hibernate session so @Transactional is used
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDao userDao;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        boolean accountNonLocked = !loginAttemptService.isBlocked(username);
        if (!accountNonLocked) {
            log.debug("username={} is currently brute-force locked", username);
        }
        return new UserPrincipal(user, accountNonLocked);
    }
}
