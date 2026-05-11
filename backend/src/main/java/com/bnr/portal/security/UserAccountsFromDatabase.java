package com.bnr.portal.security;

import com.bnr.portal.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountsFromDatabase implements UserDetailsService {

    private final UserRepository users;

    public UserAccountsFromDatabase(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var row = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return new SignedInUser(row);
    }
}
