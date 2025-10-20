package com.wego.carpark.mapper;

import com.wego.carpark.dto.CarParkResponseDto;
import com.wego.carpark.model.CarPark;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for CarPark entity to DTO conversion.
 */
@Mapper(componentModel = "spring")
public interface CarParkMapper {

    /**
     * Convert CarPark entity to CarParkResponseDto.
     * Maps totalAvailableLots from entity to availableLots in DTO.
     *
     * @param carPark The CarPark entity
     * @return CarParkResponseDto
     */
    @Mapping(source = "totalAvailableLots", target = "availableLots")
    CarParkResponseDto toDto(CarPark carPark);
}