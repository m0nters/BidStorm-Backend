package com.taitrinh.online_auction.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.taitrinh.online_auction.entity.auth.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {
}
