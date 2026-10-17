package com.spaceora.space.repository;

import com.spaceora.space.entity.Space;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface SpaceRepository extends JpaRepository<Space, Long>, JpaSpecificationExecutor<Space> {

    List<Space> findByOwnerId(Long ownerId);

    Page<Space> findByIsActiveTrue(Pageable pageable);
}
