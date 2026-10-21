package com.hospital.bedmgmt.repo;

import com.hospital.bedmgmt.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    List<UserAccount> findAllByOrderByUsernameAsc();
}
