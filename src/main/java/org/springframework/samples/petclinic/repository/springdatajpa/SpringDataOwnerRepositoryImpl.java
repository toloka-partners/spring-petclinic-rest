package org.springframework.samples.petclinic.repository.springdatajpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
@Profile("spring-data-jpa")
public class SpringDataOwnerRepositoryImpl implements CustomOwnerRepository {

    @Autowired
    private EntityManager entityManager;

    @Override
    public Collection<Owner> findAllWithEntityGraph() {
        TypedQuery<Owner> query = entityManager.createQuery("SELECT DISTINCT o FROM Owner o", Owner.class);
        query.setHint("jakarta.persistence.loadgraph", entityManager.getEntityGraph("Owner.pets"));
        return query.getResultList();
    }

    @Override
    public Owner findByIdWithEntityGraph(int id) {
        TypedQuery<Owner> query = entityManager.createQuery("SELECT o FROM Owner o WHERE o.id = :id", Owner.class);
        query.setParameter("id", id);
        query.setHint("jakarta.persistence.loadgraph", entityManager.getEntityGraph("Owner.pets"));
        return query.getSingleResult();
    }
}