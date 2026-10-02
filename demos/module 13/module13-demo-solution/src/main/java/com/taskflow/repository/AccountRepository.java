package com.taskflow.repository;

import com.taskflow.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     *  :name parameter is bound separately from the query
     * text — the database driver sends them independently, so nothing
     * a user types can change the shape of this query.
     */
    @Query("SELECT a FROM Account a WHERE a.name = :name")
    List<Account> findByNameSafe(@Param("name") String name);
}
