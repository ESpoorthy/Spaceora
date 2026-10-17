package com.spaceora.space.dto;

import com.spaceora.space.entity.SpaceImage;

public record SpaceImageDto(Long id, String imageUrl, int displayOrder) {

    public static SpaceImageDto from(SpaceImage img) {
        return new SpaceImageDto(img.getId(), img.getImageUrl(), img.getDisplayOrder());
    }
}
