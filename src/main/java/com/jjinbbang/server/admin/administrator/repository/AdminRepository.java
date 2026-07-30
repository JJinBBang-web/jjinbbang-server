package com.jjinbbang.server.admin.administrator.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jjinbbang.server.admin.administrator.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {

	Optional<Admin> findByOidcIssuerAndOidcSubject(String oidcIssuer, String oidcSubject);
}
