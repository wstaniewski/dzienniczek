package com.mobilernd.dzienniczek.repository;

import com.mobilernd.dzienniczek.model.ImportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportStatusRepository extends JpaRepository<ImportStatus, String> {
}
