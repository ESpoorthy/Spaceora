package com.spaceora.space.repository;

import com.spaceora.space.entity.SpaceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceImageRepository extends JpaRepository<SpaceImage, Long> {

    List<SpaceImage> findBySpaceIdOrderByDisplayOrderAsc(Long spaceId);

    void deleteBySpaceId(Long spaceId);
}
