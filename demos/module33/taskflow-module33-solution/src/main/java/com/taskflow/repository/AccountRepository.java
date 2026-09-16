package com.taskflow.repository;

import com.taskflow.model.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Module 13, Injection mitigation: exactly the pattern Module 12
     * taught. The :name parameter is bound separately from the query
     * text — the database driver sends them independently, so nothing
     * a user types can change the shape of this query.
     */
    @Query("SELECT a FROM Account a WHERE a.name = :name")
    List<Account> findByNameSafe(@Param("name") String name);

    /**
     * Module 33, the N+1 fix: @EntityGraph tells Hibernate to fetch
     * transactions in the SAME query as the accounts themselves, via a
     * JOIN, instead of one separate query per account when
     * .getTransactions() is later accessed. One query in, one query
     * out, regardless of how many accounts exist.
     */
    @EntityGraph(attributePaths = "transactions")
    @Query("SELECT a FROM Account a")
    List<Account> findAllWithTransactions();
}

